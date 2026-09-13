const { db } = require('../../config/firebase');
const { COLLECTIONS } = require('../../database/firestoreSchema');
const { sendSuccess, sendError } = require('../../utils/responseHelper');

const travellerController = {
  /**
   * Get travellers associated with authenticated vendor's bookings
   */
  getTravellers: async (req, res) => {
    try {
      const vId = Number(req.vendorId) || req.vendorId;
      const { search, page = 1, limit = 20 } = req.query;
      const pageNum = parseInt(page, 10) || 1;
      const limitNum = parseInt(limit, 10) || 20;

      const bookingsSnap = await db.collection(COLLECTIONS.BOOKINGS).where('vendor_id', '==', vId).get();
      const travellerMap = new Map();

      for (const bDoc of bookingsSnap.docs) {
        const b = bDoc.data();
        if (!b.traveller_id) continue;
        const tid = String(b.traveller_id);

        let tEntry = travellerMap.get(tid);
        if (!tEntry) {
          const tDoc = await db.collection(COLLECTIONS.TRAVELLERS).doc(tid).get();
          if (tDoc.exists) {
            const tData = tDoc.data();
            tEntry = {
              id: Number(tid) || tid,
              name: tData.name,
              email: tData.email,
              mobile: tData.mobile,
              profile_photo: tData.profile_photo,
              total_bookings: 0,
              total_travellers_brought: 0,
              latest_booking_date: b.created_at,
              latest_booking_status: b.status,
              booked_tours: [],
            };
            travellerMap.set(tid, tEntry);
          }
        }

        if (tEntry) {
          tEntry.total_bookings++;
          tEntry.total_travellers_brought += parseInt(b.traveller_count || '1', 10);
          if (new Date(b.created_at) > new Date(tEntry.latest_booking_date)) {
            tEntry.latest_booking_date = b.created_at;
            tEntry.latest_booking_status = b.status;
          }
          if (b.listing_id) {
            const lDoc = await db.collection(COLLECTIONS.LISTINGS).doc(String(b.listing_id)).get();
            if (lDoc.exists && !tEntry.booked_tours.includes(lDoc.data().title)) {
              tEntry.booked_tours.push(lDoc.data().title);
            }
          }
        }
      }

      let travellers = Array.from(travellerMap.values()).map((t) => ({
        ...t,
        booked_tours: t.booked_tours.join(', '),
      }));

      if (search && search.trim()) {
        const q = search.trim().toLowerCase();
        travellers = travellers.filter((t) => {
          return (
            (t.name && t.name.toLowerCase().includes(q)) ||
            (t.email && t.email.toLowerCase().includes(q)) ||
            (t.mobile && t.mobile.includes(q))
          );
        });
      }

      travellers.sort((a, b) => new Date(b.latest_booking_date) - new Date(a.latest_booking_date));

      const total = travellers.length;
      const offset = (pageNum - 1) * limitNum;
      const paged = travellers.slice(offset, offset + limitNum);

      return sendSuccess(res, 'Travellers retrieved successfully', {
        travellers: paged,
        total,
        page: pageNum,
        limit: limitNum,
      });
    } catch (err) {
      return sendError(res, 'Failed to fetch travellers: ' + err.message, null, 500);
    }
  },

  /**
   * Get single traveller details and their bookings with this vendor
   */
  getTravellerById: async (req, res) => {
    try {
      const { id } = req.params;
      const vId = Number(req.vendorId) || req.vendorId;

      const tDoc = await db.collection(COLLECTIONS.TRAVELLERS).doc(String(id)).get();
      let traveller = null;

      if (tDoc.exists) {
        traveller = tDoc.data();
      } else {
        const snap = await db.collection(COLLECTIONS.TRAVELLERS).where('id', '==', Number(id) || id).get();
        if (!snap.empty) traveller = snap.docs[0].data();
      }

      if (!traveller) {
        return sendError(res, 'Traveller not found or has no bookings with you', null, 404);
      }

      // Fetch bookings with this vendor
      const bSnap = await db.collection(COLLECTIONS.BOOKINGS)
        .where('traveller_id', '==', Number(traveller.id) || traveller.id)
        .where('vendor_id', '==', vId)
        .get();

      if (bSnap.empty) {
        return sendError(res, 'Traveller not found or has no bookings with you', null, 404);
      }

      const bookings = await Promise.all(
        bSnap.docs.map(async (doc) => {
          const b = doc.data();
          let listing_title = null;
          let listing_destination = null;
          let trip_id = null;
          let trip_status = null;

          if (b.listing_id) {
            const lDoc = await db.collection(COLLECTIONS.LISTINGS).doc(String(b.listing_id)).get();
            if (lDoc.exists) {
              listing_title = lDoc.data().title;
              listing_destination = lDoc.data().destination;
            }
          }

          const trSnap = await db.collection(COLLECTIONS.TRIPS).where('booking_id', '==', Number(b.id) || b.id).get();
          if (!trSnap.empty) {
            trip_id = trSnap.docs[0].data().id;
            trip_status = trSnap.docs[0].data().status;
          }

          return {
            ...b,
            listing_title,
            listing_destination,
            trip_id,
            trip_status,
          };
        })
      );

      bookings.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));

      return sendSuccess(res, 'Traveller details retrieved', {
        traveller,
        bookings,
      });
    } catch (err) {
      return sendError(res, 'Failed to fetch traveller details: ' + err.message, null, 500);
    }
  },
};

module.exports = travellerController;
