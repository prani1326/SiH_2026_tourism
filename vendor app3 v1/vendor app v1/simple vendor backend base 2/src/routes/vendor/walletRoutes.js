const express = require('express');
const router = express.Router();
const walletController = require('../../controllers/vendor/walletController');
const { authenticateVendor } = require('../../middlewares/auth');
const { requireKycApproved } = require('../../middlewares/kycCheck');

router.use(authenticateVendor);
router.use(requireKycApproved);

router.get('/', walletController.getWallet);
router.get('/transactions', walletController.getTransactions);
router.get('/transactions/:id', walletController.getTransactionById);

module.exports = router;
