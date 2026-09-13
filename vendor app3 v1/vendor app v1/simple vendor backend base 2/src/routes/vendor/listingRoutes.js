const express = require('express');
const router = express.Router();
const listingController = require('../../controllers/vendor/listingController');
const { authenticateVendor } = require('../../middlewares/auth');
const { requireKycApproved } = require('../../middlewares/kycCheck');

router.use(authenticateVendor);
router.use(requireKycApproved);

router.get('/', listingController.getListings);
router.get('/:id', listingController.getListingById);
router.post('/', listingController.createListing);
router.put('/:id', listingController.updateListing);
router.delete('/:id', listingController.deleteListing);

module.exports = router;
