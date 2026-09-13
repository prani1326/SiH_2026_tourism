/**
 * Firestore Seed Script
 * 
 * Run this ONCE to populate your Firestore with initial seed data.
 * Import and call seedFirestore() from browser console or a button.
 * 
 * Usage in browser console:
 *   import('./seedFirestore.js').then(m => m.seedFirestore())
 */

import { db } from '../firebase';
import { setDoc, doc, addDoc, collection } from 'firebase/firestore';

export async function seedFirestore() {
  console.log('🌱 Seeding Firestore...');

  // 1. Travelers
  const travelers = [
    {
      traveler_id: 'TRV-1001', name: 'Aarav Sharma', email: 'aarav@gmail.com', phone: '+91 9876543210',
      profile_info: 'Solo Explorer & Himalaya Trekker', trips_count: 3, itinerary: 'Manali -> Solang -> Kasol',
      booking_requests_count: 4, emergency_info: 'Emergency Contact: Ramesh Sharma (+91 9811122233)',
      trip_status: 'Active', created_at: '2026-08-15 10:00:00'
    },
    {
      traveler_id: 'TRV-1002', name: 'Ananya Roy', email: 'ananya.r@yahoo.com', phone: '+91 9123456789',
      profile_info: 'Adventure Group Lead (4 Pax)', trips_count: 5, itinerary: 'Goa North -> South Beach Circuit',
      booking_requests_count: 6, emergency_info: 'Emergency Contact: Sunita Roy (+91 9822233344)',
      trip_status: 'In Transit', created_at: '2026-08-20 14:30:00'
    },
    {
      traveler_id: 'TRV-1003', name: 'Vikramaditya Verma', email: 'v.verma@corp.com', phone: '+91 9988776655',
      profile_info: 'Cultural Backpacker & Heritage Lover', trips_count: 2, itinerary: 'Jaipur -> Udaipur -> Jaisalmer',
      booking_requests_count: 3, emergency_info: 'Emergency Contact: Pooja Verma (+91 9777665544)',
      trip_status: 'Planned', created_at: '2026-08-25 09:15:00'
    },
    {
      traveler_id: 'TRV-1004', name: 'Meera Iyer', email: 'meera.i@outlook.com', phone: '+91 9445566778',
      profile_info: 'Eco-Tourist & Backwater Enthusiast', trips_count: 4, itinerary: 'Munnar -> Alleppey Backwaters',
      booking_requests_count: 5, emergency_info: 'Emergency Contact: Suresh Iyer (+91 9334455667)',
      trip_status: 'Idle', created_at: '2026-08-28 16:45:00'
    },
    {
      traveler_id: 'TRV-1005', name: 'Rohan Deshmukh', email: 'rohan.d@gmail.com', phone: '+91 9556677889',
      profile_info: 'Weekend Hiker & Photographer', trips_count: 1, itinerary: 'Lonavala -> Khandala Trek',
      booking_requests_count: 1, emergency_info: 'Emergency Contact: Maya Deshmukh (+91 9443322110)',
      trip_status: 'Active', created_at: '2026-09-01 11:20:00'
    },
    {
      traveler_id: 'TRV-1006', name: 'Priya Nair', email: 'priya.nair@gmail.com', phone: '+91 9667788990',
      profile_info: 'App Registered Traveler', trips_count: 0, itinerary: '',
      booking_requests_count: 1, emergency_info: 'None',
      trip_status: 'Active', created_at: '2026-09-02 16:53:02'
    }
  ];

  for (const t of travelers) {
    await setDoc(doc(db, 'travelers', t.traveler_id), t);
  }
  console.log('✅ Travelers seeded');

  // 2. Vendors
  const vendors = [
    {
      vendor_id: 'VND-2001', business_name: 'Himalayan Trails & Stays',
      owner_contact: 'Rajesh Kumar (+91 9810012345)',
      kyc_info: 'GSTIN: 02AAACH1234F1Z9, PAN Verified, Tourism License Valid',
      verification_status: 'Verified', listings: 'Swiss Tents, Paragliding Pass, Trekking Gear',
      booking_requests_count: 18, accepted_bookings_count: 15, rejected_bookings_count: 3,
      vendor_activity: 'Updated tent inventory and seasonal pricing', created_at: '2026-08-10 08:00:00'
    },
    {
      vendor_id: 'VND-2002', business_name: 'Ocean Breeze Watersports & Villas',
      owner_contact: 'Goa Hospitality Pvt Ltd (+91 9820023456)',
      kyc_info: 'GSTIN: 30AABCO4321E1Z2, FSSAI & Water Safety Reg Verified',
      verification_status: 'Verified', listings: 'Scuba Diving Pass, Beachfront Villa 4BHK, JetSki Pass',
      booking_requests_count: 24, accepted_bookings_count: 21, rejected_bookings_count: 3,
      vendor_activity: 'Confirmed 3 weekend water activity slots', created_at: '2026-08-12 12:00:00'
    },
    {
      vendor_id: 'VND-2003', business_name: 'Desert Safari & Heritage Stays',
      owner_contact: 'Rawat Singh (+91 9830034567)',
      kyc_info: 'GSTIN: 08AAACD9876K1Z4, Verification In Progress',
      verification_status: 'Pending', listings: 'Camel Safari & Folk Night, Heritage Fort Rooms',
      booking_requests_count: 8, accepted_bookings_count: 5, rejected_bookings_count: 3,
      vendor_activity: 'Submitted updated GSTIN & ID proofs', created_at: '2026-08-22 15:30:00'
    },
    {
      vendor_id: 'VND-2004', business_name: 'Malabar Houseboat & Spice Resort',
      owner_contact: 'Thomas Mathew (+91 9840045678)',
      kyc_info: 'GSTIN: 32AABCM6543J1Z8, PAN & Boat License Verified',
      verification_status: 'Verified', listings: 'Deluxe Houseboat Cruise, Spice Garden Cottage',
      booking_requests_count: 14, accepted_bookings_count: 12, rejected_bookings_count: 2,
      vendor_activity: 'Added monsoons package discounts', created_at: '2026-08-26 17:10:00'
    }
  ];

  for (const v of vendors) {
    await setDoc(doc(db, 'vendors', v.vendor_id), v);
  }
  console.log('✅ Vendors seeded');

  // 3. Leaders
  const leaders = [
    {
      leader_id: 'LDR-3001', name: 'Captain Suresh Menon', role: 'Senior Ops & Safety Lead',
      assigned_work: 'North Zone Circuit Safety & Incident Dispatch',
      approvals_count: 42, tickets_handled_count: 38, incidents_created_count: 4,
      activity_history: 'Resolved SOS Alert EMG-7002 at Manali Pass', created_at: '2026-08-01 09:00:00'
    },
    {
      leader_id: 'LDR-3002', name: 'Priya Sharma', role: 'Vendor Audit & Quality Manager',
      assigned_work: 'KYC Verification & Service Standards',
      approvals_count: 29, tickets_handled_count: 25, incidents_created_count: 1,
      activity_history: 'Approved KYC verification for Ocean Breeze Watersports', created_at: '2026-08-05 10:30:00'
    },
    {
      leader_id: 'LDR-3003', name: 'Amit Patel', role: 'Traveler Support Specialist',
      assigned_work: 'Booking Dispute Resolution & Lost Items',
      approvals_count: 18, tickets_handled_count: 22, incidents_created_count: 2,
      activity_history: 'Assigned Emergency Ticket TCK-6003 to local field team', created_at: '2026-08-15 14:00:00'
    }
  ];

  for (const l of leaders) {
    await setDoc(doc(db, 'leaders', l.leader_id), l);
  }
  console.log('✅ Leaders seeded');

  // 4. Destinations
  const destinations = [
    {
      destination_name: 'Manali & Solang Valley',
      places_pois: 'Hadimba Temple, Solang Valley Adventure Park, Jogini Waterfall, Rohtang Pass',
      hotels: 'Himalayan Heights Resort, Snow Valley Stay, Pinecrest Cottages',
      activities: 'Paragliding, River Rafting, Snow Trekking, Camping',
      restaurants: 'Cafe 1947, Johnson\'s Cafe, Chopsticks Restaurant',
      emergency_help_info: 'District Hospital: 01902-252250, Police: 112, Mountain Rescue: +91 9816000000',
      content_status: 'Published', created_at: '2026-08-01 10:00:00'
    },
    {
      destination_name: 'Goa (North & South)',
      places_pois: 'Calangute Beach, Fort Aguada, Dudhsagar Waterfalls, Palolem Beach',
      hotels: 'Taj Exotica, Ocean Palms Resort, Zostel Anjuna',
      activities: 'Scuba Diving, Banana Boat Ride, Casino Cruise, Heritage Walk',
      restaurants: 'Britto\'s, Thalassa, Curlies, Martin\'s Corner',
      emergency_help_info: 'Tourism Helpline: 1364, Calangute Police: 0832-2278223, Ambulance: 108',
      content_status: 'Published', created_at: '2026-08-05 11:30:00'
    },
    {
      destination_name: 'Jaipur & Jaisalmer',
      places_pois: 'Amber Fort, Hawa Mahal, Sam Sand Dunes, Jaisalmer Fort',
      hotels: 'Chokhi Dhani Resort, Suryagarh Palace, Jaisalmer Desert Camp',
      activities: 'Desert Safari, Quad Biking, Parasailing, Heritage Walking Tour',
      restaurants: '1135 AD, Rawat Mishthan Bhandar, The Trio',
      emergency_help_info: 'Tourist Police Helpline: 0141-2821000, SMS Emergency: 1090',
      content_status: 'Published', created_at: '2026-08-10 14:20:00'
    }
  ];

  for (const d of destinations) {
    await setDoc(doc(db, 'destinations', d.destination_name.replace(/[\/\s&()]+/g, '_')), d);
  }
  console.log('✅ Destinations seeded');

  // 5. Trips
  const trips = [
    {
      trip_id: 'TRP-4001', traveler_name: 'Aarav Sharma', destination: 'Manali & Solang Valley',
      start_date: '2026-09-05', end_date: '2026-09-10', number_of_travelers: 1, budget: 25000.00,
      itinerary: 'Day 1: Hadimba Temple, Day 2: Solang Paragliding, Day 3: Rafting, Day 4: Kasol',
      status: 'Active', created_at: '2026-08-28 10:00:00'
    },
    {
      trip_id: 'TRP-4002', traveler_name: 'Ananya Roy', destination: 'Goa (North & South)',
      start_date: '2026-09-12', end_date: '2026-09-17', number_of_travelers: 4, budget: 75000.00,
      itinerary: 'Day 1: Beach Day, Day 2: Scuba Diving Grand Island, Day 3: Waterfalls Trek',
      status: 'Planned', created_at: '2026-08-29 11:30:00'
    },
    {
      trip_id: 'TRP-4003', traveler_name: 'Vikramaditya Verma', destination: 'Jaipur & Jaisalmer',
      start_date: '2026-09-01', end_date: '2026-09-06', number_of_travelers: 2, budget: 45000.00,
      itinerary: 'Day 1-2: Jaipur Amber Fort, Day 3-5: Jaisalmer Sam Sand Dunes Camp',
      status: 'Completed', created_at: '2026-08-25 15:45:00'
    }
  ];

  for (const t of trips) {
    await setDoc(doc(db, 'trips', t.trip_id), t);
  }
  console.log('✅ Trips seeded');

  // 6. Bookings
  const bookings = [
    {
      booking_id: 'BKG-5001', traveler_name: 'Aarav Sharma', vendor_name: 'Himalayan Trails & Stays',
      trip_name: 'TRP-4001', service: 'Solang Paragliding & Swiss Tent', booking_date: '2026-09-06',
      amount: 8500.00, status: 'Accepted', created_at: '2026-08-30 09:30:00'
    },
    {
      booking_id: 'BKG-5002', traveler_name: 'Ananya Roy', vendor_name: 'Ocean Breeze Watersports & Villas',
      trip_name: 'TRP-4002', service: 'Scuba Diving Pass 4 Pax', booking_date: '2026-09-13',
      amount: 18000.00, status: 'Accepted', created_at: '2026-08-31 14:15:00'
    },
    {
      booking_id: 'BKG-5003', traveler_name: 'Vikramaditya Verma', vendor_name: 'Desert Safari & Heritage Stays',
      trip_name: 'TRP-4003', service: 'Desert Safari & Sunset Camel Ride', booking_date: '2026-09-03',
      amount: 6000.00, status: 'Completed', created_at: '2026-08-26 16:00:00'
    },
    {
      booking_id: 'BKG-5004', traveler_name: 'Meera Iyer', vendor_name: 'Malabar Houseboat & Spice Resort',
      trip_name: 'TRP-4004', service: 'Deluxe Houseboat Overnight Cruise', booking_date: '2026-09-20',
      amount: 14500.00, status: 'Pending', created_at: '2026-09-01 10:20:00'
    },
    {
      booking_id: 'BKG-5005', traveler_name: 'Rohan Deshmukh', vendor_name: 'Himalayan Trails & Stays',
      trip_name: 'TRP-4005', service: 'Camping Gear Rental Set', booking_date: '2026-09-08',
      amount: 2200.00, status: 'Rejected', created_at: '2026-09-01 18:00:00'
    }
  ];

  for (const b of bookings) {
    await setDoc(doc(db, 'bookings', b.booking_id), b);
  }
  console.log('✅ Bookings seeded');

  // 7. Tickets
  const tickets = [
    {
      ticket_id: 'TCK-6001', reporter: 'Traveler: Aarav Sharma',
      issue: 'Weather delay on paragliding slot, timing reschedule request',
      category: 'Booking Request', priority: 'Medium', assigned_leader: 'Captain Suresh Menon',
      status: 'In Progress', resolution: 'Vendor agreed to move slot to 2:00 PM tomorrow',
      timestamp: '2026-09-02 10:30:00'
    },
    {
      ticket_id: 'TCK-6002', reporter: 'Vendor: Ocean Breeze Watersports',
      issue: 'Traveler group 20 mins late for boat departure',
      category: 'Vendor Conflict', priority: 'Low', assigned_leader: 'Priya Sharma',
      status: 'Resolved', resolution: 'Rescheduled transfer to next boat departure batch',
      timestamp: '2026-09-01 14:15:00'
    },
    {
      ticket_id: 'TCK-6003', reporter: 'Traveler: Rohan Deshmukh',
      issue: 'Lost mobile phone near Lonavala trekking trail head',
      category: 'Emergency', priority: 'High', assigned_leader: 'Amit Patel',
      status: 'Open', resolution: 'Ops field guide dispatched to search trail point',
      timestamp: '2026-09-02 18:45:00'
    }
  ];

  for (const t of tickets) {
    await setDoc(doc(db, 'tickets', t.ticket_id), t);
  }
  console.log('✅ Tickets seeded');

  // 8. Safety Events
  const safetyEvents = [
    {
      event_id: 'EMG-7001', event_type: 'SOS Event', user_name: 'Rohan Deshmukh',
      contact_number: '+91 9556677889', location: 'Lonavala Tiger Point Trail (Lat: 18.755, Long: 73.408)',
      active_incidents: 'Phone dropped on steep trail, traveler safe with group',
      resolution_status: 'Dispatched', created_at: '2026-09-02 18:40:00'
    },
    {
      event_id: 'EMG-7002', event_type: 'Lost-phone Event', user_name: 'Vikramaditya Verma',
      contact_number: '+91 9988776655', location: 'Jaisalmer Fort Inner Market',
      active_incidents: 'Phone lost in market, recovered by local police hotline',
      resolution_status: 'Resolved', created_at: '2026-09-01 21:10:00'
    },
    {
      event_id: 'EMG-7003', event_type: 'Emergency Request', user_name: 'Ananya Roy',
      contact_number: '+91 9123456789', location: 'Calangute Beach Road',
      active_incidents: 'First-aid assistance required for minor ankle sprain',
      resolution_status: 'Resolved', created_at: '2026-08-30 16:20:00'
    }
  ];

  for (const s of safetyEvents) {
    await setDoc(doc(db, 'safety_events', s.event_id), s);
  }
  console.log('✅ Safety Events seeded');

  // 9. App Activities
  const activities = [
    {
      app_source: 'Traveler App', action: 'New Booking Request',
      details: 'Aarav Sharma sent request for Solang Paragliding (BKG-5001)',
      timestamp: '2026-09-02 09:30:00'
    },
    {
      app_source: 'Vendor App', action: 'Booking Accepted',
      details: 'Ocean Breeze Watersports accepted booking BKG-5002',
      timestamp: '2026-09-02 10:15:00'
    },
    {
      app_source: 'Leader/Ops App', action: 'Ticket Escalated',
      details: 'Amit Patel assigned Lost-phone ticket TCK-6003 to ground ops',
      timestamp: '2026-09-02 18:45:00'
    },
    {
      app_source: 'Traveler App', action: 'SOS Alert Triggered',
      details: 'Rohan Deshmukh triggered SOS button at Lonavala Trail',
      timestamp: '2026-09-02 18:40:00'
    },
    {
      app_source: 'Vendor App', action: 'KYC Document Upload',
      details: 'Rawat Singh (VND-2003) submitted updated GSTIN proof',
      timestamp: '2026-09-01 15:20:00'
    },
    {
      app_source: 'Traveler App', action: 'Traveler App Profile & Booking Request',
      details: 'Priya Nair joined via Traveler App',
      timestamp: '2026-09-02 16:53:02'
    }
  ];

  for (const a of activities) {
    await addDoc(collection(db, 'app_activities'), a);
  }
  console.log('✅ App Activities seeded');

  console.log('🎉 Firestore seeding complete!');
  return true;
}
