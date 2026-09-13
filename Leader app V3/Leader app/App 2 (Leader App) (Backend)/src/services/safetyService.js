const fdb = require('./firestoreDb');
const { v4: uuidv4 } = require('uuid');
const { NotFoundError } = require('../utils/errors');
const { getPagination, getPaginationMeta } = require('../utils/pagination');

class SafetyService {
  async getOverview() {
    const allSos = await fdb.find('sos_cases');
    const activeSos = allSos.filter((s) => ['active', 'acknowledged', 'responding'].includes(s.status));
    activeSos.sort((a, b) => ((b.created_at || '') > (a.created_at || '') ? 1 : -1));

    const checkins = await fdb.find('tourist_checkins', [], {
      orderBy: ['created_at', 'desc'],
      limit: 20,
    });
    const enrichedCheckins = await Promise.all(
      checkins.map(async (tc) => {
        const tourist = (await fdb.findById('tourists', tc.tourist_id)) || {};
        return { ...tc, full_name: tourist.full_name };
      })
    );

    const safety_alerts = await fdb.find('safety_alerts', [['active', '==', 1]], {
      orderBy: ['created_at', 'desc'],
    });

    const allIncidents = await fdb.find('incidents');
    const activeIncidents = allIncidents.filter((i) => !['resolved', 'closed'].includes(i.status));

    // High risk destinations count
    const destCounts = {};
    activeIncidents.forEach((inc) => {
      const dest = inc.destination || 'General';
      destCounts[dest] = (destCounts[dest] || 0) + 1;
    });
    const high_risk_destinations = Object.entries(destCounts)
      .map(([destination, incident_count]) => ({ destination, incident_count }))
      .sort((a, b) => b.incident_count - a.incident_count)
      .slice(0, 10);

    const allLost = await fdb.find('lost_tourists');
    const missing_tourists = allLost
      .filter((l) => ['missing', 'searching'].includes(l.status))
      .sort((a, b) => ((b.created_at || '') > (a.created_at || '') ? 1 : -1));

    return {
      active_sos: activeSos.length,
      active_sos_cases: activeSos.slice(0, 10).map((s) => ({
        id: s.id,
        tourist_name: s.tourist_name,
        location: s.location,
        status: s.status,
        created_at: s.created_at,
      })),
      recent_checkins: enrichedCheckins,
      safety_alerts,
      high_risk_destinations,
      missing_tourists,
      active_incidents: activeIncidents.length,
    };
  }

  async getAlerts() {
    const alerts = await fdb.find('safety_alerts', [['active', '==', 1]]);
    return alerts.sort((a, b) => ((b.created_at || '') > (a.created_at || '') ? 1 : -1));
  }

  async getCheckins(query) {
    const { page, limit, offset } = getPagination(query);
    let allCheckins = await fdb.find('tourist_checkins');

    if (query.trip_id) {
      allCheckins = allCheckins.filter((c) => c.trip_id === query.trip_id);
    }
    if (query.tourist_id) {
      allCheckins = allCheckins.filter((c) => c.tourist_id === query.tourist_id);
    }

    const total = allCheckins.length;
    allCheckins.sort((a, b) => ((b.created_at || '') > (a.created_at || '') ? 1 : -1));
    const paged = allCheckins.slice(offset, offset + limit);

    const data = await Promise.all(
      paged.map(async (tc) => {
        const tourist = (await fdb.findById('tourists', tc.tourist_id)) || {};
        return { ...tc, tourist_name: tourist.full_name };
      })
    );

    return { data, pagination: getPaginationMeta(page, limit, total) };
  }

  async getLostTourists() {
    const all = await fdb.find('lost_tourists');
    return all
      .filter((l) => ['missing', 'searching'].includes(l.status))
      .sort((a, b) => ((b.created_at || '') > (a.created_at || '') ? 1 : -1));
  }

  async getLostTouristById(id) {
    const lost = await fdb.findById('lost_tourists', id);
    if (!lost) throw new NotFoundError('Lost Tourist Record');

    let tourist = null;
    if (lost.tourist_id) {
      tourist = await fdb.findById('tourists', lost.tourist_id);
    }

    let trip = null;
    if (lost.trip_id) {
      trip = await fdb.findById('trips', lost.trip_id);
    }

    return { ...lost, tourist, trip };
  }

  async lostTouristAction(id, user, actionType, data = {}) {
    const lost = await fdb.findById('lost_tourists', id);
    if (!lost) throw new NotFoundError('Lost Tourist Record');

    const now = new Date().toISOString();

    switch (actionType) {
      case 'set_meeting_point':
        await fdb.update('lost_tourists', id, {
          meeting_point: data.meeting_point,
          updated_at: now,
        });
        break;
      case 'mark_found':
        await fdb.update('lost_tourists', id, {
          status: 'found',
          found_at: now,
          resolution_notes: data.notes || 'Tourist found',
          updated_at: now,
        });
        if (lost.tourist_id) {
          await fdb.update('tourists', lost.tourist_id, {
            status: 'active',
            updated_at: now,
          });
        }
        break;
      case 'close':
        await fdb.update('lost_tourists', id, {
          status: 'closed',
          resolution_notes: data.notes || 'Case closed',
          updated_at: now,
        });
        break;
      case 'escalate':
        await fdb.update('lost_tourists', id, {
          status: 'searching',
          updated_at: now,
        });
        break;
      default:
        break;
    }

    return { message: `Action '${actionType}' completed` };
  }

  async getLostPhoneInfo(touristId) {
    const tourist = await fdb.findById('tourists', touristId);
    if (!tourist) throw new NotFoundError('Tourist');

    const currentTrip = tourist.current_trip_id
      ? await fdb.findById('trips', tourist.current_trip_id)
      : null;

    const allBookings = currentTrip
      ? await fdb.find('bookings', [['trip_id', '==', currentTrip.id]])
      : [];
    const emergencyBookings = allBookings.filter(
      (b) => (b.booking_status || b.status) === 'confirmed'
    );

    return {
      tourist: {
        id: tourist.id,
        full_name: tourist.full_name,
        emergency_contact_name: tourist.emergency_contact_name,
        emergency_contact_phone: tourist.emergency_contact_phone,
      },
      current_trip: currentTrip,
      active_bookings: emergencyBookings,
    };
  }

  async lostPhoneAction(touristId, user, actionType, data = {}) {
    const tourist = await fdb.findById('tourists', touristId);
    if (!tourist) throw new NotFoundError('Tourist');
    return { message: `Lost phone action '${actionType}' logged for tourist ${tourist.full_name}` };
  }
}

module.exports = new SafetyService();
