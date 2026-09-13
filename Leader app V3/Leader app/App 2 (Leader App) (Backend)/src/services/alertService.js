const fdb = require('./firestoreDb');
const { v4: uuidv4 } = require('uuid');
const { NotFoundError } = require('../utils/errors');
const { getPagination, getPaginationMeta } = require('../utils/pagination');
const fcmService = require('./fcmService');

class AlertService {
  async list(query) {
    const { page, limit, offset } = getPagination(query);
    let allAlerts = await fdb.find('alerts');

    if (query.type) {
      allAlerts = allAlerts.filter((a) => a.type === query.type);
    }
    if (query.priority || query.severity) {
      const p = query.priority || query.severity;
      allAlerts = allAlerts.filter((a) => (a.priority || a.severity) === p);
    }
    if (query.status) {
      allAlerts = allAlerts.filter((a) => a.status === query.status);
    }
    if (query.destination) {
      const q = query.destination.toLowerCase();
      allAlerts = allAlerts.filter((a) => a.destination && a.destination.toLowerCase().includes(q));
    }

    const total = allAlerts.length;
    const priorityOrder = { emergency: 1, critical: 2, high: 3, medium: 4, low: 5 };
    allAlerts.sort((a, b) => {
      const pA = priorityOrder[a.priority || a.severity] || 4;
      const pB = priorityOrder[b.priority || b.severity] || 4;
      if (pA !== pB) return pA - pB;
      return (b.created_at || '') > (a.created_at || '') ? 1 : -1;
    });

    const data = allAlerts.slice(offset, offset + limit);
    return { data, pagination: getPaginationMeta(page, limit, total) };
  }

  async getById(id) {
    const alert = await fdb.findById('alerts', id);
    if (!alert) throw new NotFoundError('Alert');
    return alert;
  }

  async acknowledge(id, user) {
    const alert = await this.getById(id);
    const now = new Date().toISOString();
    await fdb.update('alerts', id, {
      status: 'acknowledged',
      acknowledged: 1,
      acknowledged_by: user.id,
      acknowledged_at: now,
      updated_at: now,
    });
    return { ...alert, status: 'acknowledged', acknowledged: 1, acknowledged_at: now };
  }

  async assign(id, opsLeaderId, user) {
    const alert = await this.getById(id);
    const ops = await fdb.findById('users', opsLeaderId);
    if (!ops) throw new NotFoundError('Ops Leader');

    const now = new Date().toISOString();
    await fdb.update('alerts', id, {
      assigned_ops_id: ops.id,
      assigned_ops_name: ops.full_name,
      status: 'in_progress',
      updated_at: now,
    });
    return { ...alert, assigned_ops_id: ops.id, status: 'in_progress' };
  }

  async resolve(id, user) {
    const alert = await this.getById(id);
    const now = new Date().toISOString();
    await fdb.update('alerts', id, {
      status: 'resolved',
      resolved_at: now,
      resolved_by: user.id,
      updated_at: now,
    });
    return { ...alert, status: 'resolved' };
  }

  async create(data) {
    const id = uuidv4();
    const persistent = ['emergency', 'critical'].includes(data.priority || data.severity);
    const now = new Date().toISOString();

    const alertDoc = {
      id,
      type: data.type,
      priority: data.priority || data.severity || 'medium',
      severity: data.severity || data.priority || 'medium',
      tourist_id: data.tourist_id || null,
      tourist_name: data.tourist_name || null,
      trip_id: data.trip_id || null,
      location: data.location || null,
      destination: data.destination || null,
      description: data.description || data.message,
      message: data.message || data.description,
      status: 'active',
      required_action: data.required_action || null,
      source_type: data.source_type || null,
      source_id: data.source_id || null,
      persistent: persistent ? 1 : 0,
      created_at: now,
      updated_at: now,
    };

    await fdb.insert('alerts', alertDoc, id);

    if (persistent) {
      fcmService.sendToOpsLeaders(
        `CRITICAL OPS ALERT: ${alertDoc.priority.toUpperCase()}`,
        alertDoc.description,
        { alertId: id, priority: alertDoc.priority }
      ).catch(() => {});
    }

    return { id, ...alertDoc };
  }
}

module.exports = new AlertService();
