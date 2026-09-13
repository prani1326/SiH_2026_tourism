const express = require('express');
const router = express.Router();
const weatherService = require('../services/weatherService');
const { authenticate } = require('../middleware/auth');
const { validate } = require('../middleware/validator');
const { disruptionNotifySchema } = require('../utils/validators');
const { success, paginated } = require('../utils/response');
const { auditLog } = require('../middleware/auditLogger');

router.use(authenticate);

// GET /api/weather/disruptions
router.get('/disruptions', async (req, res, next) => {
  try {
    const result = await weatherService.listDisruptions(req.query);
    return paginated(res, result.data, result.pagination);
  } catch (err) {
    next(err);
  }
});

// GET /api/weather/disruptions/:id
router.get('/disruptions/:id', async (req, res, next) => {
  try {
    const disruption = await weatherService.getDisruptionById(req.params.id);
    return success(res, disruption);
  } catch (err) {
    next(err);
  }
});

// GET /api/weather/disruptions/:id/affected-trips
router.get('/disruptions/:id/affected-trips', async (req, res, next) => {
  try {
    const affectedTrips = await weatherService.getAffectedTrips(req.params.id);
    return success(res, affectedTrips);
  } catch (err) {
    next(err);
  }
});

// POST /api/weather/disruptions/:id/notify
router.post('/disruptions/:id/notify', validate(disruptionNotifySchema), auditLog('disruption', 'notify_tourists'), async (req, res, next) => {
  try {
    const result = await weatherService.notifyAffectedTourists(req.params.id, req.user, req.body.message, req.body.channel);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/weather/disruptions/:id/resolve
router.post('/disruptions/:id/resolve', auditLog('disruption', 'resolve'), async (req, res, next) => {
  try {
    const result = await weatherService.resolveDisruption(req.params.id, req.user);
    const io = req.app.get('io');
    if (io) io.emit('disruption:resolved', { disruptionId: req.params.id });
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// GET /api/weather/destinations/:destination/alerts
router.get('/destinations/:destination/alerts', async (req, res, next) => {
  try {
    const alerts = await weatherService.getDestinationAlerts(req.params.destination);
    return success(res, alerts);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
