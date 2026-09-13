const fdb = require('./firestoreDb');
const { v4: uuidv4 } = require('uuid');
const { NotFoundError, BadRequestError } = require('../utils/errors');
const { getPagination, getPaginationMeta } = require('../utils/pagination');
const { sanitizeText } = require('../utils/sanitizer');

function toIsoDate(val) {
  if (!val) return '';
  if (typeof val === 'string') return val.substring(0, 10);
  if (val instanceof Date) return val.toISOString().substring(0, 10);
  if (typeof val.toDate === 'function') return val.toDate().toISOString().substring(0, 10);
  if (val._seconds !== undefined) return new Date(val._seconds * 1000).toISOString().substring(0, 10);
  if (typeof val === 'number') return new Date(val).toISOString().substring(0, 10);
  return String(val).substring(0, 10);
}

class BookingService {
  async list(query) {
    const { page, limit, offset } = getPagination(query);
    let allBookings = await fdb.find('bookings');

    if (query.booking_status) {
      allBookings = allBookings.filter((b) => (b.booking_status || b.status) === query.booking_status);
    }
    if (query.payment_status) {
      allBookings = allBookings.filter((b) => b.payment_status === query.payment_status);
    }
    if (query.service_type) {
      allBookings = allBookings.filter((b) => b.service_type === query.service_type);
    }
    if (query.tourist_id) {
      allBookings = allBookings.filter((b) => b.tourist_id === query.tourist_id);
    }
    if (query.trip_id) {
      allBookings = allBookings.filter((b) => b.trip_id === query.trip_id);
    }
    if (query.start_date) {
      allBookings = allBookings.filter((b) => toIsoDate(b.created_at) >= query.start_date);
    }
    if (query.end_date) {
      allBookings = allBookings.filter((b) => toIsoDate(b.created_at) <= query.end_date);
    }

    const total = allBookings.length;
    allBookings.sort((a, b) => ((b.created_at || '') > (a.created_at || '') ? 1 : -1));
    const paged = allBookings.slice(offset, offset + limit);

    const data = await Promise.all(
      paged.map(async (b) => {
        const tourist = (await fdb.findById('tourists', b.tourist_id)) || {};
        return {
          ...b,
          tourist_name: tourist.full_name || b.tourist_name,
        };
      })
    );

    return { data, pagination: getPaginationMeta(page, limit, total) };
  }

  async getById(id) {
    const booking = await fdb.findById('bookings', id);
    if (!booking) throw new NotFoundError('Booking');

    const tourist = (await fdb.findById('tourists', booking.tourist_id)) || {};
    const notes = await fdb.find('booking_notes', [['booking_id', '==', id]], {
      orderBy: ['created_at', 'desc'],
    });

    return {
      ...booking,
      tourist_name: tourist.full_name || booking.tourist_name,
      tourist_email: tourist.email,
      tourist_phone: tourist.phone,
      notes,
    };
  }

  async confirm(id, user, data = {}) {
    const booking = await this._getBooking(id);
    const currentStatus = booking.booking_status || booking.status;
    if (!['requested', 'pending'].includes(currentStatus)) {
      throw new BadRequestError(`Cannot confirm booking in '${currentStatus}' status`);
    }

    const now = new Date().toISOString();
    await fdb.update('bookings', id, {
      booking_status: 'confirmed',
      status: 'confirmed',
      payment_status: 'paid',
      updated_at: now,
    });
    await this._addNote(id, user, sanitizeText(`Booking confirmed. ${data.notes || ''}`), 'general');

    return { message: 'Booking confirmed' };
  }

  async cancel(id, user, data = {}) {
    const booking = await this._getBooking(id);
    const currentStatus = booking.booking_status || booking.status;
    if (['cancelled', 'refunded'].includes(currentStatus)) {
      throw new BadRequestError('Booking already cancelled or refunded');
    }

    const now = new Date().toISOString();
    await fdb.update('bookings', id, {
      booking_status: 'cancelled',
      status: 'cancelled',
      cancellation_status: 'processed',
      updated_at: now,
    });
    await this._addNote(id, user, sanitizeText(`Booking cancelled. Reason: ${data.reason || 'N/A'}`), 'general');

    return { message: 'Booking cancelled' };
  }

  async processRefund(id, user, data = {}) {
    const booking = await this._getBooking(id);
    const now = new Date().toISOString();
    const refundAmount = booking.amount || booking.total_amount || 0;

    await fdb.update('bookings', id, {
      booking_status: 'refunded',
      status: 'refunded',
      refund_status: 'completed',
      refund_amount: refundAmount,
      updated_at: now,
    });
    await this._addNote(
      id,
      user,
      sanitizeText(`Refund processed. Amount: ${refundAmount} ${booking.currency || 'INR'}. ${data.notes || ''}`),
      'refund'
    );

    return { message: 'Refund processed', amount: refundAmount };
  }

  async escalate(id, user, data = {}) {
    await this._getBooking(id);
    await this._addNote(id, user, sanitizeText(`ESCALATED: ${data.reason || 'No reason provided'}. ${data.notes || ''}`), 'escalation');
    return { message: 'Booking escalated' };
  }

  async getNotes(id) {
    await this._getBooking(id);
    return fdb.find('booking_notes', [['booking_id', '==', id]], {
      orderBy: ['created_at', 'desc'],
    });
  }

  async addNote(id, user, content, type = 'general') {
    await this._getBooking(id);
    return this._addNote(id, user, sanitizeText(content), type);
  }

  async _getBooking(id) {
    const booking = await fdb.findById('bookings', id);
    if (!booking) throw new NotFoundError('Booking');
    return booking;
  }

  async _addNote(bookingId, user, content, type) {
    const id = uuidv4();
    await fdb.insert(
      'booking_notes',
      {
        id,
        booking_id: bookingId,
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
}

module.exports = new BookingService();
