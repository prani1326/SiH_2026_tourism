const express = require('express');
const router = express.Router();
const dashboardController = require('../../controllers/vendor/dashboardController');
const { authenticateVendor } = require('../../middlewares/auth');

router.use(authenticateVendor);

router.get('/', dashboardController.getDashboard);

module.exports = router;
