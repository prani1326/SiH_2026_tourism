const fdb = require('./firestoreDb');
const { v4: uuidv4 } = require('uuid');
const { NotFoundError } = require('../utils/errors');
const { getPagination, getPaginationMeta } = require('../utils/pagination');

class WeatherService {
  async listDisruptions(query) {
    const { page, limit, offset } = getPagination(query);
    let allDisruptions = await fdb.find('disruptions');

    if (query.type) {
      allDisruptions = allDisruptions.filter((d) => d.type === query.type);
    }
    if (query.severity) {
      allDisruptions = allDisruptions.filter((d) => d.severity === query.severity);
    }
    if (query.status) {
      allDisruptions = allDisruptions.filter((d) => (d.status || (d.active ? 'active' : 'resolved')) === query.status);
    }
    if (query.region) {
      allDisruptions = allDisruptions.filter((d) => d.region === query.region);
    }

    const total = allDisruptions.length;
    const severityOrder = { critical: 1, high: 2, medium: 3, low: 4 };
    allDisruptions.sort((a, b) => {
      const pA = severityOrder[a.severity] || 4;
      const pB = severityOrder[b.severity] || 4;
      if (pA !== pB) return pA - pB;
      return (b.created_at || '') > (a.created_at || '') ? 1 : -1;
    });

    const paged = allDisruptions.slice(offset, offset + limit);
    const allAffected = await fdb.find('disruption_affected_trips');

    const data = paged.map((d) => {
      const count = allAffected.filter((dat) => dat.disruption_id === d.id).length;
      return {
        ...d,
        affected_trip_count: count,
      };
    });

    return { data, pagination: getPaginationMeta(page, limit, total) };
  }

  async getDisruptionById(id) {
    const disruption = await fdb.findById('disruptions', id);
    if (!disruption) throw new NotFoundError('Disruption');

    const affectedTrips = await fdb.find('disruption_affected_trips', [['disruption_id', '==', id]]);
    const enrichedTrips = await Promise.all(
      affectedTrips.map(async (dat) => {
        const trip = (await fdb.findById('trips', dat.trip_id)) || {};
        return {
          ...dat,
          destination: trip.destination,
          trip_status: trip.status,
          assigned_ops_name: trip.assigned_ops_name,
        };
      })
    );

    return { ...disruption, affected_trips: enrichedTrips };
  }

  async getAffectedTrips(disruptionId) {
    await this._getDisruption(disruptionId);
    const affectedTrips = await fdb.find('disruption_affected_trips', [['disruption_id', '==', disruptionId]]);

    const allMembers = await fdb.find('trip_members');
    const allTourists = await fdb.find('tourists');

    const enriched = await Promise.all(
      affectedTrips.map(async (dat) => {
        const trip = (await fdb.findById('trips', dat.trip_id)) || {};
        const memberTouristIds = allMembers
          .filter((m) => m.trip_id === trip.id)
          .map((m) => m.tourist_id);
        const touristNames = allTourists
          .filter((t) => memberTouristIds.includes(t.id))
          .map((t) => t.full_name)
          .join(', ');

        return {
          ...dat,
          destination: trip.destination,
          start_date: trip.start_date,
          end_date: trip.end_date,
          trip_status: trip.status,
          assigned_ops_name: trip.assigned_ops_name,
          group_size: trip.group_size || memberTouristIds.length,
          tourists: touristNames,
        };
      })
    );

    return enriched;
  }

  async notifyAffectedTourists(disruptionId, user, message, channel) {
    await this._getDisruption(disruptionId);
    const affectedTrips = await fdb.find('disruption_affected_trips', [['disruption_id', '==', disruptionId]]);

    const allMembers = await fdb.find('trip_members');
    const allTourists = await fdb.find('tourists');
    let notified = 0;

    for (const at of affectedTrips) {
      const memberTouristIds = allMembers
        .filter((m) => m.trip_id === at.trip_id)
        .map((m) => m.tourist_id);
      const tourists = allTourists.filter((t) => memberTouristIds.includes(t.id));

      for (const tourist of tourists) {
        const msgId = uuidv4();
        await fdb.insert(
          'messages',
          {
            id: msgId,
            tourist_id: tourist.id,
            trip_id: at.trip_id,
            sender_type: 'ops',
            sender_id: user.id,
            sender_name: user.full_name,
            channel: channel || 'push',
            content_type: 'text',
            content: message,
            priority: 'high',
            created_at: new Date().toISOString(),
          },
          msgId
        );
        notified++;
      }

      await fdb.update('disruption_affected_trips', at.id, {
        notification_status: 'notified',
        notified_at: new Date().toISOString(),
      });
    }

    return { message: `${notified} tourists notified across ${affectedTrips.length} trips` };
  }

  async resolveDisruption(id, user) {
    await this._getDisruption(id);
    const now = new Date().toISOString();
    await fdb.update('disruptions', id, {
      status: 'resolved',
      active: 0,
      resolved_at: now,
      resolved_by: user.id,
      updated_at: now,
    });
    return { message: 'Disruption resolved' };
  }

  async getDestinationAlerts(destination) {
    const alerts = await fdb.find('safety_alerts', [['active', '==', 1]]);
    const q = (destination || '').toLowerCase();
    return alerts
      .filter((a) => a.destination && a.destination.toLowerCase().includes(q))
      .sort((a, b) => ((b.severity || '') > (a.severity || '') ? 1 : -1));
  }

  async _getDisruption(id) {
    const d = await fdb.findById('disruptions', id);
    if (!d) throw new NotFoundError('Disruption');
    return d;
  }
}

module.exports = new WeatherService();
