const fs = require('fs');
const path = require('path');
const bcrypt = require('bcryptjs');
const { db } = require('../config/firebase');
const { COLLECTIONS } = require('./firestoreSchema');
const logger = require('../utils/logger');

async function seedFirestore() {
  logger.info('Starting Firestore seeding process...');

  const backupDir = path.resolve(__dirname, '../../data/backup_json');
  let hasBackup = fs.existsSync(backupDir);

  if (hasBackup) {
    logger.info('Migrating existing data from JSON backups to Firestore...');
    const tables = [
      'staff_users', 'vendors', 'vendor_kyc', 'vendor_documents',
      'listings', 'travellers', 'bookings', 'trips', 'trip_locations',
      'wallets', 'transactions', 'notifications', 'audit_logs',
      'leaders', 'destinations', 'tickets', 'safety_events',
      'app_activities', 'otp_verifications'
    ];

    for (const table of tables) {
      const filePath = path.join(backupDir, `${table}.json`);
      if (fs.existsSync(filePath)) {
        try {
          const rows = JSON.parse(fs.readFileSync(filePath, 'utf8'));
          const colRef = db.collection(table);
          for (const row of rows) {
            const docId = String(row.id || row.leader_id || row.ticket_id || row.event_id || Math.random().toString(36).substring(2, 10));
            await colRef.doc(docId).set({
              ...row,
              id: isNaN(Number(row.id)) ? row.id : Number(row.id),
            }, { merge: true });
          }
          logger.info(`Migrated ${rows.length} records into Firestore collection '${table}'`);
        } catch (e) {
          logger.warn(`Error migrating table ${table}: ${e.message}`);
        }
      }
    }
  }

  // Ensure default admin and staff exist
  const staffCol = db.collection(COLLECTIONS.STAFF_USERS);
  const salt = bcrypt.genSaltSync(10);
  const adminHash = bcrypt.hashSync('admin123', salt);
  const staffHash = bcrypt.hashSync('staff123', salt);

  const adminSnap = await staffCol.where('email', '==', 'admin@travelcompany.com').get();
  if (adminSnap.empty) {
    await staffCol.doc('1').set({
      id: 1,
      name: 'Head Administrator',
      email: 'admin@travelcompany.com',
      password_hash: adminHash,
      role: 'admin',
      status: 'active',
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    });
    logger.info('Created default Head Administrator in Firestore');
  }

  const staffSnap = await staffCol.where('email', '==', 'staff@travelcompany.com').get();
  if (staffSnap.empty) {
    await staffCol.doc('2').set({
      id: 2,
      name: 'Verification Staff',
      email: 'staff@travelcompany.com',
      password_hash: staffHash,
      role: 'staff',
      status: 'active',
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    });
    logger.info('Created default Verification Staff in Firestore');
  }

  // Ensure demo approved vendor (Rahul Sharma) exists
  const vendorCol = db.collection(COLLECTIONS.VENDORS);
  const rahulSnap = await vendorCol.where('email', '==', 'rahul@himalayanguides.com').get();
  if (rahulSnap.empty) {
    const vendorHash = bcrypt.hashSync('vendor123', salt);
    await vendorCol.doc('27').set({
      id: 27,
      name: 'Rahul Sharma',
      email: 'rahul@himalayanguides.com',
      mobile: '9876543210',
      password_hash: vendorHash,
      profile_photo: '/uploads/profiles/demo-rahul.jpg',
      status: 'active',
      kyc_status: 'approved',
      verification_remarks: 'Documents and credentials verified. Tourism license valid until 2028.',
      verified_at: new Date().toISOString(),
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    });

    await db.collection(COLLECTIONS.WALLETS).doc('27').set({
      id: 27,
      vendor_id: 27,
      balance: 15400.00,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    });

    logger.info('Created demo approved vendor Rahul Sharma in Firestore');
  }

  logger.info('Firestore seeding completed successfully.');
}

if (require.main === module) {
  seedFirestore()
    .then(() => {
      console.log('Seeder finished.');
      process.exit(0);
    })
    .catch((err) => {
      console.error('Seeder failed:', err);
      process.exit(1);
    });
}

module.exports = { seedFirestore };
