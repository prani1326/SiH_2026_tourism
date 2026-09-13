const express = require('express');
const router = express.Router();
const adminAuthController = require('../../controllers/admin/adminAuthController');
const { authenticateStaff } = require('../../middlewares/auth');

router.post('/login', adminAuthController.login);
router.get('/me', authenticateStaff, adminAuthController.getMe);

module.exports = router;
