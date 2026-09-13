const express = require('express');
const router = express.Router();
const profileController = require('../../controllers/vendor/profileController');
const { authenticateVendor } = require('../../middlewares/auth');
const { uploadProfilePhoto } = require('../../middlewares/upload');

router.use(authenticateVendor);

router.get('/', profileController.getProfile);
router.put('/', profileController.updateProfile);
router.post('/photo', uploadProfilePhoto.single('profile_photo'), profileController.uploadPhoto);

module.exports = router;
