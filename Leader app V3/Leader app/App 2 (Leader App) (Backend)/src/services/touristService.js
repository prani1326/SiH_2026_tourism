const fdb = require('./firestoreDb');
const { v4: uuidv4 } = require('uuid');
const { NotFoundError } = require('../utils/errors');
const { getPagination, getPaginationMeta } = require('../utils/pagination');

class TouristService {
  async list(query) {
    const { page, limit, offset } = getPagination(query);
    let allTourists = await fdb.find('tourists');

    if (query.status) {
      allTourists = allTourists.filter((t) => t.status === query.status);
    }
    if (query.country) {
      allTourists = allTourists.filter((t) => t.country === query.country);
    }
    if (query.search) {
      const q = query.search.toLowerCase();
      allTourists = allTourists.filter((t) =>
        (t.full_name && t.full_name.toLowerCase().includes(q)) ||
        (t.email && t.email.toLowerCase().includes(q)) ||
        (t.phone && t.phone.toLowerCase().includes(q))
      );
    }

    const total = allTourists.length;
    allTourists.sort((a, b) => ((b.created_at || '') > (a.created_at || '') ? 1 : -1));
    const paged = allTourists.slice(offset, offset + limit);

    const data = paged.map((t) => ({
      id: t.id,
      full_name: t.full_name,
      email: t.email,
      phone: t.phone,
      country: t.country,
      language: t.language,
      current_trip_id: t.current_trip_id,
      current_location: t.current_location,
      status: t.status,
      created_at: t.created_at,
    }));

    return { data, pagination: getPaginationMeta(page, limit, total) };
  }

  async getById(id) {
    const tourist = await fdb.findById('tourists', id);
    if (!tourist) throw new NotFoundError('Tourist');

    // Get trips
    const members = await fdb.find('trip_members', [['tourist_id', '==', id]]);
    const trips = await Promise.all(
      members.map(async (m) => {
        const trip = (await fdb.findById('trips', m.trip_id)) || {};
        return {
          id: trip.id,
          destination: trip.destination,
          start_date: trip.start_date,
          end_date: trip.end_date,
          status: trip.status,
          safety_status: trip.safety_status,
        };
      })
    );
    trips.sort((a, b) => ((b.start_date || '') > (a.start_date || '') ? 1 : -1));

    const bookings = await fdb.count('bookings', [['tourist_id', '==', id]]);
    const incidents = await fdb.count('incidents', [['tourist_id', '==', id]]);
    const support_tickets = await fdb.count('support_tickets', [['tourist_id', '==', id]]);

    return {
      ...tourist,
      trips,
      bookings_count: bookings,
      incidents_count: incidents,
      support_tickets_count: support_tickets,
    };
  }

  async getTrips(touristId) {
    await this._checkExists(touristId);
    const members = await fdb.find('trip_members', [['tourist_id', '==', touristId]]);
    const trips = await Promise.all(
      members.map(async (m) => fdb.findById('trips', m.trip_id))
    );
    return trips.filter(Boolean).sort((a, b) => ((b.start_date || '') > (a.start_date || '') ? 1 : -1));
  }

  async getBookings(touristId) {
    await this._checkExists(touristId);
    return fdb.find('bookings', [['tourist_id', '==', touristId]], {
      orderBy: ['created_at', 'desc'],
    });
  }

  async getIncidents(touristId) {
    await this._checkExists(touristId);
    return fdb.find('incidents', [['tourist_id', '==', touristId]], {
      orderBy: ['created_at', 'desc'],
    });
  }

  async getNotes(touristId) {
    await this._checkExists(touristId);
    return fdb.find('tourist_notes', [['tourist_id', '==', touristId]], {
      orderBy: ['created_at', 'desc'],
    });
  }

  async addNote(touristId, user, content, type = 'general') {
    await this._checkExists(touristId);
    const id = uuidv4();
    await fdb.insert(
      'tourist_notes',
      {
        id,
        tourist_id: touristId,
        created_by: user.id,
        created_by_name: user.full_name,
        content,
        type,
        created_at: new Date().toISOString(),
      },
      id
    );
    return { id, message: 'Note added' };
  }

  async _checkExists(id) {
    const tourist = await fdb.findById('tourists', id);
    if (!tourist) throw new NotFoundError('Tourist');
  }
}

module.exports = new TouristService();
