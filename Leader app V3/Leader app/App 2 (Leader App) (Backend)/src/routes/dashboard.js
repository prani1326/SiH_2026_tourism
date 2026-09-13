const express = require('express');
const router = express.Router();
const dashboardService = require('../services/dashboardService');
const { authenticate } = require('../middleware/auth');
const { success } = require('../utils/response');

router.use(authenticate);

// GET /api/dashboard/stats
router.get('/stats', async (req, res, next) => {
  try {
    const stats = await dashboardService.getStats();
    return success(res, stats);
  } catch (err) {
    next(err);
  }
});

// GET /api/dashboard/kpis
router.get('/kpis', async (req, res, next) => {
  try {
    const kpis = await dashboardService.getKpis();
    return success(res, kpis);
  } catch (err) {
    next(err);
  }
});

// GET /api/dashboard/charts/:type
router.get('/charts/:type', async (req, res, next) => {
  try {
    const days = parseInt(req.query.days, 10) || 7;
    const data = await dashboardService.getChartData(req.params.type, days);
    return success(res, data);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
