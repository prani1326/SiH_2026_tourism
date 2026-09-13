const fdb = require('./firestoreDb');

class SearchService {
  /**
   * Global search across all Firestore entities
   */
  async search(query) {
    const q = (query.q || '').trim();
    if (!q || q.length < 2) return { results: [] };

    const lowerQ = q.toLowerCase();
    const limit = parseInt(query.limit, 10) || 10;

    const [allTourists, allTrips, allBookings, allIncidents, allTickets, allPartners, allSos] =
      await Promise.all([
        fdb.find('tourists'),
        fdb.find('trips'),
        fdb.find('bookings'),
        fdb.find('incidents'),
        fdb.find('support_tickets'),
        fdb.find('partners'),
        fdb.find('sos_cases'),
      ]);

    const tourists = allTourists
      .filter((t) =>
        (t.full_name && t.full_name.toLowerCase().includes(lowerQ)) ||
        (t.email && t.email.toLowerCase().includes(lowerQ)) ||
        (t.phone && t.phone.toLowerCase().includes(lowerQ))
      )
      .slice(0, limit)
      .map((t) => ({ id: t.id, title: t.full_name, subtitle: t.email, type: 'tourist' }));

    const trips = allTrips
      .filter((t) =>
        (t.id && t.id.toLowerCase().includes(lowerQ)) ||
        (t.destination && t.destination.toLowerCase().includes(lowerQ)) ||
        (t.title && t.title.toLowerCase().includes(lowerQ))
      )
      .slice(0, limit)
      .map((t) => ({ id: t.id, title: t.destination || t.title, subtitle: t.status, type: 'trip' }));

    const bookings = allBookings
      .filter((b) =>
        (b.id && b.id.toLowerCase().includes(lowerQ)) ||
        (b.service_name && b.service_name.toLowerCase().includes(lowerQ)) ||
        (b.service_type && b.service_type.toLowerCase().includes(lowerQ)) ||
        (b.booking_reference && b.booking_reference.toLowerCase().includes(lowerQ))
      )
      .slice(0, limit)
      .map((b) => ({
        id: b.id,
        title: b.service_name || b.service_type || b.booking_reference,
        subtitle: b.booking_status || b.status,
        type: 'booking',
      }));

    const incidents = allIncidents
      .filter((i) =>
        (i.id && i.id.toLowerCase().includes(lowerQ)) ||
        (i.description && i.description.toLowerCase().includes(lowerQ)) ||
        (i.tourist_name && i.tourist_name.toLowerCase().includes(lowerQ)) ||
        (i.destination && i.destination.toLowerCase().includes(lowerQ))
      )
      .slice(0, limit)
      .map((i) => ({ id: i.id, title: i.type, subtitle: i.status, type: 'incident' }));

    const tickets = allTickets
      .filter((t) =>
        (t.id && t.id.toLowerCase().includes(lowerQ)) ||
        (t.subject && t.subject.toLowerCase().includes(lowerQ))
      )
      .slice(0, limit)
      .map((t) => ({ id: t.id, title: t.subject, subtitle: t.status, type: 'support_ticket' }));

    const partners = allPartners
      .filter((p) =>
        (p.id && p.id.toLowerCase().includes(lowerQ)) ||
        (p.name && p.name.toLowerCase().includes(lowerQ)) ||
        (p.email && p.email.toLowerCase().includes(lowerQ))
      )
      .slice(0, limit)
      .map((p) => ({ id: p.id, title: p.name, subtitle: p.type || p.category, type: 'partner' }));

    const sosCases = allSos
      .filter((s) =>
        (s.id && s.id.toLowerCase().includes(lowerQ)) ||
        (s.tourist_name && s.tourist_name.toLowerCase().includes(lowerQ)) ||
        (s.location && s.location.toLowerCase().includes(lowerQ))
      )
      .slice(0, limit)
      .map((s) => ({ id: s.id, title: s.tourist_name, subtitle: s.status, type: 'sos' }));

    return {
      results: [
        ...tourists,
        ...trips,
        ...bookings,
        ...incidents,
        ...tickets,
        ...partners,
        ...sosCases,
      ],
      query: q,
    };
  }
}

module.exports = new SearchService();
