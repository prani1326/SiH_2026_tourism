const { getDb, execute, queryOne } = require('./index');

async function seed() {
  await getDb();
  console.log('Seeding central database...');

  // 1. Travelers
  execute(`INSERT OR REPLACE INTO travelers (traveler_id, name, email, phone, profile_info, trips_count, itinerary, booking_requests_count, emergency_info, trip_status, created_at) VALUES 
    ('TRV-1001', 'Aarav Sharma', 'aarav@gmail.com', '+91 9876543210', 'Solo Explorer & Himalaya Trekker', 3, 'Manali -> Solang -> Kasol', 4, 'Emergency Contact: Ramesh Sharma (+91 9811122233)', 'Active', '2026-08-15 10:00:00'),
    ('TRV-1002', 'Ananya Roy', 'ananya.r@yahoo.com', '+91 9123456789', 'Adventure Group Lead (4 Pax)', 5, 'Goa North -> South Beach Circuit', 6, 'Emergency Contact: Sunita Roy (+91 9822233344)', 'In Transit', '2026-08-20 14:30:00'),
    ('TRV-1003', 'Vikramaditya Verma', 'v.verma@corp.com', '+91 9988776655', 'Cultural Backpacker & Heritage Lover', 2, 'Jaipur -> Udaipur -> Jaisalmer', 3, 'Emergency Contact: Pooja Verma (+91 9777665544)', 'Planned', '2026-08-25 09:15:00'),
    ('TRV-1004', 'Meera Iyer', 'meera.i@outlook.com', '+91 9445566778', 'Eco-Tourist & Backwater Enthusiast', 4, 'Munnar -> Alleppey Backwaters', 5, 'Emergency Contact: Suresh Iyer (+91 9334455667)', 'Idle', '2026-08-28 16:45:00'),
    ('TRV-1005', 'Rohan Deshmukh', 'rohan.d@gmail.com', '+91 9556677889', 'Weekend Hiker & Photographer', 1, 'Lonavala -> Khandala Trek', 1, 'Emergency Contact: Maya Deshmukh (+91 9443322110)', 'Active', '2026-09-01 11:20:00');
  `);

  // 2. Vendors
  execute(`INSERT OR REPLACE INTO vendors (vendor_id, business_name, owner_contact, kyc_info, verification_status, listings, booking_requests_count, accepted_bookings_count, rejected_bookings_count, vendor_activity, created_at) VALUES 
    ('VND-2001', 'Himalayan Trails & Stays', 'Rajesh Kumar (+91 9810012345)', 'GSTIN: 02AAACH1234F1Z9, PAN Verified, Tourism License Valid', 'Verified', 'Swiss Tents, Paragliding Pass, Trekking Gear', 18, 15, 3, 'Updated tent inventory and seasonal pricing', '2026-08-10 08:00:00'),
    ('VND-2002', 'Ocean Breeze Watersports & Villas', 'Goa Hospitality Pvt Ltd (+91 9820023456)', 'GSTIN: 30AABCO4321E1Z2, FSSAI & Water Safety Reg Verified', 'Verified', 'Scuba Diving Pass, Beachfront Villa 4BHK, JetSki Pass', 24, 21, 3, 'Confirmed 3 weekend water activity slots', '2026-08-12 12:00:00'),
    ('VND-2003', 'Desert Safari & Heritage Stays', 'Rawat Singh (+91 9830034567)', 'GSTIN: 08AAACD9876K1Z4, Verification In Progress', 'Pending', 'Camel Safari & Folk Night, Heritage Fort Rooms', 8, 5, 3, 'Submitted updated GSTIN & ID proofs', '2026-08-22 15:30:00'),
    ('VND-2004', 'Malabar Houseboat & Spice Resort', 'Thomas Mathew (+91 9840045678)', 'GSTIN: 32AABCM6543J1Z8, PAN & Boat License Verified', 'Verified', 'Deluxe Houseboat Cruise, Spice Garden Cottage', 14, 12, 2, 'Added monsoons package discounts', '2026-08-26 17:10:00');
  `);

  // 3. Leaders/Ops
  execute(`INSERT OR REPLACE INTO leaders (leader_id, name, role, assigned_work, approvals_count, tickets_handled_count, incidents_created_count, activity_history, created_at) VALUES 
    ('LDR-3001', 'Captain Suresh Menon', 'Senior Ops & Safety Lead', 'North Zone Circuit Safety & Incident Dispatch', 42, 38, 4, 'Resolved SOS Alert EMG-7002 at Manali Pass', '2026-08-01 09:00:00'),
    ('LDR-3002', 'Priya Sharma', 'Vendor Audit & Quality Manager', 'KYC Verification & Service Standards', 29, 25, 1, 'Approved KYC verification for Ocean Breeze Watersports', '2026-08-05 10:30:00'),
    ('LDR-3003', 'Amit Patel', 'Traveler Support Specialist', 'Booking Dispute Resolution & Lost Items', 18, 22, 2, 'Assigned Emergency Ticket TCK-6003 to local field team', '2026-08-15 14:00:00');
  `);

  // 4. Destinations / Content
  execute(`INSERT OR REPLACE INTO destinations (destination_name, places_pois, hotels, activities, restaurants, emergency_help_info, content_status, created_at) VALUES 
    ('Manali & Solang Valley', 'Hadimba Temple, Solang Valley Adventure Park, Jogini Waterfall, Rohtang Pass', 'Himalayan Heights Resort, Snow Valley Stay, Pinecrest Cottages', 'Paragliding, River Rafting, Snow Trekking, Camping', 'Cafe 1947, Johnson’s Cafe, Chopsticks Restaurant', 'District Hospital: 01902-252250, Police: 112, Mountain Rescue: +91 9816000000', 'Published', '2026-08-01 10:00:00'),
    ('Goa (North & South)', 'Calangute Beach, Fort Aguada, Dudhsagar Waterfalls, Palolem Beach', 'Taj Exotica, Ocean Palms Resort, Zostel Anjuna', 'Scuba Diving, Banana Boat Ride, Casino Cruise, Heritage Walk', 'Britto’s, Thalassa, Curlies, Martin’s Corner', 'Tourism Helpline: 1364, Calangute Police: 0832-2278223, Ambulance: 108', 'Published', '2026-08-05 11:30:00'),
    ('Jaipur & Jaisalmer', 'Amber Fort, Hawa Mahal, Sam Sand Dunes, Jaisalmer Fort', 'Chokhi Dhani Resort, Suryagarh Palace, Jaisalmer Desert Camp', 'Desert Safari, Quad Biking, Parasailing, Heritage Walking Tour', '1135 AD, Rawat Mishthan Bhandar, The Trio', 'Tourist Police Helpline: 0141-2821000, SMS Emergency: 1090', 'Published', '2026-08-10 14:20:00');
  `);

  // 5. Trips
  execute(`INSERT OR REPLACE INTO trips (trip_id, traveler_name, destination, start_date, end_date, number_of_travelers, budget, itinerary, status, created_at) VALUES 
    ('TRP-4001', 'Aarav Sharma', 'Manali & Solang Valley', '2026-09-05', '2026-09-10', 1, 25000.00, 'Day 1: Hadimba Temple, Day 2: Solang Paragliding, Day 3: Rafting, Day 4: Kasol', 'Active', '2026-08-28 10:00:00'),
    ('TRP-4002', 'Ananya Roy', 'Goa (North & South)', '2026-09-12', '2026-09-17', 4, 75000.00, 'Day 1: Beach Day, Day 2: Scuba Diving Grand Island, Day 3: Waterfalls Trek', 'Planned', '2026-08-29 11:30:00'),
    ('TRP-4003', 'Vikramaditya Verma', 'Jaipur & Jaisalmer', '2026-09-01', '2026-09-06', 2, 45000.00, 'Day 1-2: Jaipur Amber Fort, Day 3-5: Jaisalmer Sam Sand Dunes Camp', 'Completed', '2026-08-25 15:45:00');
  `);

  // 6. Bookings
  execute(`INSERT OR REPLACE INTO bookings (booking_id, traveler_name, vendor_name, trip_name, service, booking_date, amount, status, created_at) VALUES 
    ('BKG-5001', 'Aarav Sharma', 'Himalayan Trails & Stays', 'TRP-4001', 'Solang Paragliding & Swiss Tent', '2026-09-06', 8500.00, 'Accepted', '2026-08-30 09:30:00'),
    ('BKG-5002', 'Ananya Roy', 'Ocean Breeze Watersports & Villas', 'TRP-4002', 'Scuba Diving Pass 4 Pax', '2026-09-13', 18000.00, 'Accepted', '2026-08-31 14:15:00'),
    ('BKG-5003', 'Vikramaditya Verma', 'Desert Safari & Heritage Stays', 'TRP-4003', 'Desert Safari & Sunset Camel Ride', '2026-09-03', 6000.00, 'Completed', '2026-08-26 16:00:00'),
    ('BKG-5004', 'Meera Iyer', 'Malabar Houseboat & Spice Resort', 'TRP-4004', 'Deluxe Houseboat Overnight Cruise', '2026-09-20', 14500.00, 'Pending', '2026-09-01 10:20:00'),
    ('BKG-5005', 'Rohan Deshmukh', 'Himalayan Trails & Stays', 'TRP-4005', 'Camping Gear Rental Set', '2026-09-08', 2200.00, 'Rejected', '2026-09-01 18:00:00');
  `);

  // 7. Tickets / Incidents
  execute(`INSERT OR REPLACE INTO tickets (ticket_id, reporter, issue, category, priority, assigned_leader, status, resolution, timestamp) VALUES 
    ('TCK-6001', 'Traveler: Aarav Sharma', 'Weather delay on paragliding slot, timing reschedule request', 'Booking Request', 'Medium', 'Captain Suresh Menon', 'In Progress', 'Vendor agreed to move slot to 2:00 PM tomorrow', '2026-09-02 10:30:00'),
    ('TCK-6002', 'Vendor: Ocean Breeze Watersports', 'Traveler group 20 mins late for boat departure', 'Vendor Conflict', 'Low', 'Priya Sharma', 'Resolved', 'Rescheduled transfer to next boat departure batch', '2026-09-01 14:15:00'),
    ('TCK-6003', 'Traveler: Rohan Deshmukh', 'Lost mobile phone near Lonavala trekking trail head', 'Emergency', 'High', 'Amit Patel', 'Open', 'Ops field guide dispatched to search trail point', '2026-09-02 18:45:00');
  `);

  // 8. Safety / Emergency Data
  execute(`INSERT OR REPLACE INTO safety_events (event_id, event_type, user_name, contact_number, location, active_incidents, resolution_status, created_at) VALUES 
    ('EMG-7001', 'SOS Event', 'Rohan Deshmukh', '+91 9556677889', 'Lonavala Tiger Point Trail (Lat: 18.755, Long: 73.408)', 'Phone dropped on steep trail, traveler safe with group', 'Dispatched', '2026-09-02 18:40:00'),
    ('EMG-7002', 'Lost-phone Event', 'Vikramaditya Verma', '+91 9988776655', 'Jaisalmer Fort Inner Market', 'Phone lost in market, recovered by local police hotline', 'Resolved', '2026-09-01 21:10:00'),
    ('EMG-7003', 'Emergency Request', 'Ananya Roy', '+91 9123456789', 'Calangute Beach Road', 'First-aid assistance required for minor ankle sprain', 'Resolved', '2026-08-30 16:20:00');
  `);

  // 9. App Activities (Ingestion Flow Log)
  execute(`INSERT OR REPLACE INTO app_activities (app_source, action, details, timestamp) VALUES 
    ('Traveler App', 'New Booking Request', 'Aarav Sharma sent request for Solang Paragliding (BKG-5001)', '2026-09-02 09:30:00'),
    ('Vendor App', 'Booking Accepted', 'Ocean Breeze Watersports accepted booking BKG-5002', '2026-09-02 10:15:00'),
    ('Leader/Ops App', 'Ticket Escalated', 'Amit Patel assigned Lost-phone ticket TCK-6003 to ground ops', '2026-09-02 18:45:00'),
    ('Traveler App', 'SOS Alert Triggered', 'Rohan Deshmukh triggered SOS button at Lonavala Trail', '2026-09-02 18:40:00'),
    ('Vendor App', 'KYC Document Upload', 'Rawat Singh (VND-2003) submitted updated GSTIN proof', '2026-09-01 15:20:00');
  `);

  console.log('Central database successfully seeded!');
}

if (require.main === module) {
  seed().catch(console.error);
}

module.exports = { seed };

