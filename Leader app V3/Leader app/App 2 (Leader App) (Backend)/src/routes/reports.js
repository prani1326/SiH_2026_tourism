const express = require('express');
const router = express.Router();
const reportService = require('../services/reportService');
const { authenticate } = require('../middleware/auth');
const { authorize } = require('../middleware/rbac');
const { sensitiveOpLimiter } = require('../middleware/rateLimiter');
const { success } = require('../utils/response');

router.use(authenticate);

// GET /api/reports/daily
router.get('/daily', async (req, res, next) => {
  try {
    const data = await reportService.daily(req.query);
    return success(res, data);
  } catch (err) {
    next(err);
  }
});

// GET /api/reports/bookings
router.get('/bookings', async (req, res, next) => {
  try {
    const data = await reportService.bookings(req.query);
    return success(res, data);
  } catch (err) {
    next(err);
  }
});

// GET /api/reports/support
router.get('/support', async (req, res, next) => {
  try {
    const data = await reportService.support(req.query);
    return success(res, data);
  } catch (err) {
    next(err);
  }
});

// GET /api/reports/incidents
router.get('/incidents', async (req, res, next) => {
  try {
    const data = await reportService.incidents(req.query);
    return success(res, data);
  } catch (err) {
    next(err);
  }
});

// GET /api/reports/safety
router.get('/safety', async (req, res, next) => {
  try {
    const data = await reportService.safety(req.query);
    return success(res, data);
  } catch (err) {
    next(err);
  }
});

// GET /api/reports/partners
router.get('/partners', async (req, res, next) => {
  try {
    const data = await reportService.partners(req.query);
    return success(res, data);
  } catch (err) {
    next(err);
  }
});

// GET /api/reports/destinations
router.get('/destinations', async (req, res, next) => {
  try {
    const data = await reportService.destinations(req.query);
    return success(res, data);
  } catch (err) {
    next(err);
  }
});

// POST /api/reports/export
router.post('/export', sensitiveOpLimiter, authorize('super_admin', 'ops_leader', 'analyst', 'regional_ops'), async (req, res, next) => {
  try {
    const { type = 'daily', ...filters } = req.body;
    const result = await reportService.exportData(type, filters);

    if (result.format === 'csv') {
      res.setHeader('Content-Type', 'text/csv');
      res.setHeader('Content-Disposition', `attachment; filename=report-${type}-${Date.now()}.csv`);
      return res.send(result.data);
    }
    return success(res, result.data);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
