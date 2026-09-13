const express = require('express');
const router = express.Router();
const alertService = require('../services/alertService');
const { authenticate } = require('../middleware/auth');
const { validate } = require('../middleware/validator');
const { assignSchema } = require('../utils/validators');
const { success, paginated } = require('../utils/response');
const { auditLog } = require('../middleware/auditLogger');
const fdb = require('../services/firestoreDb');

router.use(authenticate);

// GET /api/alerts
router.get('/', async (req, res, next) => {
  try {
    const result = await alertService.list(req.query);
    return paginated(res, result.data, result.pagination);
  } catch (err) {
    next(err);
  }
});

// GET /api/alerts/:id
router.get('/:id', async (req, res, next) => {
  try {
    const alert = await alertService.getById(req.params.id);
    return success(res, alert);
  } catch (err) {
    next(err);
  }
});

// POST /api/alerts/:id/acknowledge
router.post('/:id/acknowledge', auditLog('alert', 'acknowledge'), async (req, res, next) => {
  try {
    const result = await alertService.acknowledge(req.params.id, req.user);
    const io = req.app.get('io');
    if (io) io.emit('alert:acknowledged', { alertId: req.params.id, acknowledgedBy: req.user.full_name });
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/alerts/:id/read
router.post('/:id/read', async (req, res, next) => {
  try {
    await fdb.update('alerts', req.params.id, { read: 1, read_at: new Date().toISOString() });
    return success(res, { message: 'Alert marked as read' });
  } catch (err) {
    next(err);
  }
});

// POST /api/alerts/:id/assign
router.post('/:id/assign', validate(assignSchema), auditLog('alert', 'assign'), async (req, res, next) => {
  try {
    const result = await alertService.assign(req.params.id, req.body.ops_leader_id, req.user);
    const io = req.app.get('io');
    if (io) io.emit('alert:assigned', { alertId: req.params.id, assignedTo: result.assigned_ops_id });
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/alerts/:id/resolve
router.post('/:id/resolve', auditLog('alert', 'resolve'), async (req, res, next) => {
  try {
    const result = await alertService.resolve(req.params.id, req.user);
    const io = req.app.get('io');
    if (io) io.emit('alert:resolved', { alertId: req.params.id });
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
