const { db } = require('../../config/firebase');
const { COLLECTIONS } = require('../../database/firestoreSchema');
const { sendSuccess, sendError } = require('../../utils/responseHelper');

const adminDashboardController = {
  /**
   * Get Admin dashboard metrics
   */
  getDashboard: async (req, res) => {
    try {
      const vendorsSnap = await db.collection(COLLECTIONS.VENDORS).get();
      let totalVendors = 0;
      let pendingKyc = 0;
      let approvedKyc = 0;
      let rejectedKyc = 0;
      const pendingVendorsList = [];

      for (const doc of vendorsSnap.docs) {
        const v = doc.data();
        totalVendors++;
        if (v.kyc_status === 'pending') {
          pendingKyc++;
          pendingVendorsList.push(v);
        } else if (v.kyc_status === 'approved') {
          approvedKyc++;
        } else if (v.kyc_status === 'rejected') {
          rejectedKyc++;
        }
      }

      pendingVendorsList.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
      const recentPending = pendingVendorsList.slice(0, 5);

      const listingsSnap = await db.collection(COLLECTIONS.LISTINGS).get();
      const bookingsSnap = await db.collection(COLLECTIONS.BOOKINGS).get();
      const tripsSnap = await db.collection(COLLECTIONS.TRIPS).get();
      const travellersSnap = await db.collection(COLLECTIONS.TRAVELLERS).get();
      const txSnap = await db.collection(COLLECTIONS.TRANSACTIONS).where('type', '==', 'earning').get();

      let completedTrips = 0;
      let activeTrips = 0;
      tripsSnap.forEach((d) => {
        const tr = d.data();
        if (tr.status === 'completed') completedTrips++;
        if (tr.status === 'active') activeTrips++;
      });

      let platformRevenue = 0;
      txSnap.forEach((d) => {
        platformRevenue += parseFloat(d.data().amount) || 0;
      });

      return sendSuccess(res, 'Admin dashboard metrics retrieved', {
        vendors: {
          total: totalVendors,
          total_vendors: totalVendors,
          pending_kyc: pendingKyc,
          approved_kyc: approvedKyc,
          rejected_kyc: rejectedKyc,
        },
        platform: {
          total_listings: listingsSnap.size,
          total_bookings: bookingsSnap.size,
          completed_trips: completedTrips,
          active_trips: activeTrips,
          total_travellers: travellersSnap.size,
          platform_revenue: platformRevenue,
        },
        recent_pending_vendors: recentPending,
      });
    } catch (err) {
      return sendError(res, 'Failed to fetch admin dashboard: ' + err.message, null, 500);
    }
  },
};

module.exports = adminDashboardController;
