const express = require('express');
const cors = require('cors');
const path = require('path');
const { getDb, queryAll, queryOne, execute } = require('./db/index');
const { seed } = require('./db/seed');

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(express.json());

// Initialize Database
(async () => {
  try {
    await getDb();
    // Seed initial data if DB is empty
    const checkTraveler = queryOne('SELECT COUNT(*) as count FROM travelers');
    if (!checkTraveler || checkTraveler.count === 0) {
      await seed();
    }
    console.log('Database initialized successfully.');
  } catch (err) {
    console.error('Database initialization error:', err);
  }
})();

// 1. DASHBOARD / OVERVIEW
app.get('/api/overview', (req, res) => {
  try {
    const totalTravelers = queryOne('SELECT COUNT(*) as c FROM travelers').c || 0;
    const totalVendors = queryOne('SELECT COUNT(*) as c FROM vendors').c || 0;
    const totalLeaders = queryOne('SELECT COUNT(*) as c FROM leaders').c || 0;
    const totalTrips = queryOne('SELECT COUNT(*) as c FROM trips').c || 0;
    const totalBookingRequests = queryOne('SELECT COUNT(*) as c FROM bookings').c || 0;
    const activeTrips = queryOne("SELECT COUNT(*) as c FROM trips WHERE status = 'Active'").c || 0;
    const openTickets = queryOne("SELECT COUNT(*) as c FROM tickets WHERE status IN ('Open', 'In Progress')").c || 0;
    const recentActivity = queryAll('SELECT * FROM app_activities ORDER BY id DESC LIMIT 8');

    res.json({
      totalTravelers,
      totalVendors,
      totalLeaders,
      totalTrips,
      totalBookingRequests,
      activeTrips,
      openTickets,
      recentActivity
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 2. TRAVELER DATA
app.get('/api/travelers', (req, res) => {
  try {
    const data = queryAll('SELECT * FROM travelers ORDER BY id DESC');
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 3. VENDOR DATA
app.get('/api/vendors', (req, res) => {
  try {
    const data = queryAll('SELECT * FROM vendors ORDER BY id DESC');
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 4. LEADER / OPS DATA
app.get('/api/leaders', (req, res) => {
  try {
    const data = queryAll('SELECT * FROM leaders ORDER BY id DESC');
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 5. DESTINATION / CONTENT DATA
app.get('/api/destinations', (req, res) => {
  try {
    const data = queryAll('SELECT * FROM destinations ORDER BY id DESC');
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 6. BOOKING DATA
app.get('/api/bookings', (req, res) => {
  try {
    const data = queryAll('SELECT * FROM bookings ORDER BY id DESC');
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 7. TRIP DATA
app.get('/api/trips', (req, res) => {
  try {
    const data = queryAll('SELECT * FROM trips ORDER BY id DESC');
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 8. TICKETS / INCIDENTS
app.get('/api/tickets', (req, res) => {
  try {
    const data = queryAll('SELECT * FROM tickets ORDER BY id DESC');
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 9. SAFETY / EMERGENCY DATA
app.get('/api/safety', (req, res) => {
  try {
    const data = queryAll('SELECT * FROM safety_events ORDER BY id DESC');
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 10. BASIC ANALYTICS
app.get('/api/analytics', (req, res) => {
  try {
    const totalTravelers = queryOne('SELECT COUNT(*) as c FROM travelers').c || 0;
    const totalVendors = queryOne('SELECT COUNT(*) as c FROM vendors').c || 0;
    const verifiedVendors = queryOne("SELECT COUNT(*) as c FROM vendors WHERE verification_status = 'Verified'").c || 0;
    const pendingVendors = queryOne("SELECT COUNT(*) as c FROM vendors WHERE verification_status = 'Pending'").c || 0;

    const totalBookings = queryOne('SELECT COUNT(*) as c FROM bookings').c || 0;
    const completedBookings = queryOne("SELECT COUNT(*) as c FROM bookings WHERE status = 'Completed'").c || 0;
    const acceptedBookings = queryOne("SELECT COUNT(*) as c FROM bookings WHERE status = 'Accepted'").c || 0;
    const pendingBookings = queryOne("SELECT COUNT(*) as c FROM bookings WHERE status = 'Pending'").c || 0;
    const rejectedBookings = queryOne("SELECT COUNT(*) as c FROM bookings WHERE status = 'Rejected'").c || 0;

    const activeTrips = queryOne("SELECT COUNT(*) as c FROM trips WHERE status = 'Active'").c || 0;
    const plannedTrips = queryOne("SELECT COUNT(*) as c FROM trips WHERE status = 'Planned'").c || 0;
    const completedTrips = queryOne("SELECT COUNT(*) as c FROM trips WHERE status = 'Completed'").c || 0;

    const totalIncidents = queryOne('SELECT COUNT(*) as c FROM safety_events').c || 0;
    const activeIncidents = queryOne("SELECT COUNT(*) as c FROM safety_events WHERE resolution_status = 'Active' OR resolution_status = 'Dispatched'").c || 0;
    const resolvedIncidents = queryOne("SELECT COUNT(*) as c FROM safety_events WHERE resolution_status = 'Resolved'").c || 0;

    const popularDestinations = queryAll('SELECT destination, COUNT(*) as trip_count FROM trips GROUP BY destination ORDER BY trip_count DESC');

    const vendorPerformance = queryAll('SELECT business_name, booking_requests_count, accepted_bookings_count, rejected_bookings_count, verification_status FROM vendors ORDER BY accepted_bookings_count DESC');

    res.json({
      usersGrowth: { totalTravelers, totalVendors },
      vendorsOnboarded: { totalVendors, verifiedVendors, pendingVendors },
      bookingStats: { totalBookings, completedBookings, acceptedBookings, pendingBookings, rejectedBookings },
      tripStats: { activeTrips, plannedTrips, completedTrips },
      incidentsStats: { totalIncidents, activeIncidents, resolvedIncidents },
      popularDestinations,
      vendorPerformance
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// REST INGESTION APIs SIMULATING APP DATA FLOW (App -> API -> Central DB -> Web Dashboard)

// Ingest from Traveler App
app.post('/api/ingest/traveler', (req, res) => {
  try {
    const { action, name, email, phone, profile_info, destination, service, emergency_info } = req.body;
    const traveler_id = `TRV-${Date.now().toString().slice(-4)}`;
    
    execute(
      `INSERT INTO travelers (traveler_id, name, email, phone, profile_info, emergency_info, trip_status) VALUES (?, ?, ?, ?, ?, ?, 'Active')`,
      [traveler_id, name || 'New Traveler', email || 'traveler@app.com', phone || '+91 9000000000', profile_info || 'App Registered Traveler', emergency_info || 'None']
    );

    // Log Activity
    execute(
      `INSERT INTO app_activities (app_source, action, details) VALUES ('Traveler App', ?, ?)`,
      [action || 'New Traveler Registration', `${name || traveler_id} joined via Traveler App`]
    );

    res.status(201).json({ success: true, traveler_id, message: 'Data collected into Central Database' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Ingest from Vendor App
app.post('/api/ingest/vendor', (req, res) => {
  try {
    const { action, business_name, owner_contact, kyc_info, listings } = req.body;
    const vendor_id = `VND-${Date.now().toString().slice(-4)}`;

    execute(
      `INSERT INTO vendors (vendor_id, business_name, owner_contact, kyc_info, verification_status, listings, vendor_activity) VALUES (?, ?, ?, ?, 'Pending', ?, ?)`,
      [vendor_id, business_name || 'New Vendor Business', owner_contact || '+91 9111111111', kyc_info || 'Submitted via App', listings || 'Services', action || 'Onboarded via Vendor App']
    );

    execute(
      `INSERT INTO app_activities (app_source, action, details) VALUES ('Vendor App', ?, ?)`,
      [action || 'Vendor Onboarding', `${business_name || vendor_id} submitted KYC & listings`]
    );

    res.status(201).json({ success: true, vendor_id, message: 'Vendor Data collected into Central Database' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Ingest from Leader/Ops App
app.post('/api/ingest/leader', (req, res) => {
  try {
    const { action, leader_name, issue_resolved, incident_created } = req.body;

    execute(
      `INSERT INTO app_activities (app_source, action, details) VALUES ('Leader/Ops App', ?, ?)`,
      [action || 'Ops Update', `${leader_name || 'Leader'}: ${issue_resolved || incident_created || 'Logged operation detail'}`]
    );

    res.status(201).json({ success: true, message: 'Leader/Ops Data logged into Central Database' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Start Server
app.listen(PORT, () => {
  console.log(`Web Dashboard Backend API running on http://localhost:${PORT}`);
});

