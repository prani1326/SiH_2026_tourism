const request = require('supertest');
const { app, server } = require('../src/index');
const firebase = require('../src/config/firebase');

describe('LEADER Backend API Tests', () => {
  let adminToken = '';
  let opsToken = '';

  beforeAll(async () => {
    // Database and tables should be ready
  });

  afterAll((done) => {
    if (server.listening) {
      server.close(done);
    } else {
      done();
    }
  });


  test('GET /api/health - returns healthy status', async () => {
    const res = await request(app).get('/api/health');
    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.status).toBe('healthy');
  });

  test('POST /api/auth/login - Super Admin login', async () => {
    const res = await request(app)
      .post('/api/auth/login')
      .send({
        email: 'admin@leaderops.internal',
        password: 'Admin@123456',
      });
    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.token).toBeDefined();
    expect(res.body.data.user.role).toBe('super_admin');
    adminToken = res.body.data.token;
  });

  test('POST /api/auth/login - Ops Leader login', async () => {
    const res = await request(app)
      .post('/api/auth/login')
      .send({
        email: 'priya.ops@leaderops.internal',
        password: 'Admin@123456',
      });
    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.token).toBeDefined();
    expect(res.body.data.user.role).toBe('ops_leader');
    opsToken = res.body.data.token;
  });

  test('GET /api/dashboard/stats - authenticated dashboard stats', async () => {
    const res = await request(app)
      .get('/api/dashboard/stats')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.tourists).toBeDefined();
    expect(res.body.data.safety).toBeDefined();
  });

  test('GET /api/alerts - list alerts priority sorted', async () => {
    const res = await request(app)
      .get('/api/alerts')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(Array.isArray(res.body.data)).toBe(true);
  });

  test('GET /api/trips - list active trips', async () => {
    const res = await request(app)
      .get('/api/trips')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.length).toBeGreaterThan(0);
  });

  test('GET /api/sos - retrieve active SOS cases', async () => {
    const res = await request(app)
      .get('/api/sos')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
  });

  test('GET /api/search - global ops search', async () => {
    const res = await request(app)
      .get('/api/search?q=Jaipur')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.results.length).toBeGreaterThan(0);
  });
});
