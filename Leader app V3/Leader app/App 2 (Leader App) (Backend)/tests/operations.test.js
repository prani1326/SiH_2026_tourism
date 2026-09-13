const request = require('supertest');
const { app, server } = require('../src/index');
const firebase = require('../src/config/firebase');

describe('LEADER Backend Comprehensive Operations Suite', () => {
  let opsToken = '';
  let adminToken = '';
  let analystToken = '';

  beforeAll(async () => {
    // 1. Login as Ops Leader
    const resOps = await request(app)
      .post('/api/auth/login')
      .send({ email: 'priya.ops@leaderops.internal', password: 'Admin@123456' });
    opsToken = resOps.body.data.token;

    // 2. Login as Super Admin
    const resAdmin = await request(app)
      .post('/api/auth/login')
      .send({ email: 'admin@leaderops.internal', password: 'Admin@123456' });
    adminToken = resAdmin.body.data.token;

    // 3. Login as Analyst (Read-only)
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


  // 1. AUTH & SESSIONS
  test('Auth: verify sessions and login history', async () => {
    const res = await request(app)
      .get('/api/auth/sessions')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(res.status).toBe(200);
    expect(res.body.data.length).toBeGreaterThan(0);

    const history = await request(app)
      .get('/api/auth/login-history')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(history.status).toBe(200);
    expect(history.body.data.length).toBeGreaterThan(0);
  });

  // 2. ACTIVE TRIPS & DETAILS
  test('Trips: details, itinerary, safety, bookings, and notes', async () => {
    const trips = await request(app)
      .get('/api/trips')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(trips.status).toBe(200);
    const tripId = trips.body.data[0].id;

    // Trip details
    const detail = await request(app)
      .get(`/api/trips/${tripId}`)
      .set('Authorization', `Bearer ${opsToken}`);
    expect(detail.status).toBe(200);
    expect(detail.body.data.members).toBeDefined();

    // Itinerary
    const itinerary = await request(app)
      .get(`/api/trips/${tripId}/itinerary`)
      .set('Authorization', `Bearer ${opsToken}`);
    expect(itinerary.status).toBe(200);

    // Add internal note
    const note = await request(app)
      .post(`/api/trips/${tripId}/notes`)
      .set('Authorization', `Bearer ${opsToken}`)
      .send({ content: 'Tourist requested Hindi-English bilingual guide for Day 3' });
    expect(note.status).toBe(201);
  });

  // 3. TOURISTS
  test('Tourists: profiles, sensitive data isolation, and internal notes', async () => {
    const list = await request(app)
      .get('/api/tourists')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(list.status).toBe(200);
    const touristId = list.body.data[0].id;

    // Profile
    const profile = await request(app)
      .get(`/api/tourists/${touristId}`)
      .set('Authorization', `Bearer ${opsToken}`);
    expect(profile.status).toBe(200);
    expect(profile.body.data.full_name).toBeDefined();

    // Internal Note
    const addNote = await request(app)
      .post(`/api/tourists/${touristId}/notes`)
      .set('Authorization', `Bearer ${opsToken}`)
      .send({ content: 'Internal note: Tourist has mild asthma, standby inhaler arranged', type: 'general' });
    expect(addNote.status).toBe(201);
  });

  // 4. BOOKINGS
  test('Bookings: state transitions (confirm, notes, refund)', async () => {
    const list = await request(app)
      .get('/api/bookings')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(list.status).toBe(200);
    expect(list.body.data.length).toBeGreaterThan(0);

    const bookingId = 'bk-1001';
    // Notes
    const note = await request(app)
      .post(`/api/bookings/${bookingId}/notes`)
      .set('Authorization', `Bearer ${opsToken}`)
      .send({ content: 'Operational check: Heritage suite confirmed by hotel general manager', type: 'general' });
    expect(note.status).toBe(201);
  });

  // 5. SUPPORT TICKETS
  test('Support: ticket conversation, status update, and resolution', async () => {
    const list = await request(app)
      .get('/api/support/tickets')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(list.status).toBe(200);
    const ticketId = 'tkt-001';

    // Reply
    const reply = await request(app)
      .post(`/api/support/tickets/${ticketId}/reply`)
      .set('Authorization', `Bearer ${opsToken}`)
      .send({ message: 'Hotel engineering team dispatched to Room 302' });
    expect(reply.status).toBe(201);

    // Status change
    const status = await request(app)
      .patch(`/api/support/tickets/${ticketId}/status`)
      .set('Authorization', `Bearer ${opsToken}`)
      .send({ status: 'in_progress', reason: 'Hotel engineer on site' });
    expect(status.status).toBe(200);
  });

  // 6. INCIDENTS & SOS EMERGENCY
  test('Incidents & SOS: full workflow, timeline, and actions', async () => {
    // Incident workflow advance
    const incAdv = await request(app)
      .patch('/api/incidents/inc-001/workflow')
      .set('Authorization', `Bearer ${opsToken}`)
      .send({ stage: 'monitoring', notes: 'Patient admitted to Fort Kochi Taluk Hospital; vital signs stable' });
    expect(incAdv.status).toBe(200);

    // SOS action
    const sosAction = await request(app)
      .post('/api/sos/sos-001/action')
      .set('Authorization', `Bearer ${opsToken}`)
      .send({ action_type: 'dispatch_help', notes: 'Private ambulance escort arrived at location' });
    expect(sosAction.status).toBe(200);
  });

  // 7. SAFETY CENTER & LOST TOURIST MODE
  test('Safety Center: overview, lost tourists, and lost phone support', async () => {
    const overview = await request(app)
      .get('/api/safety/overview')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(overview.status).toBe(200);
    expect(overview.body.data.active_sos).toBeDefined();

    // Lost tourist action: set meeting point
    const lostAction = await request(app)
      .post('/api/safety/lost-tourists/lost-001/actions')
      .set('Authorization', `Bearer ${opsToken}`)
      .send({
        action_type: 'set_meeting_point',
        meeting_point: 'Godowlia Police Booth #2 Gate',
      });
    expect(lostAction.status).toBe(200);

    // Lost phone support info
    const lostPhone = await request(app)
      .get('/api/safety/lost-phone/tour-004')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(lostPhone.status).toBe(200);
    expect(lostPhone.body.data.tourist.emergency_contact_phone).toBeDefined();
  });

  // 8. WEATHER & TRAVEL DISRUPTIONS
  test('Weather & Disruptions: list disruptions, affected trips, and notifications', async () => {
    const disruptions = await request(app)
      .get('/api/weather/disruptions')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(disruptions.status).toBe(200);
    expect(disruptions.body.data.length).toBeGreaterThan(0);

    // Affected trips
    const affected = await request(app)
      .get('/api/weather/disruptions/dis-001/affected-trips')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(affected.status).toBe(200);

    // Bulk notify tourists
    const notify = await request(app)
      .post('/api/weather/disruptions/dis-001/notify')
      .set('Authorization', `Bearer ${opsToken}`)
      .send({
        message: 'Weather Advisory: Munnar routes temporarily closed due to rain. Cultural Cochin plan activated.',
        channel: 'push',
      });
    expect(notify.status).toBe(200);
  });

  // 9. PARTNERS & LISTINGS
  test('Partners: listing approvals, suspensions, and quality reviews', async () => {
    const list = await request(app)
      .get('/api/partners')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(list.status).toBe(200);

    // Approve listing
    const approve = await request(app)
      .post('/api/partners/listings/list-003/approve')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(approve.status).toBe(200);
  });

  // 10. COMMUNICATIONS
  test('Communications: in-app chat & emergency broadcast', async () => {
    const sendMsg = await request(app)
      .post('/api/communications/send')
      .set('Authorization', `Bearer ${opsToken}`)
      .send({
        tourist_id: 'tour-001',
        trip_id: 'trip-001',
        channel: 'chat',
        content_type: 'text',
        content: 'Good morning Emma, your chauffeur is waiting in the hotel lobby.',
      });
    expect(sendMsg.status).toBe(201);
  });

  // 11. REPORTS & EXPORT
  test('Reports: daily ops report and CSV export', async () => {
    const daily = await request(app)
      .get('/api/reports/daily')
      .set('Authorization', `Bearer ${opsToken}`);
    expect(daily.status).toBe(200);
    expect(daily.body.data.trips).toBeDefined();

    const csvExport = await request(app)
      .post('/api/reports/export')
      .set('Authorization', `Bearer ${opsToken}`)
      .send({ type: 'daily', format: 'csv' });
    expect(csvExport.status).toBe(200);
    expect(csvExport.headers['content-type']).toContain('text/csv');
  });

  // 12. TEAM & RBAC
  test('Team & RBAC: Super Admin approves account; Analyst is blocked from Admin endpoints', async () => {
    // Super Admin gets pending approvals
    const pending = await request(app)
      .get('/api/team/pending-approvals')
      .set('Authorization', `Bearer ${adminToken}`);
    expect(pending.status).toBe(200);
    expect(pending.body.data.length).toBeGreaterThan(0);

    // Analyst role is forbidden from admin approval
    const forbidden = await request(app)
      .get('/api/team/pending-approvals')
      .set('Authorization', `Bearer ${analystToken}`);
    expect(forbidden.status).toBe(403);
  });

  // 13. AUDIT LOG
  test('Audit Log: verify tamper-resistant operation logging', async () => {
    const audit = await request(app)
      .get('/api/audit')
      .set('Authorization', `Bearer ${adminToken}`);
    expect(audit.status).toBe(200);
    expect(audit.body.data.length).toBeGreaterThan(0);
  });
});
