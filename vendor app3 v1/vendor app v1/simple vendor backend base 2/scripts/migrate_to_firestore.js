/**
 * Standalone Migration Script: SQLite Backup to Firestore
 * App 3 — Vendor App
 */

const { seedFirestore } = require('../src/database/firestoreSeeder');

async function run() {
  console.log('--- Starting Migration from Backup to Firebase Firestore ---');
  await seedFirestore();
  console.log('--- Migration to Firebase Firestore Complete! ---');
}

run()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error('Migration error:', err);
    process.exit(1);
  });
