require('dotenv').config();
const fs = require('fs');
const path = require('path');
const { db, isRealFirebase } = require('../src/config/firebase');
const logger = require('../src/utils/logger');

async function seedLiveFirestore() {
  const backupPath = path.resolve(__dirname, '..', 'data', 'backup_leader_sqlite.json');
  if (!fs.existsSync(backupPath)) {
    console.error('Backup data file not found at:', backupPath);
    process.exit(1);
  }

  const backupData = JSON.parse(fs.readFileSync(backupPath, 'utf-8'));
  console.log(`Starting Firestore Cloud Seed... Real Firebase: ${isRealFirebase()}`);

  for (const [table, rows] of Object.entries(backupData)) {
    if (!Array.isArray(rows) || rows.length === 0) continue;
    console.log(`Seeding collection '${table}' with ${rows.length} documents...`);

    // Use batches of 400 (Firestore max batch limit is 500)
    const chunkSize = 400;
    for (let i = 0; i < rows.length; i += chunkSize) {
      const chunk = rows.slice(i, i + chunkSize);
      const batch = db.batch();

      for (const row of chunk) {
        const docId = String(row.id || row.user_id || `${table}_${i}`);
        const docRef = db.collection(table).doc(docId);
        batch.set(docRef, { ...row, id: docId }, { merge: true });
      }

      await batch.commit();
    }
  }

  // Also ensure default reference codes exist
  const refBatch = db.batch();
  const seedCodes = ['1326', '123456', '1234', 'OPS2026', 'LEADER2026'];
  for (const code of seedCodes) {
    const refDoc = db.collection('reference_codes').doc(code);
    refBatch.set(refDoc, {
      id: code,
      code: code,
      status: 'active',
      created_at: new Date().toISOString()
    }, { merge: true });
  }
  await refBatch.commit();

  console.log('✅ Firestore Cloud Seed completed successfully!');
  process.exit(0);
}

seedLiveFirestore().catch((err) => {
  console.error('❌ Firestore Cloud Seed failed:', err);
  process.exit(1);
});
