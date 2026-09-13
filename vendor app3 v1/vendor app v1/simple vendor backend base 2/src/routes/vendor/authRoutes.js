const express = require('express');
const router = express.Router();
const authController = require('../../controllers/vendor/authController');
const { authenticateVendor } = require('../../middlewares/auth');

router.post('/register', authController.register);
router.post('/login', authController.login);
router.post('/request-otp', authController.requestOtp);
router.post('/verify-otp', authController.verifyOtp);
router.get('/me', authenticateVendor, authController.getMe);
router.post('/logout', authenticateVendor, authController.logout);

module.exports = router;
