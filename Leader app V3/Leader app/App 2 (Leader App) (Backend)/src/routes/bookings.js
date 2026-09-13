const express = require('express');
const router = express.Router();
const bookingService = require('../services/bookingService');
const { authenticate } = require('../middleware/auth');
const { requirePermission } = require('../middleware/rbac');
const { validate } = require('../middleware/validator');
const { bookingActionSchema, noteSchema } = require('../utils/validators');
const { success, paginated, created } = require('../utils/response');
const { auditLog } = require('../middleware/auditLogger');

router.use(authenticate);

// GET /api/bookings
router.get('/', async (req, res, next) => {
  try {
    const result = await bookingService.list(req.query);
    return paginated(res, result.data, result.pagination);
  } catch (err) {
    next(err);
  }
});

// GET /api/bookings/:id
router.get('/:id', async (req, res, next) => {
  try {
    const booking = await bookingService.getById(req.params.id);
    return success(res, booking);
  } catch (err) {
    next(err);
  }
});

// POST /api/bookings/:id/confirm
router.post('/:id/confirm', requirePermission('bookings:confirm'), validate(bookingActionSchema), auditLog('booking', 'confirm'), async (req, res, next) => {
  try {
    const result = await bookingService.confirm(req.params.id, req.user, req.body);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/bookings/:id/cancel
router.post('/:id/cancel', requirePermission('bookings:cancel'), validate(bookingActionSchema), auditLog('booking', 'cancel'), async (req, res, next) => {
  try {
    const result = await bookingService.cancel(req.params.id, req.user, req.body);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/bookings/:id/refund
router.post('/:id/refund', requirePermission('bookings:refund'), validate(bookingActionSchema), auditLog('booking', 'refund'), async (req, res, next) => {
  try {
    const result = await bookingService.processRefund(req.params.id, req.user, req.body);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/bookings/:id/escalate
router.post('/:id/escalate', requirePermission('bookings:escalate'), validate(bookingActionSchema), auditLog('booking', 'escalate'), async (req, res, next) => {
  try {
    const result = await bookingService.escalate(req.params.id, req.user, req.body);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// GET /api/bookings/:id/notes
router.get('/:id/notes', async (req, res, next) => {
  try {
    const notes = await bookingService.getNotes(req.params.id);
    return success(res, notes);
  } catch (err) {
    next(err);
  }
});

// POST /api/bookings/:id/notes
router.post('/:id/notes', requirePermission('bookings:notes'), validate(noteSchema), async (req, res, next) => {
  try {
    const result = await bookingService.addNote(req.params.id, req.user, req.body.content, req.body.type);
    return created(res, result);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
