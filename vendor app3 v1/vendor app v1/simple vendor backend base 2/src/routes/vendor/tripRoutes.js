const express = require('express');
const router = express.Router();
const tripController = require('../../controllers/vendor/tripController');
const { authenticateVendor } = require('../../middlewares/auth');
const { requireKycApproved } = require('../../middlewares/kycCheck');

router.use(authenticateVendor);
router.use(requireKycApproved);

router.get('/', tripController.getTrips);
router.get('/:id', tripController.getTripById);
router.post('/:id/start', tripController.startTrip);
router.post('/:id/end', tripController.endTrip);
router.post('/:id/location', tripController.recordLocation);
router.get('/:id/location', tripController.getTripLocations);

module.exports = router;
