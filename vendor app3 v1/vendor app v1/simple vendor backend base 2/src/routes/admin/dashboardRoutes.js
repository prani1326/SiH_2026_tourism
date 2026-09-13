const express = require('express');
const router = express.Router();
const adminDashboardController = require('../../controllers/admin/adminDashboardController');
const { authenticateStaff } = require('../../middlewares/auth');

router.use(authenticateStaff);

router.get('/', adminDashboardController.getDashboard);

module.exports = router;
