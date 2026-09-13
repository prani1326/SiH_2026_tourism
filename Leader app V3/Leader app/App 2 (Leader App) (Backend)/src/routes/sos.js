const express = require('express');
const router = express.Router();
const sosService = require('../services/sosService');
const { authenticate } = require('../middleware/auth');
const { requirePermission } = require('../middleware/rbac');
const { validate } = require('../middleware/validator');
const { sosActionSchema, sosTimelineSchema } = require('../utils/validators');
const { success, paginated, created } = require('../utils/response');
const { auditLog } = require('../middleware/auditLogger');

router.use(authenticate);

// GET /api/sos
router.get('/', async (req, res, next) => {
  try {
    const result = await sosService.list(req.query);
    return paginated(res, result.data, result.pagination);
  } catch (err) {
    next(err);
  }
});

// GET /api/sos/:id
router.get('/:id', async (req, res, next) => {
  try {
    const sos = await sosService.getById(req.params.id);
    return success(res, sos);
  } catch (err) {
    next(err);
  }
});

// POST /api/sos/:id/acknowledge
router.post('/:id/acknowledge', requirePermission('sos:acknowledge'), auditLog('sos', 'acknowledge'), async (req, res, next) => {
  try {
    const result = await sosService.acknowledge(req.params.id, req.user);
    const io = req.app.get('io');
    if (io) io.emit('sos:acknowledged', { sosId: req.params.id, acknowledgedBy: req.user.full_name });
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/sos/:id/dispatch
router.post('/:id/dispatch', requirePermission('sos:action'), auditLog('sos', 'dispatch'), async (req, res, next) => {
  try {
    const result = await sosService.dispatch(req.params.id, req.user, req.body);
    const io = req.app.get('io');
    if (io) io.emit('sos:dispatch', { sosId: req.params.id, responder: result.responder });
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/sos/:id/action
router.post('/:id/action', requirePermission('sos:action'), validate(sosActionSchema), auditLog('sos', 'action'), async (req, res, next) => {
  try {
    const result = await sosService.logAction(req.params.id, req.user, req.body.action_type, req.body.notes);
    const io = req.app.get('io');
    if (io) io.emit('sos:action', { sosId: req.params.id, action: req.body.action_type });
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// GET /api/sos/:id/timeline
router.get('/:id/timeline', async (req, res, next) => {
  try {
    const timeline = await sosService.getTimeline(req.params.id);
    return success(res, timeline);
  } catch (err) {
    next(err);
  }
});

// POST /api/sos/:id/timeline
router.post('/:id/timeline', requirePermission('sos:timeline'), validate(sosTimelineSchema), async (req, res, next) => {
  try {
    const result = await sosService.addTimeline(req.params.id, req.user, req.body.entry, req.body.type);
    return created(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/sos/:id/resolve
router.post('/:id/resolve', requirePermission('sos:resolve'), auditLog('sos', 'resolve'), async (req, res, next) => {
  try {
    const result = await sosService.resolve(req.params.id, req.user, req.body.notes);
    const io = req.app.get('io');
    if (io) io.emit('sos:resolved', { sosId: req.params.id });
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
