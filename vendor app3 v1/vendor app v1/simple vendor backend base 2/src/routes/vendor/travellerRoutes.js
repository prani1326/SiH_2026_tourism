const express = require('express');
const router = express.Router();
const travellerController = require('../../controllers/vendor/travellerController');
const { authenticateVendor } = require('../../middlewares/auth');
const { requireKycApproved } = require('../../middlewares/kycCheck');

router.use(authenticateVendor);
router.use(requireKycApproved);

router.get('/', travellerController.getTravellers);
router.get('/:id', travellerController.getTravellerById);

module.exports = router;
