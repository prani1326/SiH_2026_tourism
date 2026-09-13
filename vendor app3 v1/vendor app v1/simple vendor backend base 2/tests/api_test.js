const http = require('http');
const path = require('path');
const fs = require('fs');

process.env.NODE_ENV = 'test';
process.env.PORT = '5001';

const app = require('../src/app');
const { seedFirestore } = require('../src/database/firestoreSeeder');

let server;
let baseUrl;

function makeRequest(method, endpoint, body = null, token = null) {
  return new Promise((resolve, reject) => {
    const url = new URL(endpoint, baseUrl);
    const postData = body ? JSON.stringify(body) : null;

    const headers = {
      'Accept': 'application/json',
    };

    if (postData) {
      headers['Content-Type'] = 'application/json';
      headers['Content-Length'] = Buffer.byteLength(postData);
    }

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const req = http.request(
      url,
      {
        method,
        headers,
      },
      (res) => {
        let rawData = '';
        res.on('data', (chunk) => {
          rawData += chunk;
        });
        res.on('end', () => {
          try {
            const parsed = JSON.parse(rawData);
            resolve({ statusCode: res.statusCode, data: parsed });
          } catch (e) {
            resolve({ statusCode: res.statusCode, raw: rawData });
          }
        });
      }
    );

    req.on('error', (err) => reject(err));

    if (postData) {
      req.write(postData);
    }
    req.end();
  });
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(`Assertion Failed: ${message}`);
  }
}

async function runTests() {
  console.log('\n=======================================================');
  console.log('  STARTING TRAVEL VENDOR BACKEND API TEST SUITE        ');
  console.log('=======================================================\n');

  // Seed Firestore
  await seedFirestore();

  server = http.createServer(app);
  await new Promise((res) => server.listen(5001, res));
  baseUrl = 'http://localhost:5001';

  let adminToken = '';
  let vendorApprovedToken = '';
  let vendorPendingToken = '';
  let testListingId = null;
  let testBookingId = null;
  let testTripId = null;

  try {
    // 1. Health Check
    console.log('1. Testing Health Check...');
    const health = await makeRequest('GET', '/api/health');
    assert(health.statusCode === 200, 'Health check should return 200');
    assert(health.data.success === true, 'Health check response should be success');
    console.log('   ✅ Health check passed');

    // 2. Admin Auth
    console.log('2. Testing Admin Auth (Login & Me)...');
    const adminLogin = await makeRequest('POST', '/api/admin/auth/login', {
      email: 'admin@travelcompany.com',
      password: 'admin123',
    });
    assert(adminLogin.statusCode === 200, 'Admin login should succeed with 200');
    assert(adminLogin.data.data.token, 'Admin login should return JWT token');
    adminToken = adminLogin.data.data.token;

    const adminMe = await makeRequest('GET', '/api/admin/auth/me', null, adminToken);
    assert(adminMe.statusCode === 200, 'Admin /me should succeed');
    assert(adminMe.data.data.role === 'admin', 'Admin role should be admin');
    console.log('   ✅ Admin auth passed');

    // 3. Vendor Auth (Password Login)
    console.log('3. Testing Approved Vendor Login...');
    const vendorLogin = await makeRequest('POST', '/api/vendor/auth/login', {
      email: 'rahul@himalayanguides.com',
      password: 'vendor123',
    });
    assert(vendorLogin.statusCode === 200, 'Vendor login should return 200');
    assert(vendorLogin.data.data.token, 'Vendor login should return token');
    assert(vendorLogin.data.data.vendor.kyc_status === 'approved', 'Vendor KYC should be approved');
    vendorApprovedToken = vendorLogin.data.data.token;
    console.log('   ✅ Vendor login passed');

    // 4. Pending Vendor Auth
    console.log('4. Testing Pending Vendor Login...');
    const pendingLogin = await makeRequest('POST', '/api/vendor/auth/login', {
      mobile: '9812345678',
      password: 'vendor123',
    });
    assert(pendingLogin.statusCode === 200, 'Pending vendor login should return 200');
    vendorPendingToken = pendingLogin.data.data.token;
    assert(pendingLogin.data.data.vendor.kyc_status === 'pending', 'Status should be pending');
    console.log('   ✅ Pending vendor login passed');

    // 5. Vendor OTP Flow
    console.log('5. Testing Vendor OTP Request & Verification...');
    const otpReq = await makeRequest('POST', '/api/vendor/auth/request-otp', {
      mobile: '9870001122',
    });
    assert(otpReq.statusCode === 200, 'OTP request should succeed');
    assert(otpReq.data.data.otp_demo === '123456', 'Demo OTP generated');

    const otpVerify = await makeRequest('POST', '/api/vendor/auth/verify-otp', {
      mobile: '9870001122',
      otp_code: '123456',
      name: 'New OTP Vendor',
    });
    assert(otpVerify.statusCode === 200, 'OTP verification should return 200');
    assert(otpVerify.data.data.token, 'OTP verification should return JWT token');
    console.log('   ✅ Vendor OTP request & verification passed');

    // 6. Vendor Profile
    console.log('6. Testing Vendor Profile API...');
    const profileRes = await makeRequest('GET', '/api/vendor/profile', null, vendorApprovedToken);
    assert(profileRes.statusCode === 200, 'Profile fetch should succeed');
    assert(profileRes.data.data.email === 'rahul@himalayanguides.com', 'Profile email matches');

    const updateProfile = await makeRequest('PUT', '/api/vendor/profile', { name: 'Rahul Sharma Guides' }, vendorApprovedToken);
    assert(updateProfile.statusCode === 200, 'Profile update should succeed');
    assert(updateProfile.data.data.name === 'Rahul Sharma Guides', 'Updated name matches');
    console.log('   ✅ Vendor profile passed');

    // 7. KYC Access Control (Gate Check)
    console.log('7. Testing KYC Access Control (Operational features blocked when KYC pending)...');
    const blockedListing = await makeRequest('POST', '/api/vendor/listings', {
      title: 'Blocked Tour',
      destination: 'Goa',
      price: 1500,
      duration: '1 Day',
    }, vendorPendingToken);
    assert(blockedListing.statusCode === 403, 'Pending vendor must be blocked from creating listings (403)');
    console.log('   ✅ KYC access control correctly blocked unverified vendor');

    // 8. Admin KYC Verification Workflow
    console.log('8. Testing Admin Vendor Review & Approval Workflow...');
    const adminVendors = await makeRequest('GET', '/api/admin/vendors?kyc_status=pending', null, adminToken);
    assert(adminVendors.statusCode === 200, 'Admin vendor list should succeed');
    assert(adminVendors.data.data.vendors.length > 0, 'Pending vendors found in queue');

    const pendingVendorId = adminVendors.data.data.vendors[0].id;
    const approveKyc = await makeRequest('POST', `/api/admin/vendors/${pendingVendorId}/verify-kyc`, {
      action: 'approve',
      remarks: 'Verified during automated test run',
    }, adminToken);
    assert(approveKyc.statusCode === 200, 'Admin approve KYC should return 200');
    assert(approveKyc.data.data.kyc_status === 'approved', 'Vendor status updated to approved');
    console.log('   ✅ Admin KYC approval workflow passed');

    // 9. Listings Management
    console.log('9. Testing Vendor Listings CRUD...');
    const createListing = await makeRequest('POST', '/api/vendor/listings', {
      title: 'Beas River Rafting Adventure',
      description: 'Grade 3 white water rafting on Beas river with expert safety kayakers.',
      destination: 'Kullu, Himachal Pradesh',
      price: 1800,
      duration: '2 Hours',
      max_travellers: 6,
    }, vendorApprovedToken);
    assert(createListing.statusCode === 201, 'Create listing should return 201');
    testListingId = createListing.data.data.id;

    const getListings = await makeRequest('GET', '/api/vendor/listings', null, vendorApprovedToken);
    assert(getListings.statusCode === 200, 'Listings list should return 200');
    assert(getListings.data.data.listings.length >= 1, 'Should contain created listings');
    console.log('   ✅ Vendor listings management passed');

    // 10. Bookings & Requests Flow
    console.log('10. Testing Booking Requests & Acceptance...');
    const bookingsRes = await makeRequest('GET', '/api/vendor/bookings?status=pending', null, vendorApprovedToken);
    assert(bookingsRes.statusCode === 200, 'Get bookings should succeed');
    assert(bookingsRes.data.data.bookings.length > 0, 'Should find pending booking');

    testBookingId = bookingsRes.data.data.bookings[0].id;

    const acceptBooking = await makeRequest('POST', `/api/vendor/bookings/${testBookingId}/accept`, {}, vendorApprovedToken);
    assert(acceptBooking.statusCode === 200, 'Accept booking should return 200');
    assert(acceptBooking.data.data.status === 'accepted', 'Booking status becomes accepted');
    assert(acceptBooking.data.data.trip, 'Upcoming trip generated automatically on acceptance');
    testTripId = acceptBooking.data.data.trip.id;
    console.log('   ✅ Booking acceptance & upcoming trip creation passed');

    // 11. Travellers API
    console.log('11. Testing Travellers Associated with Vendor...');
    const travellersRes = await makeRequest('GET', '/api/vendor/travellers', null, vendorApprovedToken);
    assert(travellersRes.statusCode === 200, 'Get travellers should succeed');
    assert(travellersRes.data.data.travellers.length > 0, 'Should find linked travellers');
    const travId = travellersRes.data.data.travellers[0].id;

    const travellerDetail = await makeRequest('GET', `/api/vendor/travellers/${travId}`, null, vendorApprovedToken);
    assert(travellerDetail.statusCode === 200, 'Get traveller by ID should succeed');
    assert(travellerDetail.data.data.traveller, 'Traveller profile object returned');
    console.log('   ✅ Travellers API passed');

    // 12. Trip Lifecycle & Location Tracking
    console.log('12. Testing Trip Start -> Location Tracking -> Trip End...');
    // A. Start Trip
    const startTrip = await makeRequest('POST', `/api/vendor/trips/${testTripId}/start`, {}, vendorApprovedToken);
    assert(startTrip.statusCode === 200, 'Start trip should return 200');
    assert(startTrip.data.data.location_tracking === 'ON', 'Location tracking is turned ON');
    assert(startTrip.data.data.trip.status === 'active', 'Trip status is active');

    // B. Record Live GPS Location
    const pingLocation = await makeRequest('POST', `/api/vendor/trips/${testTripId}/location`, {
      latitude: 32.2450,
      longitude: 77.1900,
    }, vendorApprovedToken);
    assert(pingLocation.statusCode === 201, 'Location recording should return 201');

    // C. Get Location Trail
    const locTrail = await makeRequest('GET', `/api/vendor/trips/${testTripId}/location`, null, vendorApprovedToken);
    assert(locTrail.statusCode === 200, 'Get trip locations should succeed');
    assert(locTrail.data.data.locations.length >= 1, 'Should contain location pings');

    // D. End Trip
    const endTrip = await makeRequest('POST', `/api/vendor/trips/${testTripId}/end`, {}, vendorApprovedToken);
    assert(endTrip.statusCode === 200, 'End trip should return 200');
    assert(endTrip.data.data.location_tracking === 'OFF', 'Location tracking is turned OFF');
    assert(endTrip.data.data.trip.status === 'completed', 'Trip is completed');
    assert(endTrip.data.data.earnings_credited > 0, 'Earnings credited on completion');
    console.log('   ✅ Trip lifecycle & Location tracking passed');

    // 13. Wallet & Transactions
    console.log('13. Testing Wallet Overview & Transactions...');
    const walletRes = await makeRequest('GET', '/api/vendor/wallet', null, vendorApprovedToken);
    assert(walletRes.statusCode === 200, 'Get wallet should succeed');
    assert(walletRes.data.data.balance > 0, 'Wallet balance should be greater than 0');
    assert(walletRes.data.data.total_earned > 0, 'Total earned calculated');

    const txRes = await makeRequest('GET', '/api/vendor/wallet/transactions', null, vendorApprovedToken);
    assert(txRes.statusCode === 200, 'Get transactions should succeed');
    assert(txRes.data.data.transactions.length >= 1, 'Contains earning transaction');
    console.log('   ✅ Wallet & transactions passed');

    // 14. Vendor Home Dashboard
    console.log('14. Testing Vendor Dashboard API...');
    const dashRes = await makeRequest('GET', '/api/vendor/dashboard', null, vendorApprovedToken);
    assert(dashRes.statusCode === 200, 'Dashboard should return 200');
    assert(dashRes.data.data.kyc_status === 'approved', 'Dashboard reflects approved KYC');
    assert(dashRes.data.data.total_earnings > 0, 'Dashboard total earnings populated');
    console.log('   ✅ Vendor dashboard API passed');

    // 15. Vendor Notifications
    console.log('15. Testing Vendor Notifications...');
    const notifsRes = await makeRequest('GET', '/api/vendor/notifications', null, vendorApprovedToken);
    assert(notifsRes.statusCode === 200, 'Get notifications should succeed');
    assert(notifsRes.data.data.notifications.length > 0, 'Should have received notifications');

    const markAll = await makeRequest('POST', '/api/vendor/notifications/read-all', {}, vendorApprovedToken);
    assert(markAll.statusCode === 200, 'Mark all notifications as read should succeed');
    console.log('   ✅ Vendor notifications passed');

    // 16. Admin Audit Logs & Dashboard
    console.log('16. Testing Admin Audit Logs & Metrics Dashboard...');
    const auditLogs = await makeRequest('GET', '/api/admin/audit-logs', null, adminToken);
    assert(auditLogs.statusCode === 200, 'Audit logs should succeed');
    assert(auditLogs.data.data.logs.length > 0, 'Audit logs recorded activities');

    const adminDash = await makeRequest('GET', '/api/admin/dashboard', null, adminToken);
    assert(adminDash.statusCode === 200, 'Admin dashboard should succeed');
    assert(adminDash.data.data.vendors.total_vendors >= 3, 'Vendors counted');
    console.log('   ✅ Admin audit logs & dashboard passed');

    console.log('\n=======================================================');
    console.log('  🎉 ALL 16 INTEGRATION TEST SUITES PASSED (100%)       ');
    console.log('=======================================================\n');

  } catch (err) {
    console.error('\n❌ TEST FAILED:', err.message);
    process.exitCode = 1;
  } finally {
    server.close();
  }
}

runTests();
