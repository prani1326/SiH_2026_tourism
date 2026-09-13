const { getDb, queryAll } = require('./src/db/index');
const { initializeApp } = require('firebase/app');
const { getFirestore, setDoc, doc, addDoc, collection } = require('firebase/firestore');

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

async function migrateAll() {
  console.log('🚀 Starting Full Migration from SQLite to Cloud Firestore...');
  await getDb();

  // 1. Travelers
  const travelers = queryAll('SELECT * FROM travelers');
  console.log(`Found ${travelers.length} travelers in SQLite`);
  for (const t of travelers) {
    const docId = t.traveler_id || `TRV-${t.id}`;
    await setDoc(doc(db, 'travelers', docId), t);
  }
  console.log('✅ Travelers migrated');

  // 2. Vendors
  const vendors = queryAll('SELECT * FROM vendors');
  console.log(`Found ${vendors.length} vendors in SQLite`);
  for (const v of vendors) {
    const docId = v.vendor_id || `VND-${v.id}`;
    await setDoc(doc(db, 'vendors', docId), v);
  }
  console.log('✅ Vendors migrated');

  // 3. Leaders
  const leaders = queryAll('SELECT * FROM leaders');
  console.log(`Found ${leaders.length} leaders in SQLite`);
  for (const l of leaders) {
    const docId = l.leader_id || `LDR-${l.id}`;
    await setDoc(doc(db, 'leaders', docId), l);
  }
  console.log('✅ Leaders migrated');

  // 4. Destinations
  const destinations = queryAll('SELECT * FROM destinations');
  console.log(`Found ${destinations.length} destinations in SQLite`);
  for (const d of destinations) {
    const docId = (d.destination_name || `DEST-${d.id}`).replace(/[\/\s&()]+/g, '_');
    await setDoc(doc(db, 'destinations', docId), d);
  }
  console.log('✅ Destinations migrated');

  // 5. Trips
  const trips = queryAll('SELECT * FROM trips');
  console.log(`Found ${trips.length} trips in SQLite`);
  for (const tr of trips) {
    const docId = tr.trip_id || `TRP-${tr.id}`;
    await setDoc(doc(db, 'trips', docId), tr);
  }
  console.log('✅ Trips migrated');

  // 6. Bookings
  const bookings = queryAll('SELECT * FROM bookings');
  console.log(`Found ${bookings.length} bookings in SQLite`);
  for (const b of bookings) {
    const docId = b.booking_id || `BKG-${b.id}`;
    await setDoc(doc(db, 'bookings', docId), b);
  }
  console.log('✅ Bookings migrated');

  // 7. Tickets
  const tickets = queryAll('SELECT * FROM tickets');
  console.log(`Found ${tickets.length} tickets in SQLite`);
  for (const tk of tickets) {
    const docId = tk.ticket_id || `TCK-${tk.id}`;
    await setDoc(doc(db, 'tickets', docId), tk);
  }
  console.log('✅ Tickets migrated');

  // 8. Safety Events
  const safetyEvents = queryAll('SELECT * FROM safety_events');
  console.log(`Found ${safetyEvents.length} safety_events in SQLite`);
  for (const s of safetyEvents) {
    const docId = s.event_id || `EMG-${s.id}`;
    await setDoc(doc(db, 'safety_events', docId), s);
  }
  console.log('✅ Safety Events migrated');

  // 9. App Activities
  const activities = queryAll('SELECT * FROM app_activities');
  console.log(`Found ${activities.length} app_activities in SQLite`);
  for (const a of activities) {
    await addDoc(collection(db, 'app_activities'), a);
  }
  console.log('✅ App Activities migrated');

  console.log('🎉🎉 COMPLETE DATABASE MIGRATION SUCCESSFUL! All data is in Cloud Firestore!');
  process.exit(0);
}

migrateAll().catch(err => {
  console.error('Migration Error:', err);
  process.exit(1);
});
