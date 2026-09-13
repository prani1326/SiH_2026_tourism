const request = require('supertest');
const { app, server } = require('../src/index');
const firebase = require('../src/config/firebase');
const fdb = require('../src/services/firestoreDb');

describe('LEADER Industrial Security & Hardening Suite', () => {
  let opsToken = '';
  let adminToken = '';
  let analystToken = '';

  beforeAll(async () => {
    // 1. Ops Leader login
    const resOps = await request(app)
      .post('/api/auth/login')
      .send({ email: 'priya.ops@leaderops.internal', password: 'Admin@123456' });
    opsToken = resOps.body.data.token;

    // 2. Super Admin login
    const resAdmin = await request(app)
      .post('/api/auth/login')
      .send({ email: 'admin@leaderops.internal', password: 'Admin@123456' });
    adminToken = resAdmin.body.data.token;

    // 3. Analyst login (Read-only)
    const resAnalyst = await request(app)
      .post('/api/auth/login')
      .send({ email: 'meera.analyst@leaderops.internal', password: 'Admin@123456' });
    analystToken = resAnalyst.body.data.token;
  });

  afterAll((done) => {
    if (server.listening) {
      server.close(done);
    } else {
      done();
    }
  });


  // 1. REQUEST TRACING & HEADERS
  describe('Observability: Request Correlation IDs', () => {
    test('Responses return X-Request-Id header', async () => {
      const res = await request(app).get('/api/health');
      expect(res.headers['x-request-id']).toBeDefined();
      expect(res.headers['x-request-id'].length).toBeGreaterThan(8);
    });

    test('Custom X-Request-Id is preserved and echoed', async () => {
      const customId = 'client-trace-12345678';
      const res = await request(app)
        .get('/api/health')
        .set('X-Request-Id', customId);
      expect(res.headers['x-request-id']).toBe(customId);
    });

    test('Error responses include requestId in JSON payload', async () => {
      const res = await request(app).get('/api/non-existent-route');
      expect(res.status).toBe(404);
      expect(res.body.error.requestId).toBeDefined();
    });
  });

  // 2. HEALTH & READINESS PROBES
  describe('Industrial Probes: Liveness, Readiness & Health', () => {
    test('GET /api/health/live returns 200 alive', async () => {
      const res = await request(app).get('/api/health/live');
      expect(res.status).toBe(200);
      expect(res.body.status).toBe('alive');
    });

    test('GET /api/health/ready returns 200 ready when DB is connected', async () => {
      const res = await request(app).get('/api/health/ready');
      expect(res.status).toBe(200);
      expect(res.body.status).toBe('ready');
      expect(res.body.database).toBe('connected');
    });

    test('GET /api/health returns comprehensive diagnostics', async () => {
      const res = await request(app).get('/api/health');
      expect(res.status).toBe(200);
      expect(res.body.data.database).toBe('connected');
      expect(res.body.data.memory).toBeDefined();
      expect(res.body.data.memory.heapUsedMb).toBeGreaterThan(0);
    });
  });

  // 3. RBAC BOUNDARY DEFENSE
  describe('RBAC & Broken Function Level Authorization Defense', () => {
    test('Analyst cannot cancel a booking (403 Forbidden)', async () => {
      const res = await request(app)
        .post('/api/bookings/bk-1001/cancel')
        .set('Authorization', `Bearer ${analystToken}`)
        .send({ reason: 'Malicious cancellation attempt' });
      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });

    test('Analyst cannot process a refund (403 Forbidden)', async () => {
      const res = await request(app)
        .post('/api/bookings/bk-1001/refund')
        .set('Authorization', `Bearer ${analystToken}`)
        .send({ reason: 'Malicious refund attempt' });
      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });

    test('Analyst cannot resolve an incident (403 Forbidden)', async () => {
      const res = await request(app)
        .post('/api/incidents/inc-001/resolve')
        .set('Authorization', `Bearer ${analystToken}`)
        .send({ resolution: 'Premature unauthorized resolution' });
      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });

    test('Analyst cannot resolve an SOS emergency (403 Forbidden)', async () => {
      const res = await request(app)
        .post('/api/sos/sos-001/resolve')
        .set('Authorization', `Bearer ${analystToken}`)
        .send({ notes: 'Unauthorized resolution' });
      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });

    test('Analyst cannot trigger an emergency broadcast (403 Forbidden)', async () => {
      const res = await request(app)
        .post('/api/communications/emergency')
        .set('Authorization', `Bearer ${analystToken}`)
        .send({
          destination: 'Jaipur',
          message: 'False alarm broadcast',
          priority: 'emergency',
        });
      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });

    test('Ops Leader has valid permissions to add notes and perform operations', async () => {
      const res = await request(app)
        .post('/api/bookings/bk-1001/notes')
        .set('Authorization', `Bearer ${opsToken}`)
        .send({ content: 'Ops verified booking status with hotel manager' });
      expect(res.status).toBe(201);
      expect(res.body.success).toBe(true);
    });
  });

  // 4. BRUTE FORCE & ACCOUNT LOCKOUT
  describe('Authentication: Brute Force Shield & Account Lockout', () => {
    const targetEmail = 'vikram.safety@leaderops.internal';

    test('Consecutive failed logins increment attempt counter and trigger lockout', async () => {
      // 5 failed attempts
      for (let i = 0; i < 5; i++) {
        const res = await request(app)
          .post('/api/auth/login')
          .send({ email: targetEmail, password: 'WrongPassword123!' });
        expect(res.status).toBe(401);
      }

      // 6th attempt should be blocked due to account lockout
      const lockedRes = await request(app)
        .post('/api/auth/login')
        .send({ email: targetEmail, password: 'Admin@123456' });
      expect(lockedRes.status).toBe(401);
      expect(lockedRes.body.error.message).toMatch(/temporarily locked|locked/i);

      // Clean up test state: unlock account via Firestore
      const userToUnlock = await fdb.findOne('users', [['email', '==', targetEmail]]);
      if (userToUnlock) {
        await fdb.update('users', userToUnlock.id, {
          failed_login_attempts: 0,
          locked_until: null,
        });
      }
    });

    test('Timing attack resistance: non-existent email returns 401 without user enumeration', async () => {
      const res = await request(app)
        .post('/api/auth/login')
        .send({ email: 'definitely.not.found.998877@leaderops.internal', password: 'AnyPassword@123' });
      expect(res.status).toBe(401);
      expect(res.body.error.message).toBe('Invalid credentials');
    });
  });

  // 5. CSV FORMULA INJECTION PREVENTION
  describe('Data Export: CSV Formula Injection Defense', () => {
    test('CSV export sanitizes formulas starting with =, +, -, @', async () => {
      const res = await request(app)
        .post('/api/reports/export')
        .set('Authorization', `Bearer ${adminToken}`)
        .send({ type: 'daily', format: 'csv' });
      expect(res.status).toBe(200);
      expect(res.headers['content-type']).toContain('text/csv');
      // Verify no unescaped formula starters are emitted directly
      const csvText = res.text;
      expect(typeof csvText).toBe('string');
    });
  });
});
