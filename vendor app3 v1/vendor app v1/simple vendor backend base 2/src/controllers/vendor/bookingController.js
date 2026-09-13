const { db } = require('../../config/firebase');
const { COLLECTIONS } = require('../../database/firestoreSchema');
const { sendSuccess, sendError } = require('../../utils/responseHelper');
const auditService = require('../../services/auditService');
const notificationService = require('../../services/notificationService');

const bookingController = {
  /**
   * Get vendor bookings with filter and pagination
   */
  getBookings: async (req, res) => {
    try {
      const vId = Number(req.vendorId) || req.vendorId;
      const { status, page = 1, limit = 20 } = req.query;
      const pageNum = parseInt(page, 10) || 1;
      const limitNum = parseInt(limit, 10) || 20;

      const bookingsCol = db.collection(COLLECTIONS.BOOKINGS);
      const snap = await bookingsCol.where('vendor_id', '==', vId).get();

      let allBookings = snap.docs.map((d) => d.data());

      // Counts per status
      let pendingCount = 0;
      let acceptedCount = 0;
      let rejectedCount = 0;
      let completedCount = 0;

      allBookings.forEach((b) => {
        if (b.status === 'pending') pendingCount++;
        if (b.status === 'accepted') acceptedCount++;
        if (b.status === 'rejected') rejectedCount++;
        if (b.status === 'completed') completedCount++;
      });

      if (status) {
        allBookings = allBookings.filter((b) => b.status === status);
      }

      allBookings.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));

      const total = allBookings.length;
      const offset = (pageNum - 1) * limitNum;
      const paged = allBookings.slice(offset, offset + limitNum);

      // Join details for each booking
      const enriched = await Promise.all(
        paged.map(async (b) => {
          let listing_title = null;
          let listing_destination = null;
          let listing_duration = null;
          let traveller_name = null;
          let traveller_email = null;
          let traveller_mobile = null;
          let traveller_photo = null;
          let trip_id = null;
          let trip_status = null;

          if (b.listing_id) {
            const lDoc = await db.collection(COLLECTIONS.LISTINGS).doc(String(b.listing_id)).get();
            if (lDoc.exists) {
              const l = lDoc.data();
              listing_title = l.title;
              listing_destination = l.destination;
              listing_duration = l.duration;
            }
          }

          if (b.traveller_id) {
            const tDoc = await db.collection(COLLECTIONS.TRAVELLERS).doc(String(b.traveller_id)).get();
            if (tDoc.exists) {
              const t = tDoc.data();
              traveller_name = t.name;
              traveller_email = t.email;
              traveller_mobile = t.mobile;
              traveller_photo = t.profile_photo;
            }
          }

          const trSnap = await db.collection(COLLECTIONS.TRIPS).where('booking_id', '==', Number(b.id) || b.id).get();
          if (!trSnap.empty) {
            const tr = trSnap.docs[0].data();
            trip_id = tr.id;
            trip_status = tr.status;
          }

          return {
            ...b,
            listing_title,
            listing_destination,
            listing_duration,
            traveller_name,
            traveller_email,
            traveller_mobile,
            traveller_photo,
            trip_id,
            trip_status,
          };
        })
      );

      return sendSuccess(res, 'Bookings retrieved successfully', {
        bookings: enriched,
        counts: {
          total: snap.size,
          pending: pendingCount,
          accepted: acceptedCount,
          rejected: rejectedCount,
          completed: completedCount,
        },
        page: pageNum,
        limit: limitNum,
      });
    } catch (err) {
      return sendError(res, 'Failed to fetch bookings: ' + err.message, null, 500);
    }
  },

  /**
   * Get single booking details
   */
  getBookingById: async (req, res) => {
    try {
      const { id } = req.params;
      const vId = Number(req.vendorId) || req.vendorId;

      const col = db.collection(COLLECTIONS.BOOKINGS);
      let doc = await col.doc(String(id)).get();
      let booking = null;

      if (doc.exists) {
        booking = doc.data();
      } else {
        const snap = await col.where('id', '==', Number(id) || id).get();
        if (!snap.empty) booking = snap.docs[0].data();
      }

      if (!booking || (Number(booking.vendor_id) !== vId && String(booking.vendor_id) !== String(vId))) {
        return sendError(res, 'Booking request not found or unauthorized', null, 404);
      }

      let listing_title = null;
      let listing_description = null;
      let listing_destination = null;
      let listing_duration = null;
      let listing_price = null;
      let traveller_name = null;
      let traveller_email = null;
      let traveller_mobile = null;
      let traveller_photo = null;
      let trip_id = null;
      let trip_status = null;
      let trip_start_time = null;
      let trip_end_time = null;

      if (booking.listing_id) {
        const lDoc = await db.collection(COLLECTIONS.LISTINGS).doc(String(booking.listing_id)).get();
        if (lDoc.exists) {
          const l = lDoc.data();
          listing_title = l.title;
          listing_description = l.description;
          listing_destination = l.destination;
          listing_duration = l.duration;
          listing_price = l.price;
        }
      }

      if (booking.traveller_id) {
        const tDoc = await db.collection(COLLECTIONS.TRAVELLERS).doc(String(booking.traveller_id)).get();
        if (tDoc.exists) {
          const t = tDoc.data();
          traveller_name = t.name;
          traveller_email = t.email;
          traveller_mobile = t.mobile;
          traveller_photo = t.profile_photo;
        }
      }

      const trSnap = await db.collection(COLLECTIONS.TRIPS).where('booking_id', '==', Number(booking.id) || booking.id).get();
      if (!trSnap.empty) {
        const tr = trSnap.docs[0].data();
        trip_id = tr.id;
        trip_status = tr.status;
        trip_start_time = tr.start_time;
        trip_end_time = tr.end_time;
      }

      return sendSuccess(res, 'Booking retrieved successfully', {
        ...booking,
        listing_title,
        listing_description,
        listing_destination,
        listing_duration,
        listing_price,
        traveller_name,
        traveller_email,
        traveller_mobile,
        traveller_photo,
        trip_id,
        trip_status,
        trip_start_time,
        trip_end_time,
      });
    } catch (err) {
      return sendError(res, 'Failed to fetch booking: ' + err.message, null, 500);
    }
  },

  /**
   * Accept booking request (pending -> accepted) & create upcoming trip
   */
  acceptBooking: async (req, res) => {
    try {
      const { id } = req.params;
      const vId = Number(req.vendorId) || req.vendorId;

      const bookingsCol = db.collection(COLLECTIONS.BOOKINGS);
      let docRef = bookingsCol.doc(String(id));
      let doc = await docRef.get();

      if (!doc.exists) {
        const snap = await bookingsCol.where('id', '==', Number(id) || id).get();
        if (!snap.empty) {
          docRef = snap.docs[0].ref;
          doc = snap.docs[0];
        }
      }

      if (!doc.exists) {
        return sendError(res, 'Booking not found or unauthorized', null, 404);
      }

      const booking = doc.data();
      if (Number(booking.vendor_id) !== vId && String(booking.vendor_id) !== String(vId)) {
        return sendError(res, 'Booking not found or unauthorized', null, 404);
      }

      if (booking.status !== 'pending') {
        return sendError(res, `Cannot accept booking. Current status is already '${booking.status}'`, null, 400);
      }

      await docRef.set({
        status: 'accepted',
        updated_at: new Date().toISOString(),
      }, { merge: true });

      // Create upcoming trip if not exists
      const tripsCol = db.collection(COLLECTIONS.TRIPS);
      const tripSnap = await tripsCol.where('booking_id', '==', Number(booking.id) || booking.id).get();
      let trip = null;

      if (tripSnap.empty) {
        const allTrips = await tripsCol.get();
        let maxTripId = 0;
        allTrips.forEach((d) => {
          const nid = Number(d.data().id || d.id);
          if (!isNaN(nid) && nid > maxTripId) maxTripId = nid;
        });
        const nextTripId = maxTripId + 1;

        trip = {
          id: nextTripId,
          booking_id: Number(booking.id) || booking.id,
          vendor_id: vId,
          start_time: null,
          end_time: null,
          status: 'upcoming',
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString(),
        };

        await tripsCol.doc(String(nextTripId)).set(trip);
      } else {
        trip = tripSnap.docs[0].data();
      }

      // Fetch metadata for audit/notifications
      let listing_title = 'Tour Package';
      if (booking.listing_id) {
        const lDoc = await db.collection(COLLECTIONS.LISTINGS).doc(String(booking.listing_id)).get();
        if (lDoc.exists) listing_title = lDoc.data().title;
      }

      await auditService.logAction({
        actorType: 'vendor',
        actorId: vId,
        action: 'Booking Accepted',
        entityType: 'booking',
        entityId: Number(booking.id) || booking.id,
        details: { amount: booking.amount, trip_id: trip.id },
        ipAddress: req.ip,
      });

      try {
        await db.collection(COLLECTIONS.APP_ACTIVITIES).add({
          app_source: 'Vendor Mobile App',
          action: 'Booking Accepted',
          details: `Vendor VND-${vId} accepted booking BKG-${booking.id} (₹${booking.amount})`,
          timestamp: new Date().toISOString(),
        });
      } catch (_) {}

      await notificationService.createNotification({
        recipientType: 'vendor',
        recipientId: vId,
        title: 'Booking Accepted',
        message: `You accepted booking #${booking.id} for "${listing_title}". An upcoming trip has been created.`,
        type: 'booking_accepted',
      });

      return sendSuccess(res, 'Booking accepted successfully and trip scheduled', {
        booking_id: parseInt(id, 10) || id,
        status: 'accepted',
        trip,
      });
    } catch (err) {
      return sendError(res, 'Failed to accept booking: ' + err.message, null, 500);
    }
  },

  /**
   * Reject booking request (pending -> rejected)
   */
  rejectBooking: async (req, res) => {
    try {
      const { id } = req.params;
      const { reason } = req.body;
      const vId = Number(req.vendorId) || req.vendorId;

      const bookingsCol = db.collection(COLLECTIONS.BOOKINGS);
      let docRef = bookingsCol.doc(String(id));
      let doc = await docRef.get();

      if (!doc.exists) {
        const snap = await bookingsCol.where('id', '==', Number(id) || id).get();
        if (!snap.empty) {
          docRef = snap.docs[0].ref;
          doc = snap.docs[0];
        }
      }

      if (!doc.exists) {
        return sendError(res, 'Booking not found or unauthorized', null, 404);
      }

      const booking = doc.data();
      if (Number(booking.vendor_id) !== vId && String(booking.vendor_id) !== String(vId)) {
        return sendError(res, 'Booking not found or unauthorized', null, 404);
      }

      if (booking.status !== 'pending') {
        return sendError(res, `Cannot reject booking. Current status is already '${booking.status}'`, null, 400);
      }

      await docRef.set({
        status: 'rejected',
        rejection_reason: reason || 'Vendor declined request',
        updated_at: new Date().toISOString(),
      }, { merge: true });

      let listing_title = 'Tour Package';
      if (booking.listing_id) {
        const lDoc = await db.collection(COLLECTIONS.LISTINGS).doc(String(booking.listing_id)).get();
        if (lDoc.exists) listing_title = lDoc.data().title;
      }

      await auditService.logAction({
        actorType: 'vendor',
        actorId: vId,
        action: 'Booking Rejected',
        entityType: 'booking',
        entityId: Number(booking.id) || booking.id,
        details: { reason: reason || 'Vendor declined request' },
        ipAddress: req.ip,
      });

      try {
        await db.collection(COLLECTIONS.APP_ACTIVITIES).add({
          app_source: 'Vendor Mobile App',
          action: 'Booking Rejected',
          details: `Vendor VND-${vId} rejected booking BKG-${booking.id}. Reason: ${reason || 'Vendor declined request'}`,
          timestamp: new Date().toISOString(),
        });
      } catch (_) {}

      await notificationService.createNotification({
        recipientType: 'vendor',
        recipientId: vId,
        title: 'Booking Rejected',
        message: `You declined booking #${booking.id} for "${listing_title}".`,
        type: 'booking_rejected',
      });

      return sendSuccess(res, 'Booking rejected successfully', {
        booking_id: parseInt(id, 10) || id,
        status: 'rejected',
      });
    } catch (err) {
      return sendError(res, 'Failed to reject booking: ' + err.message, null, 500);
    }
  },
};

module.exports = bookingController;
