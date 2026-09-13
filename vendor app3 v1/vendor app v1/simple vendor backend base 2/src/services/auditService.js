const { db } = require('../config/firebase');
const { COLLECTIONS } = require('../database/firestoreSchema');
const logger = require('../utils/logger');

const auditService = {
  logAction: async ({ actorType = 'system', actorId = null, action, entityType = null, entityId = null, details = null, ipAddress = null }) => {
    try {
      const col = db.collection(COLLECTIONS.AUDIT_LOGS);
      const allSnap = await col.get();
      const nextId = allSnap.size + 1;

      const logEntry = {
        id: nextId,
        actor_type: actorType,
        actor_id: actorId ? (Number(actorId) || actorId) : null,
        action,
        entity_type: entityType,
        entity_id: entityId ? (Number(entityId) || entityId) : null,
        details: details && typeof details === 'object' ? JSON.stringify(details) : details,
        ip_address: ipAddress,
        created_at: new Date().toISOString(),
      };

      await col.doc(String(nextId)).set(logEntry);
      return logEntry;
    } catch (err) {
      logger.error('Audit log error: ' + err.message);
    }
  },

  getLogs: async ({ page = 1, limit = 50, actorType, entityType }) => {
    const pageNum = parseInt(page, 10) || 1;
    const limitNum = parseInt(limit, 10) || 50;

    let query = db.collection(COLLECTIONS.AUDIT_LOGS);
    if (actorType) {
      query = query.where('actor_type', '==', actorType);
    }
    if (entityType) {
      query = query.where('entity_type', '==', entityType);
    }

    const snap = await query.get();
    let allLogs = snap.docs.map((d) => d.data());

    allLogs.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));

    const total = allLogs.length;
    const offset = (pageNum - 1) * limitNum;
    const paged = allLogs.slice(offset, offset + limitNum);

    return {
      logs: paged,
      total,
      page: pageNum,
      limit: limitNum,
    };
  },
};

module.exports = auditService;
