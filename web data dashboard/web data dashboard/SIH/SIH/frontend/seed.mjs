import { initializeApp } from "firebase/app";
import { getFirestore, setDoc, doc, collection, addDoc } from "firebase/firestore";

const firebaseConfig = {
  apiKey: "AIzaSyAZl_hz3aXRXWLAzBco8wefyJbH6Tqbr6s",
  authDomain: "webdashboard-d040c.firebaseapp.com",
  projectId: "webdashboard-d040c",
  storageBucket: "webdashboard-d040c.firebasestorage.app",
  messagingSenderId: "735666011006",
  appId: "1:735666011006:web:6f5e7d8002787fba7b912f",
  measurementId: "G-Q6J0JYM4QC"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

async function runSeed() {
  console.log("🌱 Starting Firestore Seeding for webdashboard-d040c...");

  // Travelers
  const travelers = [
    { traveler_id: 'TRV-1001', name: 'Aarav Sharma', email: 'aarav@gmail.com', phone: '+91 9876543210', profile_info: 'Solo Explorer & Himalaya Trekker', trips_count: 3, itinerary: 'Manali -> Solang -> Kasol', booking_requests_count: 4, emergency_info: 'Emergency Contact: Ramesh Sharma (+91 9811122233)', trip_status: 'Active', created_at: '2026-08-15 10:00:00' },
    { traveler_id: 'TRV-1002', name: 'Ananya Roy', email: 'ananya.r@yahoo.com', phone: '+91 9123456789', profile_info: 'Adventure Group Lead (4 Pax)', trips_count: 5, itinerary: 'Goa North -> South Beach Circuit', booking_requests_count: 6, emergency_info: 'Emergency Contact: Sunita Roy (+91 9822233344)', trip_status: 'In Transit', created_at: '2026-08-20 14:30:00' },
    { traveler_id: 'TRV-1003', name: 'Vikramaditya Verma', email: 'v.verma@corp.com', phone: '+91 9988776655', profile_info: 'Cultural Backpacker & Heritage Lover', trips_count: 2, itinerary: 'Jaipur -> Udaipur -> Jaisalmer', booking_requests_count: 3, emergency_info: 'Emergency Contact: Pooja Verma (+91 9777665544)', trip_status: 'Planned', created_at: '2026-08-25 09:15:00' },
    { traveler_id: 'TRV-1004', name: 'Meera Iyer', email: 'meera.i@outlook.com', phone: '+91 9445566778', profile_info: 'Eco-Tourist & Backwater Enthusiast', trips_count: 4, itinerary: 'Munnar -> Alleppey Backwaters', booking_requests_count: 5, emergency_info: 'Emergency Contact: Suresh Iyer (+91 9334455667)', trip_status: 'Idle', created_at: '2026-08-28 16:45:00' },
    { traveler_id: 'TRV-1005', name: 'Rohan Deshmukh', email: 'rohan.d@gmail.com', phone: '+91 9556677889', profile_info: 'Weekend Hiker & Photographer', trips_count: 1, itinerary: 'Lonavala -> Khandala Trek', booking_requests_count: 1, emergency_info: 'Emergency Contact: Maya Deshmukh (+91 9443322110)', trip_status: 'Active', created_at: '2026-09-01 11:20:00' },
    { traveler_id: 'TRV-1006', name: 'Priya Nair', email: 'priya.nair@gmail.com', phone: '+91 9667788990', profile_info: 'App Registered Traveler', trips_count: 0, itinerary: '', booking_requests_count: 1, emergency_info: 'None', trip_status: 'Active', created_at: '2026-09-02 16:53:02' }
  ];
  for (const t of travelers) {
    await setDoc(doc(db, 'travelers', t.traveler_id), t);
  }
  console.log("✅ Travelers seeded");

  // Vendors
  const vendors = [
    { vendor_id: 'VND-2001', business_name: 'Himalayan Trails & Stays', owner_contact: 'Rajesh Kumar (+91 9810012345)', kyc_info: 'GSTIN: 02AAACH1234F1Z9, PAN Verified, Tourism License Valid', verification_status: 'Verified', listings: 'Swiss Tents, Paragliding Pass, Trekking Gear', booking_requests_count: 18, accepted_bookings_count: 15, rejected_bookings_count: 3, vendor_activity: 'Updated tent inventory and seasonal pricing', created_at: '2026-08-10 08:00:00' },
    { vendor_id: 'VND-2002', business_name: 'Ocean Breeze Watersports & Villas', owner_contact: 'Goa Hospitality Pvt Ltd (+91 9820023456)', kyc_info: 'GSTIN: 30AABCO4321E1Z2, FSSAI & Water Safety Reg Verified', verification_status: 'Verified', listings: 'Scuba Diving Pass, Beachfront Villa 4BHK, JetSki Pass', booking_requests_count: 24, accepted_bookings_count: 21, rejected_bookings_count: 3, vendor_activity: 'Confirmed 3 weekend water activity slots', created_at: '2026-08-12 12:00:00' },
    { vendor_id: 'VND-2003', business_name: 'Desert Safari & Heritage Stays', owner_contact: 'Rawat Singh (+91 9830034567)', kyc_info: 'GSTIN: 08AAACD9876K1Z4, Verification In Progress', verification_status: 'Pending', listings: 'Camel Safari & Folk Night, Heritage Fort Rooms', booking_requests_count: 8, accepted_bookings_count: 5, rejected_bookings_count: 3, vendor_activity: 'Submitted updated GSTIN & ID proofs', created_at: '2026-08-22 15:30:00' },
    { vendor_id: 'VND-2004', business_name: 'Malabar Houseboat & Spice Resort', owner_contact: 'Thomas Mathew (+91 9840045678)', kyc_info: 'GSTIN: 32AABCM6543J1Z8, PAN & Boat License Verified', verification_status: 'Verified', listings: 'Deluxe Houseboat Cruise, Spice Garden Cottage', booking_requests_count: 14, accepted_bookings_count: 12, rejected_bookings_count: 2, vendor_activity: 'Added monsoons package discounts', created_at: '2026-08-26 17:10:00' }
  ];
  for (const v of vendors) {
    await setDoc(doc(db, 'vendors', v.vendor_id), v);
  }
  console.log("✅ Vendors seeded");

  // Leaders
  const leaders = [
    { leader_id: 'LDR-3001', name: 'Captain Suresh Menon', role: 'Senior Ops & Safety Lead', assigned_work: 'North Zone Circuit Safety & Incident Dispatch', approvals_count: 42, tickets_handled_count: 38, incidents_created_count: 4, activity_history: 'Resolved SOS Alert EMG-7002 at Manali Pass', created_at: '2026-08-01 09:00:00' },
    { leader_id: 'LDR-3002', name: 'Priya Sharma', role: 'Vendor Audit & Quality Manager', assigned_work: 'KYC Verification & Service Standards', approvals_count: 29, tickets_handled_count: 25, incidents_created_count: 1, activity_history: 'Approved KYC verification for Ocean Breeze Watersports', created_at: '2026-08-05 10:30:00' },
    { leader_id: 'LDR-3003', name: 'Amit Patel', role: 'Traveler Support Specialist', assigned_work: 'Booking Dispute Resolution & Lost Items', approvals_count: 18, tickets_handled_count: 22, incidents_created_count: 2, activity_history: 'Assigned Emergency Ticket TCK-6003 to local field team', created_at: '2026-08-15 14:00:00' }
  ];
  for (const l of leaders) {
    await setDoc(doc(db, 'leaders', l.leader_id), l);
  }
  console.log("✅ Leaders seeded");

  // Destinations
  const destinations = [
    { destination_name: 'Manali & Solang Valley', places_pois: 'Hadimba Temple, Solang Valley Adventure Park, Jogini Waterfall, Rohtang Pass', hotels: 'Himalayan Heights Resort, Snow Valley Stay, Pinecrest Cottages', activities: 'Paragliding, River Rafting, Snow Trekking, Camping', restaurants: 'Cafe 1947, Johnson\'s Cafe, Chopsticks Restaurant', emergency_help_info: 'District Hospital: 01902-252250, Police: 112, Mountain Rescue: +91 9816000000', content_status: 'Published', created_at: '2026-08-01 10:00:00' },
    { destination_name: 'Goa (North & South)', places_pois: 'Calangute Beach, Fort Aguada, Dudhsagar Waterfalls, Palolem Beach', hotels: 'Taj Exotica, Ocean Palms Resort, Zostel Anjuna', activities: 'Scuba Diving, Banana Boat Ride, Casino Cruise, Heritage Walk', restaurants: 'Britto\'s, Thalassa, Curlies, Martin\'s Corner', emergency_help_info: 'Tourism Helpline: 1364, Calangute Police: 0832-2278223, Ambulance: 108', content_status: 'Published', created_at: '2026-08-05 11:30:00' },
    { destination_name: 'Jaipur & Jaisalmer', places_pois: 'Amber Fort, Hawa Mahal, Sam Sand Dunes, Jaisalmer Fort', hotels: 'Chokhi Dhani Resort, Suryagarh Palace, Jaisalmer Desert Camp', activities: 'Desert Safari, Quad Biking, Parasailing, Heritage Walking Tour', restaurants: '1135 AD, Rawat Mishthan Bhandar, The Trio', emergency_help_info: 'Tourist Police Helpline: 0141-2821000, SMS Emergency: 1090', content_status: 'Published', created_at: '2026-08-10 14:20:00' }
  ];
  for (const d of destinations) {
    await setDoc(doc(db, 'destinations', d.destination_name.replace(/[\/\s&()]+/g, '_')), d);
  }
  console.log("✅ Destinations seeded");

  // Trips
  const trips = [
    { trip_id: 'TRP-4001', traveler_name: 'Aarav Sharma', destination: 'Manali & Solang Valley', start_date: '2026-09-05', end_date: '2026-09-10', number_of_travelers: 1, budget: 25000.00, itinerary: 'Day 1: Hadimba Temple, Day 2: Solang Paragliding, Day 3: Rafting, Day 4: Kasol', status: 'Active', created_at: '2026-08-28 10:00:00' },
    { trip_id: 'TRP-4002', traveler_name: 'Ananya Roy', destination: 'Goa (North & South)', start_date: '2026-09-12', end_date: '2026-09-17', number_of_travelers: 4, budget: 75000.00, itinerary: 'Day 1: Beach Day, Day 2: Scuba Diving Grand Island, Day 3: Waterfalls Trek', status: 'Planned', created_at: '2026-08-29 11:30:00' },
    { trip_id: 'TRP-4003', traveler_name: 'Vikramaditya Verma', destination: 'Jaipur & Jaisalmer', start_date: '2026-09-01', end_date: '2026-09-06', number_of_travelers: 2, budget: 45000.00, itinerary: 'Day 1-2: Jaipur Amber Fort, Day 3-5: Jaisalmer Sam Sand Dunes Camp', status: 'Completed', created_at: '2026-08-25 15:45:00' }
  ];
  for (const t of trips) {
    await setDoc(doc(db, 'trips', t.trip_id), t);
  }
  console.log("✅ Trips seeded");

  // Bookings
  const bookings = [
    { booking_id: 'BKG-5001', traveler_name: 'Aarav Sharma', vendor_name: 'Himalayan Trails & Stays', trip_name: 'TRP-4001', service: 'Solang Paragliding & Swiss Tent', booking_date: '2026-09-06', amount: 8500.00, status: 'Accepted', created_at: '2026-08-30 09:30:00' },
    { booking_id: 'BKG-5002', traveler_name: 'Ananya Roy', vendor_name: 'Ocean Breeze Watersports & Villas', trip_name: 'TRP-4002', service: 'Scuba Diving Grand Island (4 Pax)', booking_date: '2026-09-13', amount: 14000.00, status: 'Accepted', created_at: '2026-08-31 14:15:00' },
    { booking_id: 'BKG-5003', traveler_name: 'Vikramaditya Verma', vendor_name: 'Desert Safari & Heritage Stays', trip_name: 'TRP-4003', service: 'Sam Sand Dunes Luxury Camp & Safari', booking_date: '2026-09-03', amount: 12500.00, status: 'Completed', created_at: '2026-08-27 16:00:00' }
  ];
  for (const b of bookings) {
    await setDoc(doc(db, 'bookings', b.booking_id), b);
  }
  console.log("✅ Bookings seeded");

  // Tickets
  const tickets = [
    { ticket_id: 'TCK-6001', traveler_name: 'Aarav Sharma', issue_type: 'Booking Modification', priority: 'Medium', status: 'In Progress', assigned_leader: 'Amit Patel', response_history: 'Traveler requested date shift for Paragliding slot by 1 day', timestamp: '2026-09-06 08:30:00' },
    { ticket_id: 'TCK-6002', traveler_name: 'Meera Iyer', issue_type: 'Payment / Refund Query', priority: 'Low', status: 'Closed', assigned_leader: 'Priya Sharma', response_history: 'Refund of Rs 2,500 credited to traveler account via UPI', timestamp: '2026-09-04 11:20:00' },
    { ticket_id: 'TCK-6003', traveler_name: 'Vikramaditya Verma', issue_type: 'Lost Item / Trail Assistance', priority: 'High', status: 'Open', assigned_leader: 'Captain Suresh Menon', response_history: 'Backpack left in safari jeep; local desk contacting driver', timestamp: '2026-09-05 18:45:00' }
  ];
  for (const tk of tickets) {
    await setDoc(doc(db, 'tickets', tk.ticket_id), tk);
  }
  console.log("✅ Tickets seeded");

  // Safety Events
  const safety = [
    { event_id: 'EMG-7001', traveler_name: 'Rohan Deshmukh', trip_name: 'TRP-4001', alert_type: 'SOS Button Triggered', severity: 'High', location: 'Lonavala Western Ridge (18.7557 N, 73.4091 E)', status: 'Active', leader_action_history: 'Triggered from mobile app. Local ranger team notified at 11:45 AM.', created_at: '2026-09-06 11:45:00' },
    { event_id: 'EMG-7002', traveler_name: 'Aarav Sharma', trip_name: 'TRP-4001', alert_type: 'Severe Weather Warning', severity: 'Critical', location: 'Solang Valley (32.3166 N, 77.1578 E)', status: 'Resolved', leader_action_history: 'Landslide alert near Solang Pass. Capt. Suresh dispatched advisory.', created_at: '2026-09-05 14:00:00' },
    { event_id: 'EMG-7003', traveler_name: 'Ananya Roy', trip_name: 'TRP-4002', alert_type: 'High Tide Warning', severity: 'Medium', location: 'Calangute Coast (15.5430 N, 73.7554 E)', status: 'Resolved', leader_action_history: 'Lifeguard flagged red flag zones. All water sports paused.', created_at: '2026-09-04 09:30:00' }
  ];
  for (const s of safety) {
    await setDoc(doc(db, 'safety_events', s.event_id), s);
  }
  console.log("✅ Safety Events seeded");

  // App Activities
  const activities = [
    { app_source: 'Traveler App', activity_type: 'Profile Update', description: 'Aarav Sharma updated emergency contact details', timestamp: '2026-09-06 12:10:00' },
    { app_source: 'Vendor App', activity_type: 'Inventory Update', description: 'Himalayan Trails added 4 new Swiss tent slots', timestamp: '2026-09-06 11:35:00' },
    { app_source: 'Leader App', activity_type: 'SOS Alert Ack', description: 'Capt. Suresh acknowledged SOS Alert EMG-7001', timestamp: '2026-09-06 11:48:00' },
    { app_source: 'Traveler App', activity_type: 'Booking Created', description: 'Ananya Roy booked Scuba Diving package in Goa', timestamp: '2026-09-06 10:20:00' }
  ];
  for (const a of activities) {
    await addDoc(collection(db, 'app_activities'), a);
  }
  console.log("✅ App Activities seeded");

  console.log("🎉 ALL DATA SEEDED SUCCESSFULLY TO FIRESTORE!");
}

runSeed().catch(err => {
  console.error("❌ Seed Error:", err.message);
});
