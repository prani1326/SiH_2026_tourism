import { db } from '../firebase.js';
import {
  collection,
  getDocs,
  addDoc,
  setDoc,
  getDoc,
  updateDoc,
  doc,
  query,
  orderBy,
  limit,
  where
} from 'firebase/firestore';

// Helper: convert Firestore snapshot to array
function snapshotToArray(snapshot) {
  const results = [];
  snapshot.forEach((docSnap) => {
    results.push({ id: docSnap.id, ...docSnap.data() });
  });
  return results;
}

// Helper: fetch from local Backend REST API (SQLite)
async function fetchFromApi(endpoint) {
  try {
    const res = await fetch(`/api/${endpoint}`);
    if (res.ok) {
      const data = await res.json();
      return data;
    }
  } catch (err) {
    console.warn(`[Client API] Backend fetch failed for /api/${endpoint}:`, err.message);
  }
  return null;
}

// Helper: fetch from Firestore safely (with fallback without orderBy if needed)
async function fetchFromFirestore(collectionName, orderField = null) {
  try {
    let q = collection(db, collectionName);
    if (orderField) {
      try {
        q = query(collection(db, collectionName), orderBy(orderField, 'desc'));
      } catch (e) {}
    }
    const snap = await getDocs(q);
    return snapshotToArray(snap);
  } catch (err) {
    console.warn(`[Client API] Firestore fetch failed for ${collectionName}:`, err.message);
    return [];
  }
}

// Helper: Merge API (SQLite) and Firestore records by ID key
function mergeRecords(apiList, firestoreList, idKey) {
  const map = new Map();

  // 1. Add all API items from SQLite database
  if (Array.isArray(apiList)) {
    apiList.forEach(item => {
      if (!item) return;
      const key = String(item[idKey] || item.id || Math.random());
      map.set(key, { ...item });
    });
  }

  // 2. Overlay Firestore items
  if (Array.isArray(firestoreList)) {
    firestoreList.forEach(item => {
      if (!item) return;
      const key = String(item[idKey] || item.id || Math.random());
      if (map.has(key)) {
        map.set(key, { ...map.get(key), ...item });
      } else {
        map.set(key, { ...item });
      }
    });
  }

  return Array.from(map.values());
}

// 1. OVERVIEW
export async function fetchOverview() {
  const apiOverview = await fetchFromApi('overview');
  
  try {
    // Also check Firestore counts to overlay if available
    const [actSnap] = await Promise.all([
      getDocs(query(collection(db, 'app_activities'), orderBy('timestamp', 'desc'), limit(8))).catch(() => null)
    ]);

    const firestoreActivities = actSnap ? snapshotToArray(actSnap) : [];

    if (apiOverview) {
      return {
        ...apiOverview,
        recentActivity: firestoreActivities.length > 0 
          ? mergeRecords(apiOverview.recentActivity || [], firestoreActivities, 'id')
          : (apiOverview.recentActivity || [])
      };
    }
  } catch (err) {
    console.warn('[Client API] Firestore overview fallback error:', err);
  }

  return apiOverview || {
    totalTravelers: 6,
    totalVendors: 4,
    totalLeaders: 3,
    totalTrips: 3,
    totalBookingRequests: 5,
    activeTrips: 1,
    openTickets: 1,
    recentActivity: []
  };
}

// 2. TRAVELERS
export async function fetchTravelers() {
  const [apiData, fsData] = await Promise.all([
    fetchFromApi('travelers'),
    fetchFromFirestore('travelers', 'created_at')
  ]);
  const merged = mergeRecords(apiData, fsData, 'traveler_id');
  return merged.length > 0 ? merged : (apiData || fsData || []);
}

// 3. VENDORS
export async function fetchVendors() {
  const [apiData, fsData] = await Promise.all([
    fetchFromApi('vendors'),
    fetchFromFirestore('vendors', 'created_at')
  ]);
  const merged = mergeRecords(apiData, fsData, 'vendor_id');
  return merged.length > 0 ? merged : (apiData || fsData || []);
}

// 4. LEADERS
export async function fetchLeaders() {
  const [apiData, fsData] = await Promise.all([
    fetchFromApi('leaders'),
    fetchFromFirestore('leaders', 'created_at')
  ]);
  const merged = mergeRecords(apiData, fsData, 'leader_id');
  return merged.length > 0 ? merged : (apiData || fsData || []);
}

// 5. DESTINATIONS
export async function fetchDestinations() {
  const [apiData, fsData] = await Promise.all([
    fetchFromApi('destinations'),
    fetchFromFirestore('destinations', 'created_at')
  ]);
  const merged = mergeRecords(apiData, fsData, 'destination_name');
  return merged.length > 0 ? merged : (apiData || fsData || []);
}

// 6. BOOKINGS
export async function fetchBookings() {
  const [apiData, fsData] = await Promise.all([
    fetchFromApi('bookings'),
    fetchFromFirestore('bookings', 'created_at')
  ]);
  const merged = mergeRecords(apiData, fsData, 'booking_id');
  return merged.length > 0 ? merged : (apiData || fsData || []);
}

// 7. TRIPS
export async function fetchTrips() {
  const [apiData, fsData] = await Promise.all([
    fetchFromApi('trips'),
    fetchFromFirestore('trips', 'created_at')
  ]);
  const merged = mergeRecords(apiData, fsData, 'trip_id');
  return merged.length > 0 ? merged : (apiData || fsData || []);
}

// 8. TICKETS
export async function fetchTickets() {
  const [apiData, fsData] = await Promise.all([
    fetchFromApi('tickets'),
    fetchFromFirestore('tickets', 'timestamp')
  ]);
  const merged = mergeRecords(apiData, fsData, 'ticket_id');
  return merged.length > 0 ? merged : (apiData || fsData || []);
}

// 9. SAFETY EVENTS
export async function fetchSafetyEvents() {
  const [apiData, fsData] = await Promise.all([
    fetchFromApi('safety'),
    fetchFromFirestore('safety_events', 'created_at')
  ]);
  const merged = mergeRecords(apiData, fsData, 'event_id');
  return merged.length > 0 ? merged : (apiData || fsData || []);
}

// 10. ANALYTICS
export async function fetchAnalytics() {
  const apiData = await fetchFromApi('analytics');
  if (apiData) return apiData;

  // Fallback to direct Firestore calculations if API is unavailable
  try {
    const [travelersSnap, vendorsSnap, bookingsSnap, tripsSnap, safetySnap] =
      await Promise.all([
        getDocs(collection(db, 'travelers')),
        getDocs(collection(db, 'vendors')),
        getDocs(collection(db, 'bookings')),
        getDocs(collection(db, 'trips')),
        getDocs(collection(db, 'safety_events'))
      ]);

    const totalTravelers = travelersSnap.size;
    const vendorsArr = snapshotToArray(vendorsSnap);
    const totalVendors = vendorsArr.length;
    const verifiedVendors = vendorsArr.filter(v => v.verification_status === 'Verified').length;
    const pendingVendors = vendorsArr.filter(v => v.verification_status === 'Pending').length;

    const bookingsArr = snapshotToArray(bookingsSnap);
    const totalBookings = bookingsArr.length;
    const completedBookings = bookingsArr.filter(b => b.status === 'Completed').length;
    const acceptedBookings = bookingsArr.filter(b => b.status === 'Accepted').length;
    const pendingBookings = bookingsArr.filter(b => b.status === 'Pending').length;
    const rejectedBookings = bookingsArr.filter(b => b.status === 'Rejected').length;

    const tripsArr = snapshotToArray(tripsSnap);
    const activeTrips = tripsArr.filter(t => t.status === 'Active').length;
    const plannedTrips = tripsArr.filter(t => t.status === 'Planned').length;
    const completedTrips = tripsArr.filter(t => t.status === 'Completed').length;

    const safetyArr = snapshotToArray(safetySnap);
    const totalIncidents = safetyArr.length;
    const activeIncidents = safetyArr.filter(s => s.resolution_status === 'Active' || s.resolution_status === 'Dispatched').length;
    const resolvedIncidents = safetyArr.filter(s => s.resolution_status === 'Resolved').length;

    const destMap = {};
    tripsArr.forEach(t => {
      destMap[t.destination] = (destMap[t.destination] || 0) + 1;
    });
    const popularDestinations = Object.entries(destMap)
      .map(([destination, trip_count]) => ({ destination, trip_count }))
      .sort((a, b) => b.trip_count - a.trip_count);

    const vendorPerformance = vendorsArr.map(v => ({
      business_name: v.business_name,
      booking_requests_count: v.booking_requests_count || 0,
      accepted_bookings_count: v.accepted_bookings_count || 0,
      rejected_bookings_count: v.rejected_bookings_count || 0,
      verification_status: v.verification_status
    })).sort((a, b) => b.accepted_bookings_count - a.accepted_bookings_count);

    return {
      usersGrowth: { totalTravelers, totalVendors },
      vendorsOnboarded: { totalVendors, verifiedVendors, pendingVendors },
      bookingStats: { totalBookings, completedBookings, acceptedBookings, pendingBookings, rejectedBookings },
      tripStats: { activeTrips, plannedTrips, completedTrips },
      incidentsStats: { totalIncidents, activeIncidents, resolvedIncidents },
      popularDestinations,
      vendorPerformance
    };
  } catch (err) {
    console.error('[Client API] Analytics calculation error:', err);
    return null;
  }
}

// 11. TRIP APPROVALS & NOTIFICATIONS
export async function fetchTripApprovals() {
  const allTrips = await fetchTrips();
  return allTrips.map(t => ({
    ...t,
    payment_status: t.payment_status || 'Paid',
    approval_status: t.approval_status || (t.status === 'Active' || t.status === 'Approved' ? 'Approved' : (t.status === 'Completed' ? 'Approved' : 'Pending Approval')),
    amount_paid: t.amount_paid || t.budget || 25000,
    payment_method: t.payment_method || 'UPI / Instant Pay',
    payment_id: t.payment_id || `PAY-${(t.trip_id || 'TRP').replace(/[^0-9]/g, '').padEnd(4, '0')}`
  }));
}

export async function approveTrip(tripId, { leaderId = 'LDR-3001', leaderName = 'Captain Suresh Menon', remarks = 'Verified & Approved by Admin' } = {}) {
  try {
    const tripRef = doc(db, 'trips', tripId);
    const tripSnap = await getDoc(tripRef);
    const tripData = tripSnap.exists() ? tripSnap.data() : { trip_id: tripId, traveler_name: 'Traveler', destination: 'Destination' };

    await updateDoc(tripRef, {
      status: 'Approved',
      approval_status: 'Approved',
      assigned_leader: leaderName,
      leader_id: leaderId,
      admin_remarks: remarks,
      approved_at: new Date().toISOString()
    }).catch(async () => {
      // If doc did not exist in Firestore, setDoc
      await setDoc(tripRef, {
        trip_id: tripId,
        status: 'Approved',
        approval_status: 'Approved',
        assigned_leader: leaderName,
        leader_id: leaderId,
        admin_remarks: remarks,
        approved_at: new Date().toISOString()
      }, { merge: true });
    });

    const now = new Date().toISOString();

    // 1. Notification for Tourist / Traveler App
    const touristNotification = {
      recipient_type: 'tourist',
      traveler_name: tripData.traveler_name || 'Traveler',
      trip_id: tripId,
      title: 'Trip Approved! 🎉 Pack Your Bags!',
      message: `Great news ${tripData.traveler_name}! Your trip to ${tripData.destination} has received central admin approval. Assigned Trip Lead: ${leaderName}.`,
      type: 'approval_success',
      timestamp: now,
      status: 'unread'
    };
    await addDoc(collection(db, 'notifications'), touristNotification);

    // 2. Notification for Leader App
    const leaderNotification = {
      recipient_type: 'leader',
      leader_name: leaderName,
      trip_id: tripId,
      title: 'New Trip Assigned & Approved 🛡️',
      message: `Trip ${tripId} (${tripData.destination}) for ${tripData.traveler_name} has been approved. You are assigned as ground lead.`,
      type: 'leader_assignment',
      timestamp: now,
      status: 'unread'
    };
    await addDoc(collection(db, 'notifications'), leaderNotification);

    // 3. Central live activity stream
    await addDoc(collection(db, 'app_activities'), {
      app_source: 'Web Dashboard',
      action: 'Trip Approved',
      details: `Admin approved trip ${tripId} (${tripData.destination}) for ${tripData.traveler_name}. Assigned lead: ${leaderName}`,
      timestamp: now
    });

    return { success: true, touristNotification, leaderNotification };
  } catch (err) {
    console.error('approveTrip error:', err);
    throw err;
  }
}

export async function rejectTrip(tripId, { reason = 'Capacity Full / Adverse Weather Advisory' } = {}) {
  try {
    const tripRef = doc(db, 'trips', tripId);
    const tripSnap = await getDoc(tripRef);
    const tripData = tripSnap.exists() ? tripSnap.data() : { trip_id: tripId, traveler_name: 'Traveler', destination: 'Destination' };

    await updateDoc(tripRef, {
      status: 'Rejected',
      approval_status: 'Rejected',
      rejection_reason: reason,
      rejected_at: new Date().toISOString()
    }).catch(async () => {
      await setDoc(tripRef, {
        trip_id: tripId,
        status: 'Rejected',
        approval_status: 'Rejected',
        rejection_reason: reason,
        rejected_at: new Date().toISOString()
      }, { merge: true });
    });

    const now = new Date().toISOString();

    // 1. Notification for Tourist App
    const touristNotification = {
      recipient_type: 'tourist',
      traveler_name: tripData.traveler_name || 'Traveler',
      trip_id: tripId,
      title: 'Trip Booking Rejected ⚠️',
      message: `Hello ${tripData.traveler_name}, your trip ${tripId} to ${tripData.destination} could not be approved. Reason: ${reason}. Full refund of ₹${tripData.budget || 25000} has been initiated to your source account.`,
      type: 'rejection',
      timestamp: now,
      status: 'unread'
    };
    await addDoc(collection(db, 'notifications'), touristNotification);

    // 2. Notification for Leader App
    const leaderNotification = {
      recipient_type: 'leader',
      leader_name: 'All Leads',
      trip_id: tripId,
      title: 'Trip Rejected by Admin ❌',
      message: `Trip ${tripId} for ${tripData.traveler_name} to ${tripData.destination} was rejected. Reason: ${reason}.`,
      type: 'trip_rejected',
      timestamp: now,
      status: 'unread'
    };
    await addDoc(collection(db, 'notifications'), leaderNotification);

    // 3. Central live activity stream
    await addDoc(collection(db, 'app_activities'), {
      app_source: 'Web Dashboard',
      action: 'Trip Rejected',
      details: `Admin rejected trip ${tripId} for ${tripData.traveler_name}. Reason: ${reason}. Refund initiated.`,
      timestamp: now
    });

    return { success: true };
  } catch (err) {
    console.error('rejectTrip error:', err);
    throw err;
  }
}

export async function fetchNotifications(recipientType = null) {
  try {
    const q = query(
      collection(db, 'notifications'),
      orderBy('timestamp', 'desc'),
      limit(50)
    );
    const snap = await getDocs(q);
    const all = snapshotToArray(snap);
    if (recipientType) {
      return all.filter(n => n.recipient_type === recipientType);
    }
    return all;
  } catch (err) {
    console.warn('[Client API] fetchNotifications error:', err);
    return [];
  }
}

export async function createPaidTripRequest(data) {
  const trip_id = `TRP-${Date.now().toString().slice(-4)}`;
  const now = new Date().toISOString();
  const tripDoc = {
    trip_id,
    traveler_name: data.traveler_name || 'Rahul Varma',
    destination: data.destination || 'Manali & Solang Valley',
    start_date: data.start_date || '2026-09-15',
    end_date: data.end_date || '2026-09-20',
    number_of_travelers: Number(data.number_of_travelers) || 2,
    budget: Number(data.budget) || 35000,
    amount_paid: Number(data.amount_paid || data.budget) || 35000,
    payment_status: 'Paid',
    payment_id: data.payment_id || `PAY-${Date.now().toString().slice(-6)}`,
    payment_method: data.payment_method || 'UPI / Instant NetBanking',
    itinerary: data.itinerary || 'Day 1: Arrival & Check-in, Day 2: Sightseeing, Day 3: Adventure Sports',
    status: 'Pending Approval',
    approval_status: 'Pending Approval',
    created_at: now
  };

  await setDoc(doc(db, 'trips', trip_id), tripDoc);

  // Log in activities
  await addDoc(collection(db, 'app_activities'), {
    app_source: 'Traveler App',
    action: 'New Paid Trip Submitted',
    details: `${tripDoc.traveler_name} paid ₹${tripDoc.amount_paid} for ${tripDoc.destination}. Awaiting Admin Approval.`,
    timestamp: now
  });

  // Tourist gets instant notification of payment received & pending approval
  await addDoc(collection(db, 'notifications'), {
    recipient_type: 'tourist',
    traveler_name: tripDoc.traveler_name,
    trip_id,
    title: 'Payment Successful! Awaiting Approval ⏳',
    message: `Payment of ₹${tripDoc.amount_paid} received for ${tripDoc.destination}. Your trip has been sent to operations team for verification.`,
    type: 'payment_success',
    timestamp: now,
    status: 'unread'
  });

  return { success: true, trip_id, trip: tripDoc };
}

// APP SIMULATOR — Ingest data into both SQLite API and Firestore
export async function simulateAppIngest(appType, payload) {
  if (appType === 'traveler') {
    const traveler_id = `TRV-${Date.now().toString().slice(-4)}`;
    
    // Ingest into Firestore
    await setDoc(doc(db, 'travelers', traveler_id), {
      traveler_id,
      name: payload.name || 'New Traveler',
      email: payload.email || 'traveler@app.com',
      phone: payload.phone || '+91 9000000000',
      profile_info: payload.profile_info || 'App Registered Traveler',
      emergency_info: payload.emergency_info || 'None',
      trip_status: 'Active',
      trips_count: 0,
      itinerary: '',
      booking_requests_count: 0,
      created_at: new Date().toISOString()
    });

    await addDoc(collection(db, 'app_activities'), {
      app_source: 'Traveler App',
      action: payload.action || 'New Traveler Registration',
      details: `${payload.name || traveler_id} joined via Traveler App`,
      timestamp: new Date().toISOString()
    });

    // Also forward to backend API if running
    try {
      await fetch('/api/ingest/traveler', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
    } catch (e) {}

    return { success: true, traveler_id, message: 'Data collected into Firestore & Central Database' };
  }

  if (appType === 'vendor') {
    const vendor_id = `VND-${Date.now().toString().slice(-4)}`;
    await setDoc(doc(db, 'vendors', vendor_id), {
      vendor_id,
      business_name: payload.business_name || 'New Vendor Business',
      owner_contact: payload.owner_contact || '+91 9111111111',
      kyc_info: payload.kyc_info || 'Submitted via App',
      verification_status: 'Pending',
      listings: payload.listings || 'Services',
      vendor_activity: payload.action || 'Onboarded via Vendor App',
      booking_requests_count: 0,
      accepted_bookings_count: 0,
      rejected_bookings_count: 0,
      created_at: new Date().toISOString()
    });

    await addDoc(collection(db, 'app_activities'), {
      app_source: 'Vendor App',
      action: payload.action || 'Vendor Onboarding',
      details: `${payload.business_name || vendor_id} submitted KYC & listings`,
      timestamp: new Date().toISOString()
    });

    try {
      await fetch('/api/ingest/vendor', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
    } catch (e) {}

    return { success: true, vendor_id, message: 'Vendor Data collected into Firestore & Central Database' };
  }

  if (appType === 'leader') {
    await addDoc(collection(db, 'app_activities'), {
      app_source: 'Leader/Ops App',
      action: payload.action || 'Ops Update',
      details: `${payload.leader_name || 'Leader'}: ${payload.issue_resolved || payload.incident_created || 'Logged operation detail'}`,
      timestamp: new Date().toISOString()
    });

    try {
      await fetch('/api/ingest/leader', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
    } catch (e) {}

    return { success: true, message: 'Leader/Ops Data logged into Firestore & Central Database' };
  }

  throw new Error(`Unknown app type: ${appType}`);
}
