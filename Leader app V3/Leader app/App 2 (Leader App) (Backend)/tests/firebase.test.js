const request = require('supertest');
const { app, server } = require('../src/index');
const firebase = require('../src/config/firebase');
const fdb = require('../src/services/firestoreDb');
const fcmService = require('../src/services/fcmService');
const storageService = require('../src/services/storageService');

describe('App 2 Firebase Suite (Auth, Firestore, Storage, FCM)', () => {
  let opsToken = '';
  let adminToken = '';
  let testUserId = 'test-ops-user-1';

  beforeAll(async () => {
    // 1. Create a test user in Firestore if not exists
    await fdb.set('users', testUserId, {
      id: testUserId,
      email: 'firebase.tester@leaderops.internal',
      full_name: 'Firebase Test Leader',
      role: 'ops_leader',
      status: 'active',
      is_active: 1,
      assigned_region: 'Jaipur',
      created_at: new Date().toISOString(),
    });

    // 2. Login to get token
    const resOps = await request(app)
      .post('/api/auth/login')
      .send({ email: 'priya.ops@leaderops.internal', password: 'Admin@123456' });
    opsToken = resOps.body.data.token;

    const resAdmin = await request(app)
      .post('/api/auth/login')
      .send({ email: 'admin@leaderops.internal', password: 'Admin@123456' });
    adminToken = resAdmin.body.data.token;
  });

  afterAll((done) => {
    if (server.listening) {
      server.close(done);
    } else {
      done();
    }
  });

  describe('1. Firestore Core Operations', () => {
    test('Firestore: Create, Read, Update, Delete in Ops collections', async () => {
      const collectionName = 'operations';
      const docId = `op_${Date.now()}`;
      const payload = {
        id: docId,
        title: 'Emergency Medical Evac Ops',
        status: 'active',
        priority: 'high',
        assigned_leader_id: testUserId,
        created_at: new Date().toISOString(),
      };

      // Create
      const created = await fdb.set(collectionName, docId, payload);
      expect(created).toBeDefined();

      // Read
      const fetched = await fdb.findById(collectionName, docId);
      expect(fetched).toBeDefined();
      expect(fetched.title).toBe('Emergency Medical Evac Ops');

      // Update
      await fdb.update(collectionName, docId, { status: 'completed' });
      const updated = await fdb.findById(collectionName, docId);
      expect(updated.status).toBe('completed');

      // Query
      const queryResults = await fdb.find(collectionName, [['priority', '==', 'high']]);
      expect(queryResults.length).toBeGreaterThan(0);

      // Delete
      await fdb.delete(collectionName, docId);
      const afterDelete = await fdb.findById(collectionName, docId);
      expect(afterDelete).toBeNull();
    });

    test('Firestore: Batch write operations work', async () => {
      const batch = fdb.batch();
      const doc1 = firebase.db.collection('tasks').doc('batch_task_1');
      const doc2 = firebase.db.collection('tasks').doc('batch_task_2');

      batch.set(doc1, { title: 'Task 1', status: 'pending' });
      batch.set(doc2, { title: 'Task 2', status: 'done' });
      await batch.commit();

      const task1 = await fdb.findById('tasks', 'batch_task_1');
      const task2 = await fdb.findById('tasks', 'batch_task_2');
      expect(task1.title).toBe('Task 1');
      expect(task2.title).toBe('Task 2');

      // Clean up
      await fdb.delete('tasks', 'batch_task_1');
      await fdb.delete('tasks', 'batch_task_2');
    });
  });

  describe('2. Firebase Authentication & RBAC', () => {
    test('Supports Firebase Bearer tokens in Authorization header', async () => {
      // Valid ops token passes
      const res = await request(app)
        .get('/api/dashboard/stats')
        .set('Authorization', `Bearer ${opsToken}`);
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
    });

    test('Rejects request without Authorization header', async () => {
      const res = await request(app).get('/api/dashboard/stats');
      expect(res.status).toBe(401);
      expect(res.body.success).toBe(false);
      expect(res.body.error.code).toBe('UNAUTHORIZED');
    });

    test('Rejects invalid token format', async () => {
      const res = await request(app)
        .get('/api/dashboard/stats')
        .set('Authorization', 'Bearer totally-invalid-token');
      expect(res.status).toBe(401);
      expect(res.body.success).toBe(false);
    });

    test('RBAC: Ops Leader cannot access Super Admin endpoints', async () => {
      const res = await request(app)
        .get('/api/team/pending-approvals')
        .set('Authorization', `Bearer ${opsToken}`);
      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });

    test('RBAC: Super Admin can access admin endpoints', async () => {
      const res = await request(app)
        .get('/api/team/pending-approvals')
        .set('Authorization', `Bearer ${adminToken}`);
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
    });
  });

  describe('3. Firebase Storage File Uploads', () => {
    test('POST /api/storage/upload - uploads file and returns reference URL', async () => {
      const res = await request(app)
        .post('/api/storage/upload')
        .set('Authorization', `Bearer ${opsToken}`)
        .field('folder', 'incident-evidence')
        .attach('file', Buffer.from('Mock file contents for incident evidence report'), 'evidence.txt');

      expect(res.status).toBe(201);
      expect(res.body.success).toBe(true);
      expect(res.body.data.url).toBeDefined();
      expect(res.body.data.storage_path).toContain('incident-evidence');
      expect(res.body.data.filename).toBe('evidence.txt');

      // Verify reference was saved in Firestore storage_files
      const fileRef = await fdb.findById('storage_files', res.body.data.id);
      expect(fileRef).toBeDefined();
      expect(fileRef.original_name).toBe('evidence.txt');
    });

    test('Storage upload rejects missing file', async () => {
      const res = await request(app)
        .post('/api/storage/upload')
        .set('Authorization', `Bearer ${opsToken}`);
      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('BAD_REQUEST');
    });
  });

  describe('4. Firebase Cloud Messaging (FCM)', () => {
    test('POST /api/notifications/fcm-token - registers device token in Firestore', async () => {
      const res = await request(app)
        .post('/api/notifications/fcm-token')
        .set('Authorization', `Bearer ${opsToken}`)
        .send({
          token: 'fcm_mock_device_token_ops_leader_android_12345',
          device_info: { model: 'Pixel 8 Pro', os: 'Android 14' },
        });

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data.message).toBe('FCM device token registered');

      // Verify token in Firestore
      const tokens = await fdb.find('fcm_tokens', [['fcm_token', '==', 'fcm_mock_device_token_ops_leader_android_12345']]);
      expect(tokens.length).toBeGreaterThan(0);
    });

    test('fcmService.sendToUser - handles notification dispatch', async () => {
      const result = await fcmService.sendToUser(testUserId, 'Test Alert', 'Operations alert message', { alertId: 'alert-01' });
      expect(result).toBeDefined();
    });

    test('fcmService.sendToTopic - sends topic broadcast', async () => {
      const result = await fcmService.sendToTopic('ops_broadcasts', 'Broadcast', 'Emergency Ops Notice');
      expect(result).toBeDefined();
    });
  });

  describe('5. Ops Leader Android Kotlin Endpoints', () => {
    test('GET /api/dashboard/kpis - returns KPI metrics', async () => {
      const res = await request(app)
        .get('/api/dashboard/kpis')
        .set('Authorization', `Bearer ${opsToken}`);
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data.total_active_trips).toBeDefined();
    });

    test('POST /api/alerts/:id/read - marks alert as read', async () => {
      const alerts = await fdb.find('ops_alerts', [], { limit: 1 });
      if (alerts.length > 0) {
        const res = await request(app)
          .post(`/api/alerts/${alerts[0].id}/read`)
          .set('Authorization', `Bearer ${opsToken}`);
        expect(res.status).toBe(200);
        expect(res.body.success).toBe(true);
      }
    });

    test('PUT /api/incidents/:id/status - updates incident status', async () => {
      const incidents = await fdb.find('incidents', [], { limit: 1 });
      if (incidents.length > 0) {
        const res = await request(app)
          .put(`/api/incidents/${incidents[0].id}/status`)
          .set('Authorization', `Bearer ${opsToken}`)
          .send({ status: 'monitoring', reason: 'Under observation by Ops' });
        expect(res.status).toBe(200);
        expect(res.body.success).toBe(true);
      }
    });

    test('POST /api/sos/:id/dispatch - dispatches responder to SOS', async () => {
      const sosCases = await fdb.find('sos_cases', [], { limit: 1 });
      if (sosCases.length > 0) {
        const res = await request(app)
          .post(`/api/sos/${sosCases[0].id}/dispatch`)
          .set('Authorization', `Bearer ${opsToken}`)
          .send({ responder_name: 'Jaipur Quick Response Unit 1', responder_id: 'qru-01' });
        expect(res.status).toBe(200);
        expect(res.body.success).toBe(true);
      }
    });
  });
});
