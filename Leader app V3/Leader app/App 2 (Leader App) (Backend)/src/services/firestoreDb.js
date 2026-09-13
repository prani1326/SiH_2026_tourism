const { db } = require('../config/firebase');
const { v4: uuidv4 } = require('uuid');

class FirestoreHelper {
  get db() {
    return db;
  }

  collection(name) {
    return db.collection(name);
  }

  /**
   * Find documents matching filters
   * filters: array of [field, op, value] e.g. [['status', '==', 'active']]
   * options: { orderBy: [field, 'asc'|'desc'], limit: number, offset: number }
   */
  async find(collectionName, filters = [], options = {}) {
    let query = db.collection(collectionName);

    for (const filter of filters) {
      if (Array.isArray(filter) && filter.length === 3) {
        query = query.where(filter[0], filter[1], filter[2]);
      }
    }

    let orderedQuery = query;
    let hasOrderBy = false;
    let orderField = null;
    let orderDir = 'asc';

    if (options.orderBy) {
      hasOrderBy = true;
      if (Array.isArray(options.orderBy)) {
        orderField = options.orderBy[0];
        orderDir = options.orderBy[1] || 'asc';
      } else if (typeof options.orderBy === 'string') {
        orderField = options.orderBy;
        orderDir = 'asc';
      }
      orderedQuery = orderedQuery.orderBy(orderField, orderDir);
    }

    if (options.offset !== undefined && options.offset !== null) {
      orderedQuery = orderedQuery.offset(options.offset);
    }

    if (options.limit !== undefined && options.limit !== null) {
      orderedQuery = orderedQuery.limit(options.limit);
    }

    let snapshot;
    try {
      snapshot = await orderedQuery.get();
    } catch (err) {
      if (err.code === 9 || (err.message && err.message.includes('requires an index'))) {
        let fallbackQuery = query;
        if (options.limit !== undefined && options.limit !== null) {
          fallbackQuery = fallbackQuery.limit(options.limit);
        }
        snapshot = await fallbackQuery.get();
      } else {
        throw err;
      }
    }

    const results = [];
    snapshot.forEach((doc) => {
      results.push({ id: doc.id, ...doc.data() });
    });

    if (hasOrderBy && orderField) {
      results.sort((a, b) => {
        const valA = a[orderField] || '';
        const valB = b[orderField] || '';
        const cmp = typeof valA === 'string' ? String(valA).localeCompare(String(valB)) : (valA > valB ? 1 : (valA < valB ? -1 : 0));
        return orderDir === 'desc' ? -cmp : cmp;
      });
    }

    return results;
  }

  /**
   * Find single document matching filters
   */
  async findOne(collectionName, filters = []) {
    const results = await this.find(collectionName, filters, { limit: 1 });
    return results.length > 0 ? results[0] : null;
  }

  /**
   * Find single document by its Document ID
   */
  async findById(collectionName, id) {
    if (!id) return null;
    const docRef = db.collection(collectionName).doc(String(id));
    const snapshot = await docRef.get();
    if (!snapshot.exists) return null;
    return { id: snapshot.id, ...snapshot.data() };
  }

  /**
   * Insert new document with auto or custom ID
   */
  async insert(collectionName, data, customId = null) {
    const id = String(customId || data.id || uuidv4());
    const docData = { ...data, id };
    const docRef = db.collection(collectionName).doc(id);
    await docRef.set(docData, { merge: true });
    return docData;
  }

  /**
   * Set document with custom ID
   */
  async set(collectionName, id, data, options = { merge: true }) {
    return this.insert(collectionName, data, id);
  }

  async upsert(collectionName, id, data) {
    return this.insert(collectionName, data, id);
  }

  /**
   * Update existing document
   */
  async update(collectionName, id, data) {
    if (!id) throw new Error('Document ID required for update');
    const docRef = db.collection(collectionName).doc(String(id));
    await docRef.set(data, { merge: true });
    const updated = await docRef.get();
    return { id: updated.id, ...updated.data() };
  }

  /**
   * Delete document by ID
   */
  async delete(collectionName, id) {
    if (!id) return;
    const docRef = db.collection(collectionName).doc(String(id));
    await docRef.delete();
    return true;
  }

  /**
   * Count documents matching filters
   */
  async count(collectionName, filters = []) {
    let query = db.collection(collectionName);
    for (const filter of filters) {
      if (Array.isArray(filter) && filter.length === 3) {
        query = query.where(filter[0], filter[1], filter[2]);
      }
    }
    const countSnapshot = await query.count().get();
    return countSnapshot.data().count;
  }

  /**
   * Run atomic transaction
   */
  async runTransaction(callback) {
    return db.runTransaction(callback);
  }

  /**
   * Batch writer
   */
  batch() {
    return db.batch();
  }
}

module.exports = new FirestoreHelper();
