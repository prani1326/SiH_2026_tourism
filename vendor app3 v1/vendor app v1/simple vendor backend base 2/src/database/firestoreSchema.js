/**
 * Firestore Collection Definitions & Schema Constants
 * App 3 — Vendor App 100% Firebase Architecture
 */

const COLLECTIONS = {
  STAFF_USERS: 'staff_users',
  VENDORS: 'vendors',
  VENDOR_KYC: 'vendor_kyc',
  VENDOR_DOCUMENTS: 'vendor_documents',
  LISTINGS: 'listings',
  TRAVELLERS: 'travellers',
  BOOKINGS: 'bookings',
  TRIPS: 'trips',
  TRIP_LOCATIONS: 'trip_locations',
  WALLETS: 'wallets',
  TRANSACTIONS: 'transactions',
  NOTIFICATIONS: 'notifications',
  AUDIT_LOGS: 'audit_logs',
  OTP_VERIFICATIONS: 'otp_verifications',
  LEADERS: 'leaders',
  DESTINATIONS: 'destinations',
  TICKETS: 'tickets',
  SAFETY_EVENTS: 'safety_events',
  APP_ACTIVITIES: 'app_activities',
};

/**
 * Validate document structure for core entities
 */
function validateVendor(data) {
  if (!data.name || !data.email || !data.mobile) {
    throw new Error('Vendor requires name, email, and mobile');
  }
  return true;
}

function validateListing(data) {
  if (!data.title || !data.destination || data.price === undefined || !data.duration) {
    throw new Error('Listing requires title, destination, price, and duration');
  }
  return true;
}

function validateBooking(data) {
  if (!data.listing_id || !data.vendor_id || !data.traveller_id || !data.amount) {
    throw new Error('Booking requires listing_id, vendor_id, traveller_id, and amount');
  }
  return true;
}

module.exports = {
  COLLECTIONS,
  validateVendor,
  validateListing,
  validateBooking,
};
