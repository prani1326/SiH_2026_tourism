const express = require('express');
const router = express.Router();
const touristService = require('../services/touristService');
const communicationService = require('../services/communicationService');
const { authenticate } = require('../middleware/auth');
const { validate } = require('../middleware/validator');
const { touristNoteSchema, sendMessageSchema } = require('../utils/validators');
const { success, paginated, created } = require('../utils/response');

router.use(authenticate);

// GET /api/tourists
router.get('/', async (req, res, next) => {
  try {
    const result = await touristService.list(req.query);
    return paginated(res, result.data, result.pagination);
  } catch (err) {
    next(err);
  }
});

// GET /api/tourists/:id
router.get('/:id', async (req, res, next) => {
  try {
    const tourist = await touristService.getById(req.params.id);
    return success(res, tourist);
  } catch (err) {
    next(err);
  }
});

// GET /api/tourists/:id/trips
router.get('/:id/trips', async (req, res, next) => {
  try {
    const trips = await touristService.getTrips(req.params.id);
    return success(res, trips);
  } catch (err) {
    next(err);
  }
});

// GET /api/tourists/:id/bookings
router.get('/:id/bookings', async (req, res, next) => {
  try {
    const bookings = await touristService.getBookings(req.params.id);
    return success(res, bookings);
  } catch (err) {
    next(err);
  }
});

// GET /api/tourists/:id/incidents
router.get('/:id/incidents', async (req, res, next) => {
  try {
    const incidents = await touristService.getIncidents(req.params.id);
    return success(res, incidents);
  } catch (err) {
    next(err);
  }
});

// GET /api/tourists/:id/notes
router.get('/:id/notes', async (req, res, next) => {
  try {
    const notes = await touristService.getNotes(req.params.id);
    return success(res, notes);
  } catch (err) {
    next(err);
  }
});

// POST /api/tourists/:id/notes
router.post('/:id/notes', validate(touristNoteSchema), async (req, res, next) => {
  try {
    const result = await touristService.addNote(req.params.id, req.user, req.body.content, req.body.type);
    return created(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/tourists/:id/message
router.post('/:id/message', validate(sendMessageSchema), async (req, res, next) => {
  try {
    const payload = { ...req.body, tourist_id: req.params.id };
    const result = await communicationService.sendMessage(req.user, payload);
    return created(res, result);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
