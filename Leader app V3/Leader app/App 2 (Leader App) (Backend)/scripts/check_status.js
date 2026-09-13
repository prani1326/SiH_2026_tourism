require('dotenv').config();
const { db, isRealFirebase, ping } = require('../src/config/firebase');

async function test() {
  console.log('--- Firebase Status Check ---');
  console.log('Project ID:', process.env.FIREBASE_PROJECT_ID);
  console.log('Key file exists:', require('fs').existsSync(process.env.FIREBASE_SERVICE_ACCOUNT_KEY_PATH));
  console.log('isRealFirebase:', isRealFirebase());
  const ok = await ping();
  console.log('Ping result:', ok);
  
  // List collections to verify access
  if (isRealFirebase()) {
    try {
      const collections = await db.listCollections();
      console.log('Collections in Firestore:', collections.map(c => c.id));
    } catch (e) {
      console.error('Error listing collections:', e.message);
    }
  }
  process.exit(0);
}

test();
