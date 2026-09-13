const express = require('express');
const router = express.Router();
const adminVendorController = require('../../controllers/admin/adminVendorController');
const { authenticateStaff } = require('../../middlewares/auth');

router.use(authenticateStaff);

router.get('/', adminVendorController.getVendors);
router.get('/:id', adminVendorController.getVendorById);
router.post('/:id/verify-kyc', adminVendorController.verifyKyc);

module.exports = router;
