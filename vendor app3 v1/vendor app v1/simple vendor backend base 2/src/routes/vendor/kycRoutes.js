const express = require('express');
const router = express.Router();
const kycController = require('../../controllers/vendor/kycController');
const { authenticateVendor } = require('../../middlewares/auth');
const { uploadKycDocument } = require('../../middlewares/upload');

router.use(authenticateVendor);

router.get('/', kycController.getKyc);
router.post('/', kycController.submitKyc);
router.put('/', kycController.updateKyc);
router.post('/documents', uploadKycDocument.single('document'), kycController.uploadDocument);
router.get('/status', kycController.getKycStatus);

module.exports = router;
