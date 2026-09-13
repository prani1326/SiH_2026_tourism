const express = require('express');
const router = express.Router();
const adminAuditController = require('../../controllers/admin/adminAuditController');
const { authenticateStaff } = require('../../middlewares/auth');

router.use(authenticateStaff);

router.get('/', adminAuditController.getLogs);

module.exports = router;
