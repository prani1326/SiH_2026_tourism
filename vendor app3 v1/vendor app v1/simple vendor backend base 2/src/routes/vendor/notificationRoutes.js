const express = require('express');
const router = express.Router();
const notificationController = require('../../controllers/vendor/notificationController');
const { authenticateVendor } = require('../../middlewares/auth');

router.use(authenticateVendor);

router.get('/', notificationController.getNotifications);
router.post('/read-all', notificationController.markAllAsRead);
router.post('/:id/read', notificationController.markAsRead);

module.exports = router;
