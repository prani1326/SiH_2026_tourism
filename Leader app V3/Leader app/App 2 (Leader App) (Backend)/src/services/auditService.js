const fdb = require('./firestoreDb');
const { NotFoundError } = require('../utils/errors');
const { getPagination, getPaginationMeta } = require('../utils/pagination');

function toIsoDate(val) {
  if (!val) return '';
  if (typeof val === 'string') return val.substring(0, 10);
  if (val instanceof Date) return val.toISOString().substring(0, 10);
  if (typeof val.toDate === 'function') return val.toDate().toISOString().substring(0, 10);
  if (val._seconds !== undefined) return new Date(val._seconds * 1000).toISOString().substring(0, 10);
  if (typeof val === 'number') return new Date(val).toISOString().substring(0, 10);
  return String(val).substring(0, 10);
}

class AuditService {
  async list(query) {
    const { page, limit, offset } = getPagination(query);
    let allLogs = await fdb.find('audit_log');

    if (query.user_id) {
      allLogs = allLogs.filter((l) => l.user_id === query.user_id);
    }
    if (query.action) {
      const q = query.action.toLowerCase();
      allLogs = allLogs.filter((l) => l.action && l.action.toLowerCase().includes(q));
    }
    if (query.entity_type) {
      allLogs = allLogs.filter((l) => l.entity_type === query.entity_type);
    }
    if (query.entity_id) {
      allLogs = allLogs.filter((l) => l.entity_id === query.entity_id);
    }
    if (query.start_date) {
      allLogs = allLogs.filter((l) => toIsoDate(l.created_at) >= query.start_date);
    }
    if (query.end_date) {
      allLogs = allLogs.filter((l) => toIsoDate(l.created_at) <= query.end_date);
    }

    const total = allLogs.length;
    allLogs.sort((a, b) => ((b.created_at || '') > (a.created_at || '') ? 1 : -1));
    const data = allLogs.slice(offset, offset + limit);

    return { data, pagination: getPaginationMeta(page, limit, total) };
  }

  async getById(id) {
    const entry = await fdb.findById('audit_log', id);
    if (!entry) throw new NotFoundError('Audit Entry');
    return entry;
  }
}

module.exports = new AuditService();
