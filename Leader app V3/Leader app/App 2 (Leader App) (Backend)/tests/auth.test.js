const request = require('supertest');
const { app, server } = require('../src/index');

describe('Auth & Signup API Contract Tests', () => {
  afterAll((done) => {
    if (server && server.listening) {
      server.close(done);
    } else {
      done();
    }
  });

  describe('POST /api/auth/signup', () => {
    test('rejects signup with invalid referenceId (returns 400 & REFERENCE_ID_INVALID)', async () => {
      const res = await request(app)
        .post('/api/auth/signup')
        .send({
          name: 'Jane Doe',
          email: `test_invalid_ref_${Date.now()}@example.com`,
          password: 'SecurePassword123!',
          referenceId: '999999',
          role: 'OPS_LEADER',
        });

      expect(res.status).toBe(400);
      expect(res.body.success).toBe(false);
      expect(res.body.errorCode).toBe('REFERENCE_ID_INVALID');
      expect(res.body.message).toMatch(/reference ID provided is invalid/i);
    });

    test('accepts signup with valid seed referenceId "1326" and activates user account', async () => {
      const uniqueEmail = `jane_ops_${Date.now()}@example.com`;
      const res = await request(app)
        .post('/api/auth/signup')
        .send({
          name: 'Jane Doe',
          email: uniqueEmail,
          password: 'SecurePassword123!',
          referenceId: '1326',
          role: 'OPS_LEADER',
        });

      expect(res.status).toBe(201);
      expect(res.body.success).toBe(true);
      expect(res.body.user).toBeDefined();
      expect(res.body.user.email).toBe(uniqueEmail);
      expect(res.body.token).toBeDefined();
    });

    test('accepts signup update if account already exists with valid referenceId', async () => {
      const existingEmail = `dup_${Date.now()}@example.com`;
      // First signup
      await request(app)
        .post('/api/auth/signup')
        .send({
          name: 'First User',
          email: existingEmail,
          password: 'SecurePassword123!',
          referenceId: '1326',
        });

      // Second signup with same email updates/upgrades account
      const res = await request(app)
        .post('/api/auth/signup')
        .send({
          name: 'Second User',
          email: existingEmail,
          password: 'SecurePassword123!',
          referenceId: '1326',
        });

      expect(res.status).toBe(201);
      expect(res.body.success).toBe(true);
      expect(res.body.token).toBeDefined();
    });
  });

  describe('POST /api/auth/google', () => {
    test('returns NEW_USER when google user not found in database', async () => {
      const res = await request(app)
        .post('/api/auth/google')
        .send({
          idToken: 'mock-new-unknown-user-token',
        });

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(false);
      expect(res.body.errorCode).toBe('NEW_USER');
    });

    test('successfully signs in existing registered user', async () => {
      const res = await request(app)
        .post('/api/auth/google')
        .send({
          idToken: 'mock-google-token',
        });

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.token).toBeDefined();
      expect(res.body.user).toBeDefined();
    });
  });
});
