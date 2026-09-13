const { db } = require('../../config/firebase');
const { COLLECTIONS } = require('../../database/firestoreSchema');
const { sendSuccess, sendError } = require('../../utils/responseHelper');
const { isValidCoordinate } = require('../../utils/validators');
const auditService = require('../../services/auditService');
const notificationService = require('../../services/notificationService');
const walletService = require('../../services/walletService');

const tripController = {
  /**
   * Get vendor trips
   */
  getTrips: async (req, res) => {
    try {
      const vId = Number(req.vendorId) || req.vendorId;
      const { status, page = 1, limit = 20 } = req.query;
      const pageNum = parseInt(page, 10) || 1;
      const limitNum = parseInt(limit, 10) || 20;

      const tripsCol = db.collection(COLLECTIONS.TRIPS);
      const snap = await tripsCol.where('vendor_id', '==', vId).get();

      let allTrips = snap.docs.map((d) => d.data());

      let upcomingCount = 0;
      let activeCount = 0;
      let completedCount = 0;

      allTrips.forEach((t) => {
        if (t.status === 'upcoming') upcomingCount++;
        if (t.status === 'active') activeCount++;
        if (t.status === 'completed') completedCount++;
      });

      if (status) {
        allTrips = allTrips.filter((t) => t.status === status);
      }

      allTrips.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));

      const total = allTrips.length;
      const offset = (pageNum - 1) * limitNum;
      const paged = allTrips.slice(offset, offset + limitNum);

      const enriched = await Promise.all(
        paged.map(async (tr) => {
          let booking_date = null;
          let traveller_count = 1;
          let amount = 0;
          let booking_status = null;
          let listing_title = null;
          let listing_destination = null;
          let listing_duration = null;
          let traveller_name = null;
          let traveller_mobile = null;
          let traveller_email = null;
          let traveller_photo = null;

          if (tr.booking_id) {
            const bDoc = await db.collection(COLLECTIONS.BOOKINGS).doc(String(tr.booking_id)).get();
            if (bDoc.exists) {
              const b = bDoc.data();
              booking_date = b.booking_date;
              traveller_count = b.traveller_count;
              amount = b.amount;
              booking_status = b.status;

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
                  traveller_mobile = t.mobile;
                  traveller_email = t.email;
                  traveller_photo = t.profile_photo;
                }
              }
            }
          }

          const locSnap = await db.collection(COLLECTIONS.TRIP_LOCATIONS).where('trip_id', '==', Number(tr.id) || tr.id).get();

          return {
            ...tr,
            booking_date,
            traveller_count,
            amount,
            booking_status,
            listing_title,
            listing_destination,
            listing_duration,
            traveller_name,
            traveller_mobile,
            traveller_email,
            traveller_photo,
            location_pings_count: locSnap.size,
          };
        })
      );

      return sendSuccess(res, 'Trips retrieved successfully', {
        trips: enriched,
        counts: {
          total: snap.size,
          upcoming: upcomingCount,
          active: activeCount,
          completed: completedCount,
        },
        page: pageNum,
        limit: limitNum,
      });
    } catch (err) {
      return sendError(res, 'Failed to fetch trips: ' + err.message, null, 500);
    }
  },

  /**
   * My trips alias
   */
  getMyTrips: async (req, res) => {
    return tripController.getTrips(req, res);
  },

  /**
   * Get single trip details with location history & booking info
   */
  getTripById: async (req, res) => {
    try {
      const { id } = req.params;
      const vId = Number(req.vendorId) || req.vendorId;

      const tripsCol = db.collection(COLLECTIONS.TRIPS);
      let doc = await tripsCol.doc(String(id)).get();
      let trip = null;

      if (doc.exists) {
        trip = doc.data();
      } else {
        const snap = await tripsCol.where('id', '==', Number(id) || id).get();
        if (!snap.empty) trip = snap.docs[0].data();
      }

      if (!trip || (Number(trip.vendor_id) !== vId && String(trip.vendor_id) !== String(vId))) {
        return sendError(res, 'Trip not found or unauthorized', null, 404);
      }

      let booking_date = null;
      let traveller_count = 1;
      let amount = 0;
      let booking_status = null;
      let listing_id = null;
      let listing_title = null;
      let listing_description = null;
      let listing_destination = null;
      let listing_duration = null;
      let listing_price = null;
      let traveller_id = null;
      let traveller_name = null;
      let traveller_mobile = null;
      let traveller_email = null;
      let traveller_photo = null;

      if (trip.booking_id) {
        const bDoc = await db.collection(COLLECTIONS.BOOKINGS).doc(String(trip.booking_id)).get();
        if (bDoc.exists) {
          const b = bDoc.data();
          booking_date = b.booking_date;
          traveller_count = b.traveller_count;
          amount = b.amount;
          booking_status = b.status;
          listing_id = b.listing_id;
          traveller_id = b.traveller_id;

          if (b.listing_id) {
            const lDoc = await db.collection(COLLECTIONS.LISTINGS).doc(String(b.listing_id)).get();
            if (lDoc.exists) {
              const l = lDoc.data();
              listing_title = l.title;
              listing_description = l.description;
              listing_destination = l.destination;
              listing_duration = l.duration;
              listing_price = l.price;
            }
          }

          if (b.traveller_id) {
            const tDoc = await db.collection(COLLECTIONS.TRAVELLERS).doc(String(b.traveller_id)).get();
            if (tDoc.exists) {
              const t = tDoc.data();
              traveller_name = t.name;
              traveller_mobile = t.mobile;
              traveller_email = t.email;
              traveller_photo = t.profile_photo;
            }
          }
        }
      }

      const locSnap = await db.collection(COLLECTIONS.TRIP_LOCATIONS).where('trip_id', '==', Number(trip.id) || trip.id).get();
      const recentLocations = locSnap.docs.map((d) => d.data());
      recentLocations.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

      return sendSuccess(res, 'Trip details retrieved', {
        ...trip,
        booking_date,
        traveller_count,
        amount,
        booking_status,
        listing_id,
        listing_title,
        listing_description,
        listing_destination,
        listing_duration,
        listing_price,
        traveller_id,
        traveller_name,
        traveller_mobile,
        traveller_email,
        traveller_photo,
        is_tracking_active: trip.status === 'active',
        recent_locations: recentLocations.slice(0, 50),
      });
    } catch (err) {
      return sendError(res, 'Failed to fetch trip: ' + err.message, null, 500);
    }
  },

  /**
   * Start Trip (upcoming -> active)
   */
  startTrip: async (req, res) => {
    try {
      const { id } = req.params;
      const vId = Number(req.vendorId) || req.vendorId;

      const tripsCol = db.collection(COLLECTIONS.TRIPS);
      let docRef = tripsCol.doc(String(id));
      let doc = await docRef.get();

      if (!doc.exists) {
        const snap = await tripsCol.where('id', '==', Number(id) || id).get();
        if (!snap.empty) {
          docRef = snap.docs[0].ref;
          doc = snap.docs[0];
        }
      }

      if (!doc.exists) {
        return sendError(res, 'Trip not found or unauthorized', null, 404);
      }

      const trip = doc.data();
      if (Number(trip.vendor_id) !== vId && String(trip.vendor_id) !== String(vId)) {
        return sendError(res, 'Trip not found or unauthorized', null, 404);
      }

      if (trip.status === 'active') {
        return sendError(res, 'Trip is already active', null, 400);
      }

      if (trip.status === 'completed') {
        return sendError(res, 'Trip is already completed', null, 400);
      }

      // Check if vendor already has another active trip running
      const activeSnap = await tripsCol.where('vendor_id', '==', vId).where('status', '==', 'active').get();
      let otherActive = false;
      activeSnap.forEach((d) => {
        if (String(d.data().id) !== String(trip.id)) otherActive = true;
      });

      if (otherActive) {
        return sendError(res, 'You already have an ongoing active trip. Please end it before starting a new one.', null, 400);
      }

      await docRef.set({
        status: 'active',
        start_time: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      }, { merge: true });

      const updatedTrip = (await docRef.get()).data();

      await auditService.logAction({
        actorType: 'vendor',
        actorId: vId,
        action: 'Trip Started',
        entityType: 'trip',
        entityId: Number(trip.id) || trip.id,
        ipAddress: req.ip,
      });

      await notificationService.createNotification({
        recipientType: 'vendor',
        recipientId: vId,
        title: 'Trip Started',
        message: `Trip #${trip.id} has started. Location tracking is now active.`,
        type: 'trip_started',
      });

      return sendSuccess(res, 'Trip started successfully. Location tracking is now ON.', {
        trip: updatedTrip,
        location_tracking: 'ON',
      });
    } catch (err) {
      return sendError(res, 'Failed to start trip: ' + err.message, null, 500);
    }
  },

  /**
   * End Trip (active -> completed)
   */
  endTrip: async (req, res) => {
    try {
      const { id } = req.params;
      const vId = Number(req.vendorId) || req.vendorId;

      const tripsCol = db.collection(COLLECTIONS.TRIPS);
      let docRef = tripsCol.doc(String(id));
      let doc = await docRef.get();

      if (!doc.exists) {
        const snap = await tripsCol.where('id', '==', Number(id) || id).get();
        if (!snap.empty) {
          docRef = snap.docs[0].ref;
          doc = snap.docs[0];
        }
      }

      if (!doc.exists) {
        return sendError(res, 'Trip not found or unauthorized', null, 404);
      }

      const trip = doc.data();
      if (Number(trip.vendor_id) !== vId && String(trip.vendor_id) !== String(vId)) {
        return sendError(res, 'Trip not found or unauthorized', null, 404);
      }

      if (trip.status !== 'active') {
        return sendError(res, `Cannot end trip. Current trip status is '${trip.status}' (must be 'active')`, null, 400);
      }

      // Mark trip completed
      await docRef.set({
        status: 'completed',
        end_time: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      }, { merge: true });

      // Mark booking completed and fetch amount
      let tripAmount = 0;
      let listingTitle = 'Tour';
      if (trip.booking_id) {
        const bRef = db.collection(COLLECTIONS.BOOKINGS).doc(String(trip.booking_id));
        const bDoc = await bRef.get();
        if (bDoc.exists) {
          tripAmount = parseFloat(bDoc.data().amount) || 0;
          await bRef.set({ status: 'completed', updated_at: new Date().toISOString() }, { merge: true });

          if (bDoc.data().listing_id) {
            const lDoc = await db.collection(COLLECTIONS.LISTINGS).doc(String(bDoc.data().listing_id)).get();
            if (lDoc.exists) listingTitle = lDoc.data().title;
          }
        }
      }

      // Credit wallet with earnings
      const earningDesc = `Earnings for completing Trip #${trip.id} (${listingTitle})`;
      const transaction = await walletService.addTransaction(
        vId,
        'earning',
        tripAmount,
        earningDesc,
        trip.booking_id
      );

      await auditService.logAction({
        actorType: 'vendor',
        actorId: vId,
        action: 'Trip Ended',
        entityType: 'trip',
        entityId: Number(trip.id) || trip.id,
        details: { amount: tripAmount, transaction_id: transaction.id },
        ipAddress: req.ip,
      });

      await notificationService.createNotification({
        recipientType: 'vendor',
        recipientId: vId,
        title: 'Trip Completed & Earnings Credited',
        message: `Trip #${trip.id} completed. ₹${tripAmount} has been added to your wallet!`,
        type: 'trip_completed',
      });

      const updatedTrip = (await docRef.get()).data();
      const walletSummary = await walletService.getSummary(vId);

      return sendSuccess(res, 'Trip completed successfully. Location tracking is now OFF.', {
        trip: updatedTrip,
        location_tracking: 'OFF',
        earnings_credited: tripAmount,
        wallet: walletSummary,
      });
    } catch (err) {
      return sendError(res, 'Failed to end trip: ' + err.message, null, 500);
    }
  },

  /**
   * Submit live GPS location during active trip
   */
  recordLocation: async (req, res) => {
    try {
      const { id } = req.params;
      const vId = Number(req.vendorId) || req.vendorId;
      const { latitude, longitude, timestamp } = req.body;

      if (!isValidCoordinate(latitude, longitude)) {
        return sendError(res, 'Valid latitude and longitude coordinates are required', null, 422);
      }

      const tripsCol = db.collection(COLLECTIONS.TRIPS);
      let trip = null;
      const doc = await tripsCol.doc(String(id)).get();
      if (doc.exists) {
        trip = doc.data();
      } else {
        const snap = await tripsCol.where('id', '==', Number(id) || id).get();
        if (!snap.empty) trip = snap.docs[0].data();
      }

      if (!trip || (Number(trip.vendor_id) !== vId && String(trip.vendor_id) !== String(vId))) {
        return sendError(res, 'Trip not found or unauthorized', null, 404);
      }

      if (trip.status !== 'active') {
        return sendError(res, `Location tracking is only allowed for active trips. Trip is currently '${trip.status}'.`, null, 400);
      }

      const locCol = db.collection(COLLECTIONS.TRIP_LOCATIONS);
      const allLocSnap = await locCol.get();
      const nextId = allLocSnap.size + 1;

      const locPing = {
        id: nextId,
        trip_id: Number(trip.id) || trip.id,
        latitude: parseFloat(latitude),
        longitude: parseFloat(longitude),
        timestamp: timestamp || new Date().toISOString(),
      };

      await locCol.doc(String(nextId)).set(locPing);

      return sendSuccess(res, 'Location ping recorded', locPing, 201);
    } catch (err) {
      return sendError(res, 'Failed to record location: ' + err.message, null, 500);
    }
  },

  /**
   * Get location trail for a trip
   */
  getTripLocations: async (req, res) => {
    try {
      const { id } = req.params;
      const vId = Number(req.vendorId) || req.vendorId;

      const tripsCol = db.collection(COLLECTIONS.TRIPS);
      let trip = null;
      const doc = await tripsCol.doc(String(id)).get();
      if (doc.exists) {
        trip = doc.data();
      } else {
        const snap = await tripsCol.where('id', '==', Number(id) || id).get();
        if (!snap.empty) trip = snap.docs[0].data();
      }

      if (!trip || (Number(trip.vendor_id) !== vId && String(trip.vendor_id) !== String(vId))) {
        return sendError(res, 'Trip not found or unauthorized', null, 404);
      }

      const locSnap = await db.collection(COLLECTIONS.TRIP_LOCATIONS).where('trip_id', '==', Number(trip.id) || trip.id).get();
      const locations = locSnap.docs.map((d) => d.data());
      locations.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));

      return sendSuccess(res, 'Trip location history retrieved', {
        trip_id: parseInt(id, 10) || id,
        status: trip.status,
        total_pings: locations.length,
        locations,
      });
    } catch (err) {
      return sendError(res, 'Failed to fetch trip locations: ' + err.message, null, 500);
    }
  },
};

module.exports = tripController;
