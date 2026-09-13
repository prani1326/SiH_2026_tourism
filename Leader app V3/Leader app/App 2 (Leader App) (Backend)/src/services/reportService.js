const fdb = require('./firestoreDb');
const { sanitizeCsvValue } = require('../utils/sanitizer');

function toIsoDate(val) {
  if (!val) return '';
  if (typeof val === 'string') return val.substring(0, 10);
  if (val instanceof Date) return val.toISOString().substring(0, 10);
  if (typeof val.toDate === 'function') return val.toDate().toISOString().substring(0, 10);
  if (val._seconds !== undefined) return new Date(val._seconds * 1000).toISOString().substring(0, 10);
  if (typeof val === 'number') return new Date(val).toISOString().substring(0, 10);
  return String(val).substring(0, 10);
}

class ReportService {
  async daily(query = {}) {
    const date = query.date || new Date().toISOString().split('T')[0];

    const trips = await fdb.find('trips');
    const bookings = await fdb.find('bookings');
    const support = await fdb.find('support_tickets');
    const incidents = await fdb.find('incidents');
    const sos = await fdb.find('sos_cases');

    return {
      date,
      trips: {
        active: trips.filter(
          (t) => t.status === 'active' && toIsoDate(t.start_date) <= date && toIsoDate(t.end_date) >= date
        ).length,
        started_today: trips.filter((t) => toIsoDate(t.start_date) === date).length,
        ending_today: trips.filter((t) => toIsoDate(t.end_date) === date).length,
      },
      bookings: {
        total: bookings.filter((b) => toIsoDate(b.created_at) === date).length,
        confirmed: bookings.filter(
          (b) =>
            toIsoDate(b.created_at) === date &&
            (b.booking_status || b.status) === 'confirmed'
        ).length,
        failed: bookings.filter(
          (b) =>
            toIsoDate(b.created_at) === date &&
            (b.booking_status || b.status) === 'failed'
        ).length,
        cancelled: bookings.filter(
          (b) =>
            toIsoDate(b.created_at) === date &&
            (b.booking_status || b.status) === 'cancelled'
        ).length,
      },
      support: {
        new_tickets: support.filter((s) => toIsoDate(s.created_at) === date).length,
        resolved: support.filter((s) => toIsoDate(s.resolved_at) === date).length,
      },
      incidents: {
        new: incidents.filter((i) => toIsoDate(i.created_at) === date).length,
        resolved: incidents.filter((i) => toIsoDate(i.resolved_at) === date).length,
      },
      sos: {
        triggered: sos.filter((s) => toIsoDate(s.created_at) === date).length,
        resolved: sos.filter((s) => toIsoDate(s.resolved_at) === date).length,
      },
    };
  }

  async bookings(query = {}) {
    const startDate = query.start_date || new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0];
    const endDate = query.end_date || new Date().toISOString().split('T')[0];
    let all = await fdb.find('bookings');

    all = all.filter((b) => {
      const d = toIsoDate(b.created_at);
      return d >= startDate && d <= endDate;
    });

    if (query.destination) {
      const q = query.destination.toLowerCase();
      all = all.filter((b) => (b.service_location || '').toLowerCase().includes(q));
    }

    const byStatusMap = {};
    const byServiceMap = {};
    let totalRevenue = 0;
    let cancelledCount = 0;

    all.forEach((b) => {
      const status = b.booking_status || b.status || 'unknown';
      byStatusMap[status] = (byStatusMap[status] || 0) + 1;

      const service = b.service_type || 'other';
      if (!byServiceMap[service]) byServiceMap[service] = { count: 0, total_amount: 0 };
      byServiceMap[service].count++;
      const amt = Number(b.amount || b.total_amount || 0);
      byServiceMap[service].total_amount += amt;

      if (b.payment_status === 'paid') {
        totalRevenue += amt;
      }
      if (status === 'cancelled') {
        cancelledCount++;
      }
    });

    const cancellation_rate = all.length > 0 ? Math.round((cancelledCount / all.length) * 1000) / 10 : 0;

    return {
      period: { start: startDate, end: endDate },
      total: all.length,
      by_status: Object.entries(byStatusMap).map(([booking_status, count]) => ({ booking_status, count })),
      by_service: Object.entries(byServiceMap).map(([service_type, s]) => ({ service_type, count: s.count, total_amount: s.total_amount })),
      revenue: { total: totalRevenue },
      cancellation_rate: { rate: cancellation_rate },
    };
  }

  async support(query = {}) {
    const startDate = query.start_date || new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0];
    const endDate = query.end_date || new Date().toISOString().split('T')[0];
    let tickets = await fdb.find('support_tickets');

    tickets = tickets.filter((s) => {
      const d = toIsoDate(s.created_at);
      return d >= startDate && d <= endDate;
    });

    const byCategoryMap = {};
    const byPriorityMap = {};
    let totalResolutionHours = 0;
    let resolvedCount = 0;

    tickets.forEach((t) => {
      byCategoryMap[t.category] = (byCategoryMap[t.category] || 0) + 1;
      byPriorityMap[t.priority] = (byPriorityMap[t.priority] || 0) + 1;

      if (t.resolved_at) {
        totalResolutionHours += (new Date(t.resolved_at) - new Date(t.created_at)) / (1000 * 60 * 60);
        resolvedCount++;
      }
    });

    const avgHours = resolvedCount > 0 ? totalResolutionHours / resolvedCount : null;

    return {
      period: { start: startDate, end: endDate },
      total: tickets.length,
      by_category: Object.entries(byCategoryMap)
        .map(([category, count]) => ({ category, count }))
        .sort((a, b) => b.count - a.count),
      by_priority: Object.entries(byPriorityMap).map(([priority, count]) => ({ priority, count })),
      avg_resolution_hours: { hours: avgHours ? Math.round(avgHours * 10) / 10 : null },
    };
  }

  async incidents(query = {}) {
    const startDate = query.start_date || new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0];
    const endDate = query.end_date || new Date().toISOString().split('T')[0];
    let all = await fdb.find('incidents');

    all = all.filter((i) => {
      const d = toIsoDate(i.created_at);
      return d >= startDate && d <= endDate;
    });

    const byTypeMap = {};
    const bySeverityMap = {};
    let resolvedCount = 0;

    all.forEach((i) => {
      byTypeMap[i.type] = (byTypeMap[i.type] || 0) + 1;
      bySeverityMap[i.severity] = (bySeverityMap[i.severity] || 0) + 1;
      if (i.resolved_at || ['resolved', 'closed'].includes(i.status)) {
        resolvedCount++;
      }
    });

    return {
      period: { start: startDate, end: endDate },
      total: all.length,
      by_type: Object.entries(byTypeMap)
        .map(([type, count]) => ({ type, count }))
        .sort((a, b) => b.count - a.count),
      by_severity: Object.entries(bySeverityMap).map(([severity, count]) => ({ severity, count })),
      resolved: resolvedCount,
    };
  }

  async safety(query = {}) {
    const startDate = query.start_date || new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0];
    const endDate = query.end_date || new Date().toISOString().split('T')[0];

    const sos = await fdb.find('sos_cases');
    const periodSos = sos.filter((s) => {
      const d = toIsoDate(s.created_at);
      return d >= startDate && d <= endDate;
    });

    let totalMinutes = 0;
    let ackCount = 0;
    periodSos.forEach((s) => {
      if (s.acknowledged_at) {
        totalMinutes += (new Date(s.acknowledged_at) - new Date(s.created_at)) / (1000 * 60);
        ackCount++;
      }
    });

    const lostTourists = await fdb.find('lost_tourists');
    const periodLost = lostTourists.filter((l) => {
      const d = toIsoDate(l.created_at);
      return d >= startDate && d <= endDate;
    });

    const alerts = await fdb.find('safety_alerts');
    const periodAlerts = alerts.filter((a) => {
      const d = toIsoDate(a.created_at);
      return d >= startDate && d <= endDate;
    });

    return {
      period: { start: startDate, end: endDate },
      sos_count: periodSos.length,
      sos_avg_response_min: { minutes: ackCount > 0 ? Math.round((totalMinutes / ackCount) * 10) / 10 : null },
      lost_tourists: periodLost.length,
      safety_alerts: periodAlerts.length,
    };
  }

  async partners(query = {}) {
    const all = await fdb.find('partners');
    const complaints = await fdb.find('partner_complaints');

    const byStatusMap = {};
    all.forEach((p) => {
      const s = p.verification_status || p.status || 'unknown';
      byStatusMap[s] = (byStatusMap[s] || 0) + 1;
    });

    const top_rated = all
      .filter((p) => (p.verification_status || p.status) === 'verified')
      .sort((a, b) => (b.rating || 0) - (a.rating || 0))
      .slice(0, 10)
      .map((p) => ({ id: p.id, name: p.name, type: p.type || p.category, rating: p.rating, review_count: p.review_count || 0 }));

    const complaintCounts = {};
    complaints.forEach((c) => {
      complaintCounts[c.partner_id] = (complaintCounts[c.partner_id] || 0) + 1;
    });

    const most_complaints = Object.entries(complaintCounts)
      .map(([partner_id, count]) => {
        const p = all.find((item) => item.id === partner_id) || {};
        return { id: partner_id, name: p.name || 'Unknown', type: p.type || p.category, complaint_count: count };
      })
      .sort((a, b) => b.complaint_count - a.complaint_count)
      .slice(0, 10);

    const flagged = all
      .filter((p) => p.flagged === 1 || p.flagged === true)
      .map((p) => ({ id: p.id, name: p.name, type: p.type || p.category, flag_reason: p.flag_reason }));

    return {
      total: all.length,
      by_status: Object.entries(byStatusMap).map(([verification_status, count]) => ({ verification_status, count })),
      top_rated,
      most_complaints,
      flagged,
    };
  }

  async destinations(query = {}) {
    const trips = await fdb.find('trips', [['status', '==', 'active']]);
    const incidents = await fdb.find('incidents');
    const activeIncidents = incidents.filter((i) => !['resolved', 'closed'].includes(i.status));

    const map = {};
    trips.forEach((t) => {
      const key = `${t.destination}_${t.destination_country}`;
      if (!map[key]) {
        map[key] = {
          destination: t.destination,
          destination_country: t.destination_country,
          active_trips: 0,
          total_tourists: 0,
        };
      }
      map[key].active_trips++;
      map[key].total_tourists += t.group_size || 1;
    });

    const incidentHotspots = {};
    activeIncidents.forEach((i) => {
      const dest = i.destination || 'General';
      incidentHotspots[dest] = (incidentHotspots[dest] || 0) + 1;
    });

    return {
      active_destinations: Object.values(map).sort((a, b) => b.active_trips - a.active_trips),
      incident_hotspots: Object.entries(incidentHotspots)
        .map(([destination, count]) => ({ destination, incidents: count }))
        .sort((a, b) => b.incidents - a.incidents)
        .slice(0, 10),
    };
  }

  async exportData(type, query) {
    let data;
    switch (type) {
      case 'daily': data = await this.daily(query); break;
      case 'bookings': data = await this.bookings(query); break;
      case 'support': data = await this.support(query); break;
      case 'incidents': data = await this.incidents(query); break;
      case 'safety': data = await this.safety(query); break;
      case 'partners': data = await this.partners(query); break;
      case 'destinations': data = await this.destinations(query); break;
      default: data = {};
    }

    if (query.format === 'csv') {
      return { format: 'csv', data: this._toCsv(data) };
    }
    return { format: 'json', data };
  }

  _toCsv(obj, prefix = '') {
    const rows = [];
    for (const [key, value] of Object.entries(obj)) {
      if (Array.isArray(value)) {
        if (value.length > 0 && typeof value[0] === 'object') {
          const headers = Object.keys(value[0]);
          rows.push(headers.map((h) => sanitizeCsvValue(h)).join(','));
          for (const row of value) {
            rows.push(headers.map((h) => JSON.stringify(sanitizeCsvValue(row[h] ?? ''))).join(','));
          }
        }
      } else if (typeof value === 'object' && value !== null) {
        rows.push(`\n# ${prefix}${key}`);
        rows.push(this._toCsv(value, `${key}.`));
      } else {
        rows.push(`${prefix}${key},${JSON.stringify(sanitizeCsvValue(value))}`);
      }
    }
    return rows.join('\n');
  }
}

module.exports = new ReportService();
