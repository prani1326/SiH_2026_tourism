const express = require('express');
const router = express.Router();
const bookingController = require('../../controllers/vendor/bookingController');
const { authenticateVendor } = require('../../middlewares/auth');
const { requireKycApproved } = require('../../middlewares/kycCheck');

router.use(authenticateVendor);
router.use(requireKycApproved);

router.get('/', bookingController.getBookings);
router.get('/:id', bookingController.getBookingById);
router.post('/:id/accept', bookingController.acceptBooking);
router.post('/:id/reject', bookingController.rejectBooking);

module.exports = router;
