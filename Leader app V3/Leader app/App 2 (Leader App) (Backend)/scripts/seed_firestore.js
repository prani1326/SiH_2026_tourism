const path = require('path');
const fs = require('fs');
const { db, isRealFirebase } = require('../src/config/firebase');

async function seedFirestore() {
  console.log('Seeding data to Firestore...');
  const backupPath = path.resolve(__dirname, '..', 'data', 'backup_leader_sqlite.json');
  if (!fs.existsSync(backupPath)) {
    console.error('Error: backup_leader_sqlite.json not found!');
    process.exit(1);
  }

  const backupData = JSON.parse(fs.readFileSync(backupPath, 'utf-8'));
  let totalSeeded = 0;

  for (const [collectionName, records] of Object.entries(backupData)) {
    console.log(`Processing collection: ${collectionName} (${records.length} records)...`);
    const batch = db.batch();
    let countInBatch = 0;

    for (const record of records) {
      const docId = String(record.id || record.user_id || `${collectionName}_${Date.now()}_${Math.random()}`);
      const docRef = db.collection(collectionName).doc(docId);
      batch.set(docRef, { ...record, id: docId }, { merge: true });
      countInBatch++;
      totalSeeded++;

      if (countInBatch >= 450) {
        await batch.commit();
        countInBatch = 0;
      }
    }

    if (countInBatch > 0) {
      await batch.commit();
    }
    console.log(`Seeded ${records.length} documents into '${collectionName}'`);
  }

  console.log(`Firestore seeding complete! Total documents seeded: ${totalSeeded} (Real Firebase: ${isRealFirebase()})`);
}

if (require.main === module) {
  seedFirestore()
    .then(() => process.exit(0))
    .catch((err) => {
      console.error('Seeding error:', err);
      process.exit(1);
    });
}

module.exports = seedFirestore;
