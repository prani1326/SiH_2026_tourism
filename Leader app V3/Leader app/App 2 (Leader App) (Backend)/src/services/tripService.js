const fdb = require('./firestoreDb');
const { v4: uuidv4 } = require('uuid');
const { NotFoundError } = require('../utils/errors');
const { getPagination, getPaginationMeta } = require('../utils/pagination');

class TripService {
  async list(query) {
    const { page, limit, offset } = getPagination(query);
    let allTrips = await fdb.find('trips');

    if (query.status) {
      allTrips = allTrips.filter((t) => t.status === query.status);
    }
    if (query.destination) {
      const q = query.destination.toLowerCase();
      allTrips = allTrips.filter((t) => t.destination && t.destination.toLowerCase().includes(q));
    }
    if (query.safety_status) {
      allTrips = allTrips.filter((t) => t.safety_status === query.safety_status);
    }
    if (query.country) {
      allTrips = allTrips.filter((t) => t.destination_country === query.country);
    }
    if (query.priority) {
      allTrips = allTrips.filter((t) => t.priority === query.priority);
    }
    if (query.assigned_ops) {
      allTrips = allTrips.filter((t) => t.assigned_ops_leader === query.assigned_ops);
    }
    if (query.start_date) {
      allTrips = allTrips.filter((t) => t.start_date >= query.start_date);
    }
    if (query.end_date) {
      allTrips = allTrips.filter((t) => t.end_date <= query.end_date);
    }
    if (query.region) {
      allTrips = allTrips.filter((t) => t.region === query.region);
    }

    const total = allTrips.length;

    // Sort by safety_status priority, then start_date DESC
    const safetyOrder = { emergency: 1, critical: 2, warning: 3, attention: 4, normal: 5 };
    allTrips.sort((a, b) => {
      const pA = safetyOrder[a.safety_status] || 5;
      const pB = safetyOrder[b.safety_status] || 5;
      if (pA !== pB) return pA - pB;
      return (b.start_date || '') > (a.start_date || '') ? 1 : -1;
    });

    const pagedTrips = allTrips.slice(offset, offset + limit);

    // Calculate member counts
    const allMembers = await fdb.find('trip_members');
    const data = pagedTrips.map((trip) => {
      const count = allMembers.filter((m) => m.trip_id === trip.id).length;
      return { ...trip, member_count: count };
    });

    return { data, pagination: getPaginationMeta(page, limit, total) };
  }

  async getById(id) {
    const trip = await fdb.findById('trips', id);
    if (!trip) throw new NotFoundError('Trip');

    // Get members with tourist details
    const members = await fdb.find('trip_members', [['trip_id', '==', id]]);
    const enrichedMembers = await Promise.all(
      members.map(async (m) => {
        const tourist = (await fdb.findById('tourists', m.tourist_id)) || {};
        return {
          ...m,
          full_name: tourist.full_name,
          email: tourist.email,
          phone: tourist.phone,
          country: tourist.country,
          tourist_status: tourist.status,
        };
      })
    );

    // Get counts
    const bookings = await fdb.count('bookings', [['trip_id', '==', id]]);
    const allIncidents = await fdb.find('incidents', [['trip_id', '==', id]]);
    const openIncidents = allIncidents.filter((inc) => !['resolved', 'closed'].includes(inc.status)).length;

    const allSos = await fdb.find('sos_cases', [['trip_id', '==', id]]);
    const activeSos = allSos.filter((s) => ['active', 'acknowledged', 'responding'].includes(s.status)).length;

    return {
      ...trip,
      members: enrichedMembers,
      bookings_count: bookings,
      open_incidents: openIncidents,
      active_sos: activeSos,
    };
  }

  async getItinerary(tripId) {
    await this.getById(tripId);
    const items = await fdb.find('itinerary_items', [['trip_id', '==', tripId]]);
    return items.sort((a, b) => {
      if (a.day_number !== b.day_number) return a.day_number - b.day_number;
      return (a.sort_order || 0) - (b.sort_order || 0);
    });
  }

  async getSafety(tripId) {
    const trip = await this.getById(tripId);
    const checkins = await fdb.find('tourist_checkins', [['trip_id', '==', tripId]], {
      orderBy: ['created_at', 'desc'],
      limit: 20,
    });
    const allAlerts = await fdb.find('safety_alerts', [['active', '==', 1]]);
    const alerts = allAlerts.filter(
      (a) => a.destination === trip.destination || a.region === trip.region
    );
    return { safety_status: trip.safety_status, checkins, alerts };
  }

  async getBookings(tripId) {
    await this.getById(tripId);
    return fdb.find('bookings', [['trip_id', '==', tripId]], {
      orderBy: ['service_date', 'asc'],
    });
  }

  async getMessages(tripId) {
    await this.getById(tripId);
    return fdb.find('messages', [['trip_id', '==', tripId]], {
      orderBy: ['created_at', 'desc'],
      limit: 50,
    });
  }

  async getActivityLog(tripId) {
    await this.getById(tripId);
    return fdb.find('trip_activity_log', [['trip_id', '==', tripId]], {
      orderBy: ['created_at', 'desc'],
      limit: 100,
    });
  }

  async assign(tripId, payload, user) {
    const trip = await this.getById(tripId);
    const now = new Date().toISOString();
    const updateData = { updated_at: now };
    let assignedTarget = '';

    const vendorId = payload.vendor_id || payload.vendorId;
    const vendorName = payload.vendor_name || payload.vendorName;
    const opsLeaderId = payload.ops_leader_id || payload.opsLeaderId;

    if (vendorId || vendorName) {
      updateData.assigned_vendor_id = vendorId || 'partner';
      updateData.assigned_vendor_name = vendorName || 'Field Partner';
      updateData.status = 'ACCEPTED';
      if (payload.notes) updateData.assignment_notes = payload.notes;
      assignedTarget = updateData.assigned_vendor_name;
    }

    if (opsLeaderId) {
      const ops = await fdb.findById('users', opsLeaderId);
      if (ops) {
        updateData.assigned_ops_leader = ops.id;
        updateData.assigned_ops_name = ops.full_name || ops.name;
        assignedTarget = updateData.assigned_ops_name;
      }
    }

    await fdb.update('trips', tripId, updateData);

    const logId = uuidv4();
    await fdb.insert(
      'trip_activity_log',
      {
        id: logId,
        trip_id: tripId,
        user_id: user.id,
        user_name: user.full_name || user.name || 'Ops Leader',
        action: 'assignment_updated',
        previous_state: trip.assigned_vendor_name || trip.assigned_ops_name || 'unassigned',
        new_state: assignedTarget,
        created_at: now,
      },
      logId
    );

    const updatedTrip = await this.getById(tripId);
    return updatedTrip;
  }

  async getNotes(tripId) {
    await this.getById(tripId);
    const logs = await fdb.find('trip_activity_log', [
      ['trip_id', '==', tripId],
      ['action', '==', 'internal_note'],
    ], { orderBy: ['created_at', 'desc'] });
    return logs;
  }

  async addNote(tripId, user, content) {
    await this.getById(tripId);
    const id = uuidv4();
    await fdb.insert(
      'trip_activity_log',
      {
        id,
        trip_id: tripId,
        user_id: user.id,
        user_name: user.full_name,
        action: 'internal_note',
        notes: content,
        created_at: new Date().toISOString(),
      },
      id
    );
    return { id, message: 'Note added' };
  }

  async updateSafetyStatus(tripId, safetyStatus, user) {
    const trip = await this.getById(tripId);
    const now = new Date().toISOString();
    await fdb.update('trips', tripId, {
      safety_status: safetyStatus,
      updated_at: now,
    });

    const logId = uuidv4();
    await fdb.insert(
      'trip_activity_log',
      {
        id: logId,
        trip_id: tripId,
        user_id: user?.id || 'system',
        user_name: user?.full_name || 'System',
        action: 'safety_status_updated',
        previous_state: trip.safety_status,
        new_state: safetyStatus,
        created_at: now,
      },
      logId
    );

    return { ...trip, safety_status: safetyStatus, updated_at: now };
  }
}

module.exports = new TripService();
