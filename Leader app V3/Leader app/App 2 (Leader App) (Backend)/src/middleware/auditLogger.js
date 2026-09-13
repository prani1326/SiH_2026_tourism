const fdb = require('../services/firestoreDb');
const { v4: uuidv4 } = require('uuid');

/**
 * Audit logger middleware factory.
 * Automatically logs mutation operations to the Firestore audit_log collection.
 *
 * Usage: router.post('/endpoint', auditLog('entity_type', 'action'), handler)
 */
function auditLog(entityType, action) {
  return (req, res, next) => {
    const originalJson = res.json.bind(res);

    res.json = function (body) {
      if (res.statusCode >= 200 && res.statusCode < 300 && req.user) {
        const entityId = req.params.id || (body && body.data && body.data.id) || null;
        const id = uuidv4();
        fdb.insert('audit_log', {
          id,
          user_id: req.user.id,
          user_name: req.user.full_name,
          user_role: req.user.role,
          action,
          entity_type: entityType,
          entity_id: entityId,
          previous_state: req.body._previousState ? JSON.stringify(req.body._previousState) : null,
          new_state: body.data ? JSON.stringify(body.data) : null,
          reason: req.body.reason || req.body.notes || null,
          ip_address: req.ip || req.connection?.remoteAddress || 'unknown',
          created_at: new Date().toISOString(),
        }, id).catch((err) => {
          console.error('Audit log write to Firestore failed:', err.message);
        });
      }
      return originalJson(body);
    };
    next();
  };
}

module.exports = { auditLog };
