const fs = require('fs');
const path = require('path');
const config = require('./env');
const logger = require('../utils/logger');

let adminApp = null;
let firestoreDb = null;
let authService = null;
let storageService = null;
let messagingService = null;
let isRealFirebase = false;

try {
  const { initializeApp, cert, applicationDefault, getApps } = require('firebase-admin/app');
  const { getFirestore, FieldValue, Timestamp } = require('firebase-admin/firestore');
  const { getAuth } = require('firebase-admin/auth');
  const { getStorage } = require('firebase-admin/storage');
  const { getMessaging } = require('firebase-admin/messaging');

  let credential = null;

  if (config.FIREBASE_SERVICE_ACCOUNT_PATH && fs.existsSync(config.FIREBASE_SERVICE_ACCOUNT_PATH)) {
    try {
      const serviceAccount = JSON.parse(fs.readFileSync(config.FIREBASE_SERVICE_ACCOUNT_PATH, 'utf8'));
      credential = cert(serviceAccount);
      logger.info('Firebase Admin: Loaded certificate from file path: ' + config.FIREBASE_SERVICE_ACCOUNT_PATH);
    } catch (e) {
      logger.warn('Failed to parse service account JSON file: ' + e.message);
    }
  } else if (process.env.FIREBASE_SERVICE_ACCOUNT_KEY) {
    try {
      const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_KEY);
      credential = cert(serviceAccount);
      logger.info('Firebase Admin: Loaded certificate from FIREBASE_SERVICE_ACCOUNT_KEY env');
    } catch (e) {
      logger.warn('Failed to parse FIREBASE_SERVICE_ACCOUNT_KEY: ' + e.message);
    }
  } else if (config.FIREBASE_PROJECT_ID && config.FIREBASE_CLIENT_EMAIL && config.FIREBASE_PRIVATE_KEY) {
    credential = cert({
      projectId: config.FIREBASE_PROJECT_ID,
      clientEmail: config.FIREBASE_CLIENT_EMAIL,
      privateKey: config.FIREBASE_PRIVATE_KEY,
    });
    logger.info('Firebase Admin: Loaded certificate from individual environment variables');
  } else if (process.env.GOOGLE_APPLICATION_CREDENTIALS && fs.existsSync(process.env.GOOGLE_APPLICATION_CREDENTIALS)) {
    credential = applicationDefault();
    logger.info('Firebase Admin: Loaded Application Default Credentials (ADC)');
  }

  const appOptions = {
    projectId: config.FIREBASE_PROJECT_ID || 'travel-vendor-app-2026',
  };

  if (credential) {
    appOptions.credential = credential;
    isRealFirebase = true;
  } else if (process.env.FIRESTORE_EMULATOR_HOST) {
    logger.info('Firebase Admin: Using FIRESTORE_EMULATOR_HOST: ' + process.env.FIRESTORE_EMULATOR_HOST);
    isRealFirebase = true;
  }

  if (config.FIREBASE_STORAGE_BUCKET) {
    appOptions.storageBucket = config.FIREBASE_STORAGE_BUCKET;
  }

  if (!getApps().length) {
    adminApp = initializeApp(appOptions);
  } else {
    adminApp = getApps()[0];
  }

  firestoreDb = getFirestore(adminApp);
  authService = getAuth(adminApp);
  storageService = getStorage(adminApp);
  messagingService = getMessaging(adminApp);

  // Set Firestore settings if needed
  try {
    firestoreDb.settings({ ignoreUndefinedProperties: true });
  } catch (_) {}

  logger.info(`Firebase Admin SDK initialized successfully (Project: ${appOptions.projectId}, Real Firebase/Emulator: ${isRealFirebase})`);
} catch (err) {
  logger.error('Firebase Admin initialization error: ' + err.message);
}

// In-memory Firestore & Auth fallback adapter for seamless local development and offline test runs
class MockDocumentReference {
  constructor(collectionRef, docId) {
    this.collectionRef = collectionRef;
    this.id = docId;
    this.path = `${collectionRef.id}/${docId}`;
  }

  async get() {
    const data = this.collectionRef.dataStore.get(this.id);
    return {
      id: this.id,
      exists: data !== undefined,
      data: () => (data ? JSON.parse(JSON.stringify(data)) : undefined),
    };
  }

  async set(data, options = {}) {
    let existing = this.collectionRef.dataStore.get(this.id) || {};
    let finalData;
    if (options.merge) {
      finalData = { ...existing, ...data, id: this.id };
    } else {
      finalData = { ...data, id: this.id };
    }
    this.collectionRef.dataStore.set(this.id, finalData);
    return { writeTime: new Date() };
  }

  async update(data) {
    let existing = this.collectionRef.dataStore.get(this.id);
    if (!existing) {
      throw new Error(`No document to update: ${this.path}`);
    }
    const finalData = { ...existing, ...data, id: this.id };
    this.collectionRef.dataStore.set(this.id, finalData);
    return { writeTime: new Date() };
  }

  async delete() {
    this.collectionRef.dataStore.delete(this.id);
    return { writeTime: new Date() };
  }
}

class MockQuery {
  constructor(collectionRef, filters = [], sorts = [], limitCount = null, offsetCount = null) {
    this.collectionRef = collectionRef;
    this.filters = filters;
    this.sorts = sorts;
    this.limitCount = limitCount;
    this.offsetCount = offsetCount;
  }

  where(field, op, value) {
    return new MockQuery(
      this.collectionRef,
      [...this.filters, { field, op, value }],
      this.sorts,
      this.limitCount,
      this.offsetCount
    );
  }

  orderBy(field, direction = 'asc') {
    return new MockQuery(
      this.collectionRef,
      this.filters,
      [...this.sorts, { field, direction }],
      this.limitCount,
      this.offsetCount
    );
  }

  limit(count) {
    return new MockQuery(
      this.collectionRef,
      this.filters,
      this.sorts,
      count,
      this.offsetCount
    );
  }

  offset(count) {
    return new MockQuery(
      this.collectionRef,
      this.filters,
      this.sorts,
      this.limitCount,
      count
    );
  }

  count() {
    return {
      get: async () => {
        const snap = await this.get();
        return {
          data: () => ({ count: snap.size }),
        };
      },
    };
  }

  async get() {
    let items = Array.from(this.collectionRef.dataStore.values()).map((doc) =>
      JSON.parse(JSON.stringify(doc))
    );

    // Apply filters
    for (const f of this.filters) {
      items = items.filter((item) => {
        const val = item[f.field];
        if (f.op === '==') return val === f.value || String(val) === String(f.value);
        if (f.op === '!=') return val !== f.value;
        if (f.op === '>') return val > f.value;
        if (f.op === '>=') return val >= f.value;
        if (f.op === '<') return val < f.value;
        if (f.op === '<=') return val <= f.value;
        if (f.op === 'in') return Array.isArray(f.value) && f.value.includes(val);
        if (f.op === 'array-contains') return Array.isArray(val) && val.includes(f.value);
        return true;
      });
    }

    // Apply sorts
    if (this.sorts.length > 0) {
      items.sort((a, b) => {
        for (const s of this.sorts) {
          const va = a[s.field];
          const vb = b[s.field];
          if (va < vb) return s.direction === 'desc' ? 1 : -1;
          if (va > vb) return s.direction === 'desc' ? -1 : 1;
        }
        return 0;
      });
    }

    // Apply offset
    if (this.offsetCount) {
      items = items.slice(this.offsetCount);
    }

    // Apply limit
    if (this.limitCount !== null) {
      items = items.slice(0, this.limitCount);
    }

    const docs = items.map((item) => ({
      id: String(item.id),
      data: () => item,
      exists: true,
    }));

    return {
      empty: docs.length === 0,
      size: docs.length,
      docs,
      forEach: (cb) => docs.forEach(cb),
    };
  }
}

class MockCollectionReference extends MockQuery {
  constructor(collectionId) {
    super(null, [], [], null, null);
    this.collectionRef = this;
    this.id = collectionId;
    this.dataStore = new Map();
    this.autoIdCounter = 1;
  }

  doc(docId) {
    const id = docId ? String(docId) : String(this.autoIdCounter++);
    return new MockDocumentReference(this, id);
  }

  async add(data) {
    const id = String(this.autoIdCounter++);
    const docRef = new MockDocumentReference(this, id);
    await docRef.set({ ...data, id });
    return docRef;
  }
}

class MockFirestoreAdapter {
  constructor() {
    this.collections = new Map();
  }

  collection(name) {
    if (!this.collections.has(name)) {
      this.collections.set(name, new MockCollectionReference(name));
    }
    return this.collections.get(name);
  }

  async runTransaction(updateFunction) {
    const transaction = {
      get: async (docRef) => docRef.get(),
      set: (docRef, data, opts) => docRef.set(data, opts),
      update: (docRef, data) => docRef.update(data),
      delete: (docRef) => docRef.delete(),
    };
    return updateFunction(transaction);
  }

  batch() {
    const ops = [];
    return {
      set: (ref, data, opts) => ops.push(() => ref.set(data, opts)),
      update: (ref, data) => ops.push(() => ref.update(data)),
      delete: (ref) => ops.push(() => ref.delete()),
      commit: async () => {
        for (const op of ops) {
          await op();
        }
      },
    };
  }
}

const mockDb = new MockFirestoreAdapter();

// Wrapper around Firestore to gracefully delegate to real Firestore or in-memory fallback
const dbWrapper = {
  collection: (name) => {
    if (isRealFirebase && firestoreDb) {
      return firestoreDb.collection(name);
    }
    return mockDb.collection(name);
  },
  runTransaction: (fn) => {
    if (isRealFirebase && firestoreDb) {
      return firestoreDb.runTransaction(fn);
    }
    return mockDb.runTransaction(fn);
  },
  batch: () => {
    if (isRealFirebase && firestoreDb) {
      return firestoreDb.batch();
    }
    return mockDb.batch();
  },
};

// Auth wrapper
const authWrapper = {
  verifyIdToken: async (idToken, checkRevoked = false) => {
    if (isRealFirebase && authService) {
      try {
        return await authService.verifyIdToken(idToken, checkRevoked);
      } catch (err) {
        throw err;
      }
    }
    // Test / Dev verification for Firebase tokens
    if (!idToken) throw new Error('ID token must not be empty');
    const tokenStr = String(idToken).trim();
    if (tokenStr.startsWith('mock-') || tokenStr.startsWith('fb-') || tokenStr.includes('eyJ')) {
      return {
        uid: 'vendor_fb_uid_' + Math.abs(tokenStr.split('').reduce((a, b) => ((a << 5) - a) + b.charCodeAt(0), 0) % 100000),
        email: 'vendor@travelvendor.com',
        role: 'vendor',
      };
    }
    throw new Error('Invalid Firebase ID token in development/test environment');
  },
  createCustomToken: async (uid, developerClaims = {}) => {
    if (isRealFirebase && authService) {
      return await authService.createCustomToken(uid, developerClaims);
    }
    return `custom_fb_token_${uid}_${Date.now()}`;
  },
  createUser: async (properties) => {
    if (isRealFirebase && authService) {
      return await authService.createUser(properties);
    }
    return {
      uid: 'user_' + Date.now(),
      email: properties.email,
      displayName: properties.displayName,
      ...properties,
    };
  },
  getUserByEmail: async (email) => {
    if (isRealFirebase && authService) {
      return await authService.getUserByEmail(email);
    }
    return null;
  },
};

// Storage wrapper
const storageWrapper = {
  bucket: (name) => {
    if (isRealFirebase && storageService) {
      return storageService.bucket(name || config.FIREBASE_STORAGE_BUCKET);
    }
    return {
      file: (filename) => ({
        save: async (buffer, opts) => {
          const uploadsDir = path.resolve(config.UPLOAD_PATH);
          if (!fs.existsSync(uploadsDir)) fs.mkdirSync(uploadsDir, { recursive: true });
          fs.writeFileSync(path.join(uploadsDir, path.basename(filename)), buffer);
          return true;
        },
        getSignedUrl: async () => [`/uploads/${path.basename(filename)}`],
        delete: async () => true,
      }),
    };
  },
};

// Messaging wrapper (FCM)
const messagingWrapper = {
  send: async (payload) => {
    if (isRealFirebase && messagingService) {
      try {
        return await messagingService.send(payload);
      } catch (err) {
        logger.warn('FCM dispatch warning: ' + err.message);
        return { success: false, error: err.message };
      }
    }
    logger.info('FCM simulated push notification: ' + JSON.stringify(payload));
    return { messageId: 'projects/mock/messages/' + Date.now() };
  },
};

module.exports = {
  adminApp,
  db: dbWrapper,
  auth: authWrapper,
  storage: storageWrapper,
  messaging: messagingWrapper,
  getFirestore: () => dbWrapper,
  getAuth: () => authWrapper,
  getStorage: () => storageWrapper,
  getMessaging: () => messagingWrapper,
  getFieldValue: () => ({
    serverTimestamp: () => new Date().toISOString(),
    increment: (n) => n,
    arrayUnion: (...elements) => elements,
    arrayRemove: (...elements) => elements,
  }),
  isRealFirebase,
  mockDb,
};
