const { db } = require('../../config/firebase');
const { COLLECTIONS } = require('../../database/firestoreSchema');
const { sendSuccess, sendError } = require('../../utils/responseHelper');
const walletService = require('../../services/walletService');

const dashboardController = {
  /**
   * Get vendor home dashboard data
   */
  getDashboard: async (req, res) => {
    try {
      const vId = Number(req.vendorId) || req.vendorId;
      const vendorCol = db.collection(COLLECTIONS.VENDORS);

      let vendor = null;
      const vDoc = await vendorCol.doc(String(vId)).get();
      if (vDoc.exists) {
        vendor = vDoc.data();
      } else {
        const snap = await vendorCol.where('id', '==', vId).get();
        if (!snap.empty) vendor = snap.docs[0].data();
      }

      if (!vendor) {
        return sendError(res, 'Vendor not found', null, 404);
      }

      // Booking stats
      const bookingsSnap = await db.collection(COLLECTIONS.BOOKINGS).where('vendor_id', '==', vId).get();
      let totalBookings = 0;
      let pendingRequests = 0;
      let acceptedBookings = 0;

      bookingsSnap.forEach((bDoc) => {
        const b = bDoc.data();
        totalBookings++;
        if (b.status === 'pending') pendingRequests++;
        if (b.status === 'accepted') acceptedBookings++;
      });

      // Trip stats
      const tripsSnap = await db.collection(COLLECTIONS.TRIPS).where('vendor_id', '==', vId).get();
      let upcomingTrips = 0;
      let todayTrips = 0;
      let activeTrip = null;

      const todayStr = new Date().toISOString().slice(0, 10);

      for (const trDoc of tripsSnap.docs) {
        const tr = trDoc.data();
        if (tr.status === 'upcoming') upcomingTrips++;
        if (tr.created_at && tr.created_at.slice(0, 10) === todayStr) todayTrips++;

        if (tr.status === 'active' && !activeTrip) {
          let listing_title = null;
          let listing_destination = null;
          let traveller_name = null;
          let traveller_mobile = null;
          let amount = 0;
          let traveller_count = 1;

          if (tr.booking_id) {
            const bDoc = await db.collection(COLLECTIONS.BOOKINGS).doc(String(tr.booking_id)).get();
            if (bDoc.exists) {
              const b = bDoc.data();
              amount = b.amount;
              traveller_count = b.traveller_count;

              if (b.listing_id) {
                const lDoc = await db.collection(COLLECTIONS.LISTINGS).doc(String(b.listing_id)).get();
                if (lDoc.exists) {
                  listing_title = lDoc.data().title;
                  listing_destination = lDoc.data().destination;
                }
              }

              if (b.traveller_id) {
                const tDoc = await db.collection(COLLECTIONS.TRAVELLERS).doc(String(b.traveller_id)).get();
                if (tDoc.exists) {
                  traveller_name = tDoc.data().name;
                  traveller_mobile = tDoc.data().mobile;
                }
              }
            }
          }

          activeTrip = {
            ...tr,
            listing_title,
            listing_destination,
            traveller_name,
            traveller_mobile,
            amount,
            traveller_count,
          };
        }
      }

      // Wallet stats
      const walletSummary = await walletService.getSummary(vId);

      const { password_hash, ...vendorSafe } = vendor;

      return sendSuccess(res, 'Vendor dashboard retrieved', {
        vendor: vendorSafe,
        kyc_status: vendor.kyc_status,
        today_trips: todayTrips,
        total_bookings: totalBookings,
        pending_requests: pendingRequests,
        accepted_bookings: acceptedBookings,
        upcoming_trips: upcomingTrips,
        active_trip: activeTrip || null,
        wallet_balance: walletSummary.balance,
        total_earnings: walletSummary.total_earned,
      });
    } catch (err) {
      return sendError(res, 'Failed to fetch dashboard: ' + err.message, null, 500);
    }
  },
};

module.exports = dashboardController;
