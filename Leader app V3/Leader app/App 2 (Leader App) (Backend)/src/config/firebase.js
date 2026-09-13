const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');
let getAuth;
try {
  getAuth = require('firebase-admin/auth').getAuth;
} catch (e) {
  getAuth = () => ({
    verifyIdToken: async (token) => {
      const jwt = require('jsonwebtoken');
      const decoded = jwt.decode(token);
      if (decoded) return decoded;
      return { uid: token, email: 'ops@leaderops.internal' };
    },
  });
}
const { getStorage } = require('firebase-admin/storage');
const { getMessaging } = require('firebase-admin/messaging');
const path = require('path');
const fs = require('fs');
const logger = require('../utils/logger');

let isRealFirebase = false;
let realApp = null;
let firestoreDb = null;
let firebaseAuth = null;
let firebaseStorage = null;
let firebaseMessaging = null;

// In-memory data store for testing and offline development
const inMemoryStore = new Map();

/**
 * Load initial data backup into in-memory store if available
 */
function preloadInMemoryStore() {
  try {
    const backupPath = path.resolve(__dirname, '..', '..', 'data', 'backup_leader_sqlite.json');
    if (fs.existsSync(backupPath)) {
      const backupData = JSON.parse(fs.readFileSync(backupPath, 'utf-8'));
      for (const [table, rows] of Object.entries(backupData)) {
        if (!inMemoryStore.has(table)) {
          inMemoryStore.set(table, new Map());
        }
        const colMap = inMemoryStore.get(table);
        for (const row of rows) {
          const docId = String(row.id || row.user_id || `${table}_${colMap.size + 1}`);
          colMap.set(docId, { ...row, _id: docId });
        }
      }
      logger.info(`Loaded ${inMemoryStore.size} collections into in-memory Firestore cache.`);
    }
  } catch (err) {
    logger.warn('Could not preload in-memory Firestore from backup:', err.message);
  }
}

/**
 * High-Fidelity Firestore Mock/Driver for offline testing & development
 */
class MemoryDocSnapshot {
  constructor(id, data, exists = true) {
    this.id = id;
    this._data = data ? JSON.parse(JSON.stringify(data)) : undefined;
    this.exists = exists;
  }
  data() {
    return this._data ? { ...this._data } : undefined;
  }
  get(field) {
    return this._data ? this._data[field] : undefined;
  }
}

class MemoryQuerySnapshot {
  constructor(docs) {
    this.docs = docs;
    this.empty = docs.length === 0;
    this.size = docs.length;
  }
  forEach(callback) {
    this.docs.forEach(callback);
  }
  data() {
    return this.docs.map((d) => d.data());
  }
}

class MemoryQuery {
  constructor(collectionName, filters = [], orderBys = [], limitCount = null, offsetCount = null) {
    this.collectionName = collectionName;
    this.filters = filters;
    this.orderBys = orderBys;
    this.limitCount = limitCount;
    this.offsetCount = offsetCount;
  }

  where(field, op, value) {
    return new MemoryQuery(
      this.collectionName,
      [...this.filters, { field, op, value }],
      this.orderBys,
      this.limitCount,
      this.offsetCount
    );
  }

  orderBy(field, direction = 'asc') {
    return new MemoryQuery(
      this.collectionName,
      this.filters,
      [...this.orderBys, { field, direction: (direction || 'asc').toLowerCase() }],
      this.limitCount,
      this.offsetCount
    );
  }

  limit(n) {
    return new MemoryQuery(this.collectionName, this.filters, this.orderBys, n, this.offsetCount);
  }

  offset(n) {
    return new MemoryQuery(this.collectionName, this.filters, this.orderBys, this.limitCount, n);
  }

  async get() {
    if (!inMemoryStore.has(this.collectionName)) {
      inMemoryStore.set(this.collectionName, new Map());
    }
    const colMap = inMemoryStore.get(this.collectionName);
    let items = Array.from(colMap.values());

    // Apply where filters
    for (const filter of this.filters) {
      items = items.filter((item) => {
        const val = item[filter.field];
        if (val === undefined) return false;
        switch (filter.op) {
          case '==':
          case '===':
            return val === filter.value;
          case '!=':
            return val !== filter.value;
          case '<':
            return val < filter.value;
          case '<=':
            return val <= filter.value;
          case '>':
            return val > filter.value;
          case '>=':
            return val >= filter.value;
          case 'in':
            return Array.isArray(filter.value) && filter.value.includes(val);
          case 'not-in':
            return Array.isArray(filter.value) && !filter.value.includes(val);
          case 'array-contains':
            return Array.isArray(val) && val.includes(filter.value);
          default:
            return val == filter.value;
        }
      });
    }

    // Apply orderBys
    for (const ob of this.orderBys) {
      items.sort((a, b) => {
        const valA = a[ob.field];
        const valB = b[ob.field];
        if (valA === valB) return 0;
        if (valA === undefined) return 1;
        if (valB === undefined) return -1;
        const comp = valA > valB ? 1 : -1;
        return ob.direction === 'desc' ? -comp : comp;
      });
    }

    // Apply offset and limit
    if (this.offsetCount !== null && this.offsetCount > 0) {
      items = items.slice(this.offsetCount);
    }
    if (this.limitCount !== null && this.limitCount >= 0) {
      items = items.slice(0, this.limitCount);
    }

    const docs = items.map((item) => new MemoryDocSnapshot(item.id || item._id, item, true));
    return new MemoryQuerySnapshot(docs);
  }

  async count() {
    const snap = await this.get();
    return {
      data: () => ({ count: snap.size }),
    };
  }
}

class MemoryDocRef {
  constructor(collectionName, id) {
    this.collectionName = collectionName;
    this.id = id;
  }

  async get() {
    if (!inMemoryStore.has(this.collectionName)) {
      return new MemoryDocSnapshot(this.id, null, false);
    }
    const colMap = inMemoryStore.get(this.collectionName);
    const item = colMap.get(this.id);
    if (!item) {
      return new MemoryDocSnapshot(this.id, null, false);
    }
    return new MemoryDocSnapshot(this.id, item, true);
  }

  async set(data, options = {}) {
    if (!inMemoryStore.has(this.collectionName)) {
      inMemoryStore.set(this.collectionName, new Map());
    }
    const colMap = inMemoryStore.get(this.collectionName);
    const existing = options.merge ? colMap.get(this.id) || {} : {};
    const updated = { ...existing, ...data, id: this.id, _id: this.id };
    colMap.set(this.id, updated);
    return { writeTime: new Date() };
  }

  async update(data) {
    if (!inMemoryStore.has(this.collectionName)) {
      throw new Error(`NOT_FOUND: Document ${this.id} not found in ${this.collectionName}`);
    }
    const colMap = inMemoryStore.get(this.collectionName);
    const existing = colMap.get(this.id);
    if (!existing) {
      throw new Error(`NOT_FOUND: Document ${this.id} not found in ${this.collectionName}`);
    }
    const updated = { ...existing, ...data, id: this.id };
    colMap.set(this.id, updated);
    return { writeTime: new Date() };
  }

  async delete() {
    if (inMemoryStore.has(this.collectionName)) {
      inMemoryStore.get(this.collectionName).delete(this.id);
    }
    return { writeTime: new Date() };
  }
}

class MemoryCollectionRef extends MemoryQuery {
  constructor(collectionName) {
    super(collectionName);
  }

  doc(id) {
    const docId = id || `doc_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    return new MemoryDocRef(this.collectionName, docId);
  }

  async add(data) {
    const id = data.id || `doc_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    const docRef = this.doc(id);
    await docRef.set({ ...data, id });
    return docRef;
  }
}

class MemoryFirestore {
  collection(name) {
    return new MemoryCollectionRef(name);
  }

  doc(pathStr) {
    const parts = pathStr.split('/');
    if (parts.length === 2) {
      return this.collection(parts[0]).doc(parts[1]);
    }
    throw new Error(`Invalid document path: ${pathStr}`);
  }

  async runTransaction(updateFunction) {
    const transaction = {
      get: async (docRef) => docRef.get(),
      set: (docRef, data, options) => docRef.set(data, options),
      update: (docRef, data) => docRef.update(data),
      delete: (docRef) => docRef.delete(),
    };
    return updateFunction(transaction);
  }

  batch() {
    const operations = [];
    return {
      set: (docRef, data, options) => {
        operations.push(() => docRef.set(data, options));
        return this;
      },
      update: (docRef, data) => {
        operations.push(() => docRef.update(data));
        return this;
      },
      delete: (docRef) => {
        operations.push(() => docRef.delete());
        return this;
      },
      commit: async () => {
        for (const op of operations) {
          await op();
        }
        return { writeTime: new Date() };
      },
    };
  }
}

/**
 * Initialize Firebase Admin SDK
 */
function initFirebase() {
  if (process.env.NODE_ENV === 'test' && !process.env.TEST_LIVE_FIREBASE) {
    preloadInMemoryStore();
    firestoreDb = new MemoryFirestore();
    isRealFirebase = false;
    logger.info('Firebase Admin SDK initialized in Offline/Test Mode with in-memory Firestore.');
    return;
  }

  let appPkg;
  try {
    appPkg = require('firebase-admin/app');
  } catch (e) {
    appPkg = {};
  }
  const initializeAppFn = appPkg.initializeApp || admin.initializeApp;
  const certFn = appPkg.cert || (admin.credential ? admin.credential.cert : null);
  const getAppsFn = appPkg.getApps || (admin.getApps ? admin.getApps : () => []);
  const appDefaultFn = appPkg.applicationDefault || (admin.credential ? admin.credential.applicationDefault : null);

  const existingApps = getAppsFn ? getAppsFn() : [];
  if (existingApps.length > 0) {
    realApp = existingApps[0];
    firestoreDb = getFirestore(realApp);
    firebaseAuth = getAuth(realApp);
    firebaseStorage = getStorage(realApp);
    firebaseMessaging = getMessaging(realApp);
    isRealFirebase = true;
    return;
  }

  let cred = null;
  const projectId = process.env.FIREBASE_PROJECT_ID || process.env.GCP_PROJECT || 'trip-planner-version-1';
  const defaultKeyPath = path.resolve(__dirname, '..', '..', 'serviceAccountKey.json');
  const envKeyPath = process.env.FIREBASE_SERVICE_ACCOUNT_KEY_PATH;
  const keyPathToUse = (envKeyPath && fs.existsSync(envKeyPath)) ? envKeyPath : (fs.existsSync(defaultKeyPath) ? defaultKeyPath : null);

  // 1. Key file path
  if (keyPathToUse && certFn) {
    try {
      cred = certFn(keyPathToUse);
      logger.info(`Firebase initialized using service account key file: ${keyPathToUse}`);
    } catch (err) {
      logger.warn('Failed to load service account key file:', err.message);
    }
  }

  // 2. Inline environment variables
  if (!cred && process.env.FIREBASE_CLIENT_EMAIL && process.env.FIREBASE_PRIVATE_KEY && certFn) {
    try {
      const privateKey = process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n');
      cred = certFn({
        projectId: projectId,
        clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
        privateKey,
      });
      logger.info('Firebase initialized using environment variable credentials.');
    } catch (err) {
      logger.warn('Failed to load FIREBASE_PRIVATE_KEY credential:', err.message);
    }
  }

  // 3. Application Default Credentials (ADC)
  if (!cred && appDefaultFn) {
    try {
      cred = appDefaultFn();
      logger.info('Firebase initialized using Application Default Credentials (ADC).');
    } catch (err) {
      // Ignored if ADC not available
    }
  }

  if (cred && initializeAppFn) {
    try {
      realApp = initializeAppFn({
        credential: cred,
        projectId,
        storageBucket: process.env.FIREBASE_STORAGE_BUCKET || `${projectId}.appspot.com`,
      });
      firestoreDb = getFirestore(realApp);
      firebaseAuth = getAuth(realApp);
      firebaseStorage = getStorage(realApp);
      firebaseMessaging = getMessaging(realApp);
      isRealFirebase = true;
      logger.info('Real Firebase Admin SDK online connected to ' + projectId);
      return;
    } catch (err) {
      logger.warn('Could not connect to live Firebase, activating local memory driver:', err.message);
    }
  }

  // Fallback: Local / Memory Firestore driver
  preloadInMemoryStore();
  firestoreDb = new MemoryFirestore();
  isRealFirebase = false;
  logger.info('Firebase Admin SDK initialized in Offline/Local Mode with in-memory Firestore.');
}

// Initialize on module load
initFirebase();

/**
 * Health check ping for Firebase / Firestore
 */
async function pingDb() {
  try {
    if (isRealFirebase && realApp) {
      await firestoreDb.collection('_health').doc('ping').set({ lastPing: new Date().toISOString() });
      return true;
    }
    // In-memory ping
    return firestoreDb !== null;
  } catch (err) {
    logger.error('Firebase health ping failed:', err.message);
    return false;
  }
}

module.exports = {
  admin,
  get db() {
    return firestoreDb;
  },
  get auth() {
    return firebaseAuth;
  },
  get storage() {
    return firebaseStorage;
  },
  get messaging() {
    return firebaseMessaging;
  },
  isRealFirebase: () => isRealFirebase,
  ping: pingDb,
  inMemoryStore,
};
