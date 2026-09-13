const express = require('express');
const router = express.Router();
const dashboardController = require('../controllers/dashboardController');

// Web Dashboard Main Endpoints
router.get('/overview', dashboardController.getOverview);
router.get('/travelers', dashboardController.getTravelers);
router.get('/vendors', dashboardController.getVendors);
router.get('/vendors/:id', dashboardController.getVendorDetails);
router.post('/vendors/:id/verify-kyc', dashboardController.verifyVendorKyc);
router.get('/leaders', dashboardController.getLeaders);
router.get('/destinations', dashboardController.getDestinations);
router.get('/bookings', dashboardController.getBookings);
router.get('/trips', dashboardController.getTrips);
router.get('/tickets', dashboardController.getTickets);
router.get('/safety', dashboardController.getSafety);
router.get('/analytics', dashboardController.getAnalytics);

// Ingestion Simulation Endpoints
router.post('/ingest/traveler', dashboardController.ingestTraveler);
router.post('/ingest/vendor', dashboardController.ingestVendor);
router.post('/ingest/leader', dashboardController.ingestLeader);

module.exports = router;
