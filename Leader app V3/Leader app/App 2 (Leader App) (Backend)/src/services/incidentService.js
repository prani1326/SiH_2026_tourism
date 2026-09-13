const fdb = require('./firestoreDb');
const { v4: uuidv4 } = require('uuid');
const { NotFoundError, BadRequestError } = require('../utils/errors');
const { getPagination, getPaginationMeta } = require('../utils/pagination');
const { sanitizeText } = require('../utils/sanitizer');
const fcmService = require('./fcmService');

class IncidentService {
  async list(query) {
    const { page, limit, offset } = getPagination(query);
    let allIncidents = await fdb.find('incidents');

    if (query.type) {
      allIncidents = allIncidents.filter((i) => i.type === query.type);
    }
    if (query.severity) {
      allIncidents = allIncidents.filter((i) => i.severity === query.severity);
    }
    if (query.status) {
      allIncidents = allIncidents.filter((i) => i.status === query.status);
    }
    if (query.destination) {
      const q = query.destination.toLowerCase();
      allIncidents = allIncidents.filter((i) => i.destination && i.destination.toLowerCase().includes(q));
    }
    if (query.assigned_ops) {
      allIncidents = allIncidents.filter((i) => i.assigned_ops_id === query.assigned_ops);
    }

    const total = allIncidents.length;
    const severityOrder = { emergency: 1, critical: 2, high: 3, medium: 4, low: 5 };
    allIncidents.sort((a, b) => {
      const pA = severityOrder[a.severity] || 5;
      const pB = severityOrder[b.severity] || 5;
      if (pA !== pB) return pA - pB;
      return (b.created_at || '') > (a.created_at || '') ? 1 : -1;
    });

    const data = allIncidents.slice(offset, offset + limit);
    return { data, pagination: getPaginationMeta(page, limit, total) };
  }

  async create(data, user) {
    const id = uuidv4();
    let touristName = null;
    if (data.tourist_id) {
      const tourist = await fdb.findById('tourists', data.tourist_id);
      touristName = tourist ? tourist.full_name : null;
    }

    const cleanDescription = sanitizeText(data.description);
    const cleanLocation = data.location ? sanitizeText(data.location) : null;
    const cleanDestination = data.destination ? sanitizeText(data.destination) : null;
    const now = new Date().toISOString();

    const incidentData = {
      id,
      type: data.type,
      severity: data.severity,
      tourist_id: data.tourist_id || null,
      tourist_name: touristName,
      trip_id: data.trip_id || null,
      location: cleanLocation,
      gps_lat: data.gps_lat || null,
      gps_lng: data.gps_lng || null,
      destination: cleanDestination,
      description: cleanDescription,
      status: 'open',
      workflow_stage: 'reported',
      created_at: now,
      updated_at: now,
    };

    await fdb.insert('incidents', incidentData, id);
    await this._addTimeline(id, user, 'Incident reported', 'update');

    // Push notification if high or emergency
    if (['critical', 'emergency', 'high'].includes(data.severity)) {
      fcmService.sendToOpsLeaders(
        `INCIDENT ALERT: [${data.severity.toUpperCase()}]`,
        `${data.type} reported at ${cleanLocation || cleanDestination || 'unknown location'}: ${cleanDescription}`,
        { incidentId: id, severity: data.severity }
      ).catch(() => {});
    }

    return { id, message: 'Incident created' };
  }

  async getById(id) {
    const incident = await fdb.findById('incidents', id);
    if (!incident) throw new NotFoundError('Incident');

    const timeline = await fdb.find('incident_timeline', [['incident_id', '==', id]], {
      orderBy: ['created_at', 'asc'],
    });
    const notes = await fdb.find('incident_notes', [['incident_id', '==', id]], {
      orderBy: ['created_at', 'desc'],
    });
    return { ...incident, timeline, notes };
  }

  async assign(id, opsLeaderId, user) {
    await this._getIncident(id);
    const ops = await fdb.findById('users', opsLeaderId);
    if (!ops) throw new NotFoundError('Ops Leader');

    const now = new Date().toISOString();
    await fdb.update('incidents', id, {
      assigned_ops_id: ops.id,
      assigned_ops_name: ops.full_name,
      workflow_stage: 'assigned',
      status: 'investigating',
      updated_at: now,
    });
    await this._addTimeline(id, user, `Assigned to ${ops.full_name}`, 'action');

    return { message: 'Incident assigned' };
  }

  async addTimeline(id, user, action, type = 'update') {
    await this._getIncident(id);
    return this._addTimeline(id, user, sanitizeText(action), type);
  }

  async advanceWorkflow(id, stage, user, notes) {
    await this._getIncident(id);
    const cleanNotes = notes ? sanitizeText(notes) : '';
    const now = new Date().toISOString();

    await fdb.update('incidents', id, {
      workflow_stage: stage,
      updated_at: now,
    });
    await this._addTimeline(id, user, `Workflow advanced to '${stage}'. ${cleanNotes}`, 'action');

    return { message: `Workflow advanced to '${stage}'` };
  }

  async resolve(id, user, data = {}) {
    await this._getIncident(id);
    const now = new Date().toISOString();
    const cleanResolution = data.resolution ? sanitizeText(data.resolution) : 'Resolved';
    const cleanFinalReport = data.final_report ? sanitizeText(data.final_report) : null;

    await fdb.update('incidents', id, {
      status: 'resolved',
      workflow_stage: 'report_created',
      resolution: cleanResolution,
      final_report: cleanFinalReport,
      resolved_at: now,
      updated_at: now,
    });
    await this._addTimeline(id, user, `Incident resolved: ${cleanResolution}`, 'resolution');

    return { message: 'Incident resolved' };
  }

  async getNotes(id) {
    await this._getIncident(id);
    return fdb.find('incident_notes', [['incident_id', '==', id]], {
      orderBy: ['created_at', 'desc'],
    });
  }

  async addNote(id, user, content, type = 'general') {
    await this._getIncident(id);
    const noteId = uuidv4();
    await fdb.insert(
      'incident_notes',
      {
        id: noteId,
        incident_id: id,
        created_by: user.id,
        created_by_name: user.full_name,
        content: sanitizeText(content),
        type,
        created_at: new Date().toISOString(),
      },
      noteId
    );
    return { id: noteId, message: 'Note added' };
  }

  async _getIncident(id) {
    const incident = await fdb.findById('incidents', id);
    if (!incident) throw new NotFoundError('Incident');
    return incident;
  }

  async _addTimeline(incidentId, user, action, type) {
    const id = uuidv4();
    await fdb.insert(
      'incident_timeline',
      {
        id,
        incident_id: incidentId,
        user_id: user ? user.id : null,
        user_name: user ? user.full_name : 'System',
        type,
        action,
        created_at: new Date().toISOString(),
      },
      id
    );
    return { id };
  }
}

module.exports = new IncidentService();
