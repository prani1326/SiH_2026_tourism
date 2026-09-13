const express = require('express');
const router = express.Router();
const safetyService = require('../services/safetyService');
const { authenticate } = require('../middleware/auth');
const { requirePermission } = require('../middleware/rbac');
const { validate } = require('../middleware/validator');
const { lostTouristActionSchema, lostPhoneActionSchema } = require('../utils/validators');
const { success, paginated } = require('../utils/response');
const { auditLog } = require('../middleware/auditLogger');

router.use(authenticate);

// GET /api/safety/overview
router.get('/overview', async (req, res, next) => {
  try {
    const overview = await safetyService.getOverview();
    return success(res, overview);
  } catch (err) {
    next(err);
  }
});

// GET /api/safety/alerts
router.get('/alerts', async (req, res, next) => {
  try {
    const alerts = await safetyService.getAlerts();
    return success(res, alerts);
  } catch (err) {
    next(err);
  }
});

// GET /api/safety/checkins
router.get('/checkins', async (req, res, next) => {
  try {
    const result = await safetyService.getCheckins(req.query);
    return paginated(res, result.data, result.pagination);
  } catch (err) {
    next(err);
  }
});

// GET /api/safety/lost-tourists
router.get('/lost-tourists', async (req, res, next) => {
  try {
    const lostTourists = await safetyService.getLostTourists();
    return success(res, lostTourists);
  } catch (err) {
    next(err);
  }
});

// GET /api/safety/lost-tourists/:id
router.get('/lost-tourists/:id', async (req, res, next) => {
  try {
    const lost = await safetyService.getLostTouristById(req.params.id);
    return success(res, lost);
  } catch (err) {
    next(err);
  }
});

// POST /api/safety/lost-tourists/:id/actions
router.post('/lost-tourists/:id/actions', requirePermission('safety:action'), validate(lostTouristActionSchema), auditLog('lost_tourist', 'action'), async (req, res, next) => {
  try {
    const result = await safetyService.lostTouristAction(req.params.id, req.user, req.body.action_type, req.body);
    const io = req.app.get('io');
    if (io) io.emit('safety:lost_tourist_action', { id: req.params.id, action: req.body.action_type });
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// GET /api/safety/lost-phone/:touristId
router.get('/lost-phone/:touristId', async (req, res, next) => {
  try {
    const info = await safetyService.getLostPhoneInfo(req.params.touristId);
    return success(res, info);
  } catch (err) {
    next(err);
  }
});

// POST /api/safety/lost-phone/:touristId/actions
router.post('/lost-phone/:touristId/actions', requirePermission('safety:action'), validate(lostPhoneActionSchema), auditLog('lost_phone', 'action'), async (req, res, next) => {
  try {
    const result = await safetyService.lostPhoneAction(req.params.touristId, req.user, req.body.action_type, req.body);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
