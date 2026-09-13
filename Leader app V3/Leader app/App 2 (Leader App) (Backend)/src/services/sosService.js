const fdb = require('./firestoreDb');
const { v4: uuidv4 } = require('uuid');
const { NotFoundError } = require('../utils/errors');
const { getPagination, getPaginationMeta } = require('../utils/pagination');
const { sanitizeText } = require('../utils/sanitizer');
const fcmService = require('./fcmService');

class SosService {
  async list(query) {
    const { page, limit, offset } = getPagination(query);
    let allSos = await fdb.find('sos_cases');

    if (query.status) {
      allSos = allSos.filter((s) => s.status === query.status);
    }

    const total = allSos.length;
    const statusOrder = { active: 1, acknowledged: 2, responding: 3, resolved: 4, closed: 5 };
    allSos.sort((a, b) => {
      const pA = statusOrder[a.status] || 4;
      const pB = statusOrder[b.status] || 4;
      if (pA !== pB) return pA - pB;
      return (b.created_at || '') > (a.created_at || '') ? 1 : -1;
    });

    const data = allSos.slice(offset, offset + limit);
    return { data, pagination: getPaginationMeta(page, limit, total) };
  }

  async getById(id) {
    const sos = await fdb.findById('sos_cases', id);
    if (!sos) throw new NotFoundError('SOS Case');

    let tourist = null;
    if (sos.tourist_id) {
      tourist = await fdb.findById('tourists', sos.tourist_id);
    }

    let trip = null;
    if (sos.trip_id) {
      trip = await fdb.findById('trips', sos.trip_id);
    }

    const allIncidents = sos.tourist_id
      ? await fdb.find('incidents', [['tourist_id', '==', sos.tourist_id]], {
          orderBy: ['created_at', 'desc'],
          limit: 10,
        })
      : [];

    const timeline = await fdb.find('sos_timeline', [['sos_id', '==', id]], {
      orderBy: ['created_at', 'asc'],
    });

    const actions = await fdb.find('sos_actions', [['sos_id', '==', id]], {
      orderBy: ['created_at', 'asc'],
    });

    return {
      ...sos,
      tourist,
      trip,
      incident_history: allIncidents,
      timeline,
      actions,
    };
  }

  async acknowledge(id, user) {
    await this._getSos(id);
    const now = new Date().toISOString();

    await fdb.update('sos_cases', id, {
      status: 'acknowledged',
      acknowledged_by: user.id,
      acknowledged_by_name: user.full_name,
      acknowledged_at: now,
      updated_at: now,
    });

    await this._addAction(id, user, 'acknowledge', 'SOS acknowledged');
    await this._addTimeline(id, user, 'SOS acknowledged', 'action');

    return { message: 'SOS acknowledged' };
  }

  async dispatch(id, user, data = {}) {
    const sos = await this._getSos(id);
    const now = new Date().toISOString();
    const responderName = data.responder_name || data.name || 'Emergency Response Unit';

    await fdb.update('sos_cases', id, {
      status: 'responding',
      dispatched_to: data.responder_id || 'unit-01',
      responder_name: responderName,
      updated_at: now,
    });

    await this._addAction(id, user, 'dispatch', `Dispatched responder: ${responderName}`);
    await this._addTimeline(id, user, `Responder dispatched: ${responderName}`, 'action');

    // Send FCM alert to ops team
    fcmService.sendToOpsLeaders(
      'SOS RESPONDER DISPATCHED',
      `Responder ${responderName} dispatched to SOS case ${sos.case_number || id}`,
      { sosId: id, status: 'responding' }
    ).catch(() => {});

    return { message: 'Responder dispatched', responder: responderName };
  }

  async logAction(id, user, actionType, notes) {
    await this._getSos(id);
    const cleanNotes = notes ? sanitizeText(notes) : '';
    const now = new Date().toISOString();

    await this._addAction(id, user, actionType, cleanNotes);
    await this._addTimeline(id, user, `Action: ${actionType}. ${cleanNotes}`, 'action');

    if (actionType === 'response_started') {
      await fdb.update('sos_cases', id, {
        status: 'responding',
        updated_at: now,
      });
    }

    return { message: `Action '${actionType}' logged` };
  }

  async getTimeline(id) {
    await this._getSos(id);
    return fdb.find('sos_timeline', [['sos_id', '==', id]], {
      orderBy: ['created_at', 'asc'],
    });
  }

  async addTimeline(id, user, entry, type = 'update') {
    await this._getSos(id);
    return this._addTimeline(id, user, sanitizeText(entry), type);
  }

  async resolve(id, user, notes) {
    await this._getSos(id);
    const now = new Date().toISOString();
    const cleanNotes = notes ? sanitizeText(notes) : 'Resolved';

    await fdb.update('sos_cases', id, {
      status: 'resolved',
      resolved_at: now,
      resolution_notes: cleanNotes,
      updated_at: now,
    });

    await this._addAction(id, user, 'resolve', cleanNotes);
    await this._addTimeline(id, user, `SOS resolved: ${cleanNotes}`, 'resolution');

    return { message: 'SOS resolved' };
  }

  async _getSos(id) {
    const sos = await fdb.findById('sos_cases', id);
    if (!sos) throw new NotFoundError('SOS Case');
    return sos;
  }

  async _addAction(sosId, user, actionType, notes) {
    const id = uuidv4();
    await fdb.insert(
      'sos_actions',
      {
        id,
        sos_id: sosId,
        action_type: actionType,
        user_id: user.id,
        user_name: user.full_name,
        notes: notes || null,
        created_at: new Date().toISOString(),
      },
      id
    );
    return { id };
  }

  async _addTimeline(sosId, user, entry, type) {
    const id = uuidv4();
    await fdb.insert(
      'sos_timeline',
      {
        id,
        sos_id: sosId,
        user_id: user ? user.id : null,
        user_name: user ? user.full_name : 'System',
        entry,
        type,
        created_at: new Date().toISOString(),
      },
      id
    );
    return { id };
  }
}

module.exports = new SosService();
