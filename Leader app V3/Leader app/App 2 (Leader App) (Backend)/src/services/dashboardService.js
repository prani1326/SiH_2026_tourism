const fdb = require('./firestoreDb');

function toIsoDate(val) {
  if (!val) return '';
  if (typeof val === 'string') return val.substring(0, 10);
  if (val instanceof Date) return val.toISOString().substring(0, 10);
  if (typeof val.toDate === 'function') return val.toDate().toISOString().substring(0, 10);
  if (val._seconds !== undefined) return new Date(val._seconds * 1000).toISOString().substring(0, 10);
  if (typeof val === 'number') return new Date(val).toISOString().substring(0, 10);
  return String(val).substring(0, 10);
}

class DashboardService {
  /**
   * Get all dashboard KPI stats from Firestore
   */
  async getStats() {
    const today = new Date().toISOString().split('T')[0];

    const [
      tourists,
      trips,
      bookings,
      support,
      sos,
      incidents,
      lostTourists,
      weatherAlerts,
      disruptions,
      partners,
      listings,
      alerts
    ] = await Promise.all([
      fdb.find('tourists'),
      fdb.find('trips'),
      fdb.find('bookings'),
      fdb.find('support_tickets'),
      fdb.find('sos_cases'),
      fdb.find('incidents'),
      fdb.find('lost_tourists'),
      fdb.find('safety_alerts'),
      fdb.find('disruptions'),
      fdb.find('partners'),
      fdb.find('listings'),
      fdb.find('alerts')
    ]);

    return {
      tourists: {
        total_active: tourists.filter((t) => t.status === 'active').length,
        in_emergency: tourists.filter((t) => t.status === 'emergency').length,
        lost: tourists.filter((t) => t.status === 'lost').length,
      },
      trips: {
        active: trips.filter((t) => t.status === 'active').length,
        upcoming: trips.filter((t) => t.status === 'upcoming').length,
        completed: trips.filter((t) => t.status === 'completed').length,
      },
      bookings: {
        today: bookings.filter((b) => toIsoDate(b.created_at) === today).length,
        pending: bookings.filter((b) => (b.booking_status || b.status) === 'pending').length,
        failed: bookings.filter((b) => (b.booking_status || b.status) === 'failed').length,
        cancellation_requests: bookings.filter((b) => b.cancellation_status === 'requested').length,
      },
      support: {
        open_tickets: support.filter((s) => ['open', 'in_progress'].includes(s.status)).length,
        critical_tickets: support.filter(
          (s) => s.priority === 'critical' && !['resolved', 'closed'].includes(s.status)
        ).length,
      },
      safety: {
        active_sos: sos.filter((s) => ['active', 'acknowledged', 'responding'].includes(s.status)).length,
        incidents: incidents.filter((i) => !['resolved', 'closed'].includes(i.status)).length,
        lost_tourists: lostTourists.filter((l) => ['missing', 'searching'].includes(l.status)).length,
      },
      weather: {
        active_alerts: weatherAlerts.filter((a) => a.active === 1 || a.active === true).length,
        active_disruptions: disruptions.filter((d) => d.status === 'active' || d.active === 1).length,
      },
      partners: {
        pending_approvals: partners.filter(
          (p) => (p.verification_status || p.status) === 'pending'
        ).length,
        pending_listings: listings.filter((l) => l.status === 'pending').length,
        flagged: partners.filter((p) => p.flagged === 1 || p.flagged === true).length,
      },
      alerts: {
        critical: alerts.filter(
          (a) =>
            ['critical', 'emergency'].includes(a.priority || a.severity) &&
            a.status === 'active'
        ).length,
        unacknowledged: alerts.filter(
          (a) =>
            a.status === 'active' &&
            (a.persistent === 1 || a.persistent === true) &&
            (!a.acknowledged || a.acknowledged === 0)
        ).length,
      },
    };
  }

  /**
   * Mobile KPI payload format
   */
  async getKpis() {
    const stats = await this.getStats();
    return {
      activeTourists: stats.tourists.total_active,
      activeTrips: stats.trips.active,
      activeSos: stats.safety.active_sos,
      openIncidents: stats.safety.incidents,
      criticalAlerts: stats.alerts.critical,
      openSupportTickets: stats.support.open_tickets,
      total_active_trips: stats.trips.active,
      total_active_tourists: stats.tourists.total_active,
      active_trips: stats.trips.active,
      active_sos: stats.safety.active_sos,
      open_incidents: stats.safety.incidents,
      critical_alerts: stats.alerts.critical,
      open_support_tickets: stats.support.open_tickets,
      timestamp: new Date().toISOString(),
    };
  }

  /**
   * Get chart data for a given type and time range
   */
  async getChartData(type, days = 7) {
    const startDate = new Date(Date.now() - days * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

    switch (type) {
      case 'trips': {
        const trips = await fdb.find('trips');
        const filtered = trips.filter((t) => toIsoDate(t.start_date) >= startDate);
        const map = {};
        filtered.forEach((t) => {
          const d = toIsoDate(t.start_date);
          const key = `${d}_${t.status}`;
          if (!map[key]) map[key] = { date: d, count: 0, status: t.status };
          map[key].count++;
        });
        return Object.values(map).sort((a, b) => (a.date > b.date ? 1 : -1));
      }

      case 'bookings': {
        const bookings = await fdb.find('bookings');
        const filtered = bookings.filter((b) => toIsoDate(b.created_at) >= startDate);
        const map = {};
        filtered.forEach((b) => {
          const date = toIsoDate(b.created_at);
          const status = b.booking_status || b.status;
          const key = `${date}_${status}`;
          if (!map[key]) map[key] = { date, count: 0, status };
          map[key].count++;
        });
        return Object.values(map).sort((a, b) => (a.date > b.date ? 1 : -1));
      }

      case 'support': {
        const tickets = await fdb.find('support_tickets');
        const filtered = tickets.filter((t) => toIsoDate(t.created_at) >= startDate);
        const map = {};
        filtered.forEach((t) => {
          const date = toIsoDate(t.created_at);
          const key = `${date}_${t.priority}`;
          if (!map[key]) map[key] = { date, count: 0, priority: t.priority };
          map[key].count++;
        });
        return Object.values(map).sort((a, b) => (a.date > b.date ? 1 : -1));
      }

      case 'incidents': {
        const incidents = await fdb.find('incidents');
        const filtered = incidents.filter((i) => toIsoDate(i.created_at) >= startDate);
        const map = {};
        filtered.forEach((i) => {
          const date = toIsoDate(i.created_at);
          const key = `${date}_${i.type}`;
          if (!map[key]) map[key] = { date, count: 0, type: i.type };
          map[key].count++;
        });
        return Object.values(map).sort((a, b) => (a.date > b.date ? 1 : -1));
      }

      case 'sos': {
        const sosCases = await fdb.find('sos_cases');
        const filtered = sosCases.filter((s) => toIsoDate(s.created_at) >= startDate);
        const map = {};
        filtered.forEach((s) => {
          const date = toIsoDate(s.created_at);
          const key = `${date}_${s.status}`;
          if (!map[key]) map[key] = { date, count: 0, status: s.status };
          map[key].count++;
        });
        return Object.values(map).sort((a, b) => (a.date > b.date ? 1 : -1));
      }

      case 'destinations': {
        const trips = await fdb.find('trips', [['status', '==', 'active']]);
        const incidents = await fdb.find('incidents');
        const activeIncidents = incidents.filter((i) => !['resolved', 'closed'].includes(i.status));

        const counts = {};
        trips.forEach((t) => {
          counts[t.destination] = (counts[t.destination] || 0) + 1;
        });

        return Object.entries(counts)
          .map(([destination, active_trips]) => ({
            destination,
            active_trips,
            incidents: activeIncidents.filter((i) => i.destination === destination).length,
          }))
          .sort((a, b) => b.active_trips - a.active_trips)
          .slice(0, 20);
      }

      case 'performance': {
        const support = await fdb.find('support_tickets');
        const resolvedSupport = support.filter(
          (s) => s.resolved_at && toIsoDate(s.created_at) >= startDate
        );
        let totalSupportDiff = 0;
        resolvedSupport.forEach((s) => {
          totalSupportDiff += (new Date(s.resolved_at) - new Date(s.created_at)) / (1000 * 60 * 60);
        });
        const avg_hours = resolvedSupport.length > 0 ? totalSupportDiff / resolvedSupport.length : 0;

        const sos = await fdb.find('sos_cases');
        const ackSos = sos.filter(
          (s) => s.acknowledged_at && toIsoDate(s.created_at) >= startDate
        );
        let totalSosDiff = 0;
        ackSos.forEach((s) => {
          totalSosDiff += (new Date(s.acknowledged_at) - new Date(s.created_at)) / (1000 * 60);
        });
        const avg_minutes = ackSos.length > 0 ? totalSosDiff / ackSos.length : 0;

        const bookings = await fdb.find('bookings');
        const periodBookings = bookings.filter((b) => toIsoDate(b.created_at) >= startDate);
        const confirmedBookings = periodBookings.filter(
          (b) => (b.booking_status || b.status) === 'confirmed'
        ).length;
        const rate = periodBookings.length > 0
          ? Math.round((confirmedBookings / periodBookings.length) * 1000) / 10
          : 100;

        return {
          avg_support_response: { avg_hours: Math.round(avg_hours * 10) / 10 },
          avg_sos_response: { avg_minutes: Math.round(avg_minutes * 10) / 10 },
          booking_success_rate: { rate },
        };
      }

      default:
        return [];
    }
  }
}

module.exports = new DashboardService();
