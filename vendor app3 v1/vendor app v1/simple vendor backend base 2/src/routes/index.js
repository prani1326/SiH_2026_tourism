const express = require('express');
const router = express.Router();

// Vendor route modules
const vendorAuthRoutes = require('./vendor/authRoutes');
const vendorProfileRoutes = require('./vendor/profileRoutes');
const vendorKycRoutes = require('./vendor/kycRoutes');
const vendorListingRoutes = require('./vendor/listingRoutes');
const vendorBookingRoutes = require('./vendor/bookingRoutes');
const vendorTravellerRoutes = require('./vendor/travellerRoutes');
const vendorTripRoutes = require('./vendor/tripRoutes');
const vendorWalletRoutes = require('./vendor/walletRoutes');
const vendorNotificationRoutes = require('./vendor/notificationRoutes');
const vendorDashboardRoutes = require('./vendor/dashboardRoutes');

// Admin route modules
const adminAuthRoutes = require('./admin/authRoutes');
const adminVendorRoutes = require('./admin/vendorRoutes');
const adminAuditRoutes = require('./admin/auditRoutes');
const adminDashboardRoutes = require('./admin/dashboardRoutes');

// Web Dashboard Central Routes
const dashboardRoutes = require('./dashboardRoutes');

// Mount Web Dashboard routes directly on /api
router.use('/', dashboardRoutes);

// Mount Vendor API routes
router.use('/vendor/auth', vendorAuthRoutes);
router.use('/vendor/profile', vendorProfileRoutes);
router.use('/vendor/kyc', vendorKycRoutes);
router.use('/vendor/listings', vendorListingRoutes);
router.use('/vendor/bookings', vendorBookingRoutes);
router.use('/vendor/travellers', vendorTravellerRoutes);
router.use('/vendor/trips', vendorTripRoutes);
router.use('/vendor/my-trips', vendorTripRoutes); // Alias for My Trips screen
router.use('/vendor/wallet', vendorWalletRoutes);
router.use('/vendor/notifications', vendorNotificationRoutes);
router.use('/vendor/dashboard', vendorDashboardRoutes);

// Mount Admin / Staff API routes
router.use('/admin/auth', adminAuthRoutes);
router.use('/admin/vendors', adminVendorRoutes);
router.use('/admin/audit-logs', adminAuditRoutes);
router.use('/admin/dashboard', adminDashboardRoutes);

// Mount Tourist / Traveler Mobile App API routes
const travellerAuthRoutes = require('./travellerAuthRoutes');
router.use('/auth', travellerAuthRoutes);
router.use('/v1/auth', travellerAuthRoutes);
router.use('/traveler/auth', travellerAuthRoutes);

module.exports = router;
