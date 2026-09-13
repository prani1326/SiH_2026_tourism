const express = require('express');
const router = express.Router();
const travellerAuthController = require('../controllers/travellerAuthController');

router.post('/register', travellerAuthController.register);
router.post('/login', travellerAuthController.login);
router.post('/otp/request', travellerAuthController.requestOtp);
router.post('/otp/verify', travellerAuthController.verifyOtp);

module.exports = router;
