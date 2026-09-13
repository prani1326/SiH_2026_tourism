const express = require('express');
const router = express.Router();
const tripService = require('../services/tripService');
const { authenticate } = require('../middleware/auth');
const { validate } = require('../middleware/validator');
const { assignSchema, noteSchema } = require('../utils/validators');
const { success, paginated, created } = require('../utils/response');
const { auditLog } = require('../middleware/auditLogger');

router.use(authenticate);

// GET /api/trips
router.get('/', async (req, res, next) => {
  try {
    const result = await tripService.list(req.query);
    return paginated(res, result.data, result.pagination);
  } catch (err) {
    next(err);
  }
});

// GET /api/trips/:id
router.get('/:id', async (req, res, next) => {
  try {
    const trip = await tripService.getById(req.params.id);
    return success(res, trip);
  } catch (err) {
    next(err);
  }
});

// GET /api/trips/:id/itinerary
router.get('/:id/itinerary', async (req, res, next) => {
  try {
    const itinerary = await tripService.getItinerary(req.params.id);
    return success(res, itinerary);
  } catch (err) {
    next(err);
  }
});

// GET /api/trips/:id/safety
router.get('/:id/safety', async (req, res, next) => {
  try {
    const safety = await tripService.getSafety(req.params.id);
    return success(res, safety);
  } catch (err) {
    next(err);
  }
});

// GET /api/trips/:id/bookings
router.get('/:id/bookings', async (req, res, next) => {
  try {
    const bookings = await tripService.getBookings(req.params.id);
    return success(res, bookings);
  } catch (err) {
    next(err);
  }
});

// GET /api/trips/:id/messages
router.get('/:id/messages', async (req, res, next) => {
  try {
    const messages = await tripService.getMessages(req.params.id);
    return success(res, messages);
  } catch (err) {
    next(err);
  }
});

// GET /api/trips/:id/activity-log
router.get('/:id/activity-log', async (req, res, next) => {
  try {
    const log = await tripService.getActivityLog(req.params.id);
    return success(res, log);
  } catch (err) {
    next(err);
  }
});

// POST /api/trips/:id/assign
router.post('/:id/assign', validate(assignSchema), auditLog('trip', 'assign'), async (req, res, next) => {
  try {
    const result = await tripService.assign(req.params.id, req.body, req.user);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// GET /api/trips/:id/notes
router.get('/:id/notes', async (req, res, next) => {
  try {
    const notes = await tripService.getNotes(req.params.id);
    return success(res, notes);
  } catch (err) {
    next(err);
  }
});

// POST /api/trips/:id/notes
router.post('/:id/notes', validate(noteSchema), async (req, res, next) => {
  try {
    const result = await tripService.addNote(req.params.id, req.user, req.body.content);
    return created(res, result);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
