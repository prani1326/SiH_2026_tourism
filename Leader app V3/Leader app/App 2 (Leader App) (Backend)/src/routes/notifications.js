const express = require('express');
const router = express.Router();
const notificationService = require('../services/notificationService');
const { authenticate } = require('../middleware/auth');
const { success, paginated } = require('../utils/response');
const fdb = require('../services/firestoreDb');

router.use(authenticate);

// GET /api/notifications
router.get('/', async (req, res, next) => {
  try {
    const result = await notificationService.list(req.user.id, req.query);
    return paginated(res, result.data, { ...result.pagination, unread_count: result.unread_count });
  } catch (err) {
    next(err);
  }
});

// POST /api/notifications/fcm-token (Register device push token)
router.post('/fcm-token', async (req, res, next) => {
  try {
    const fcmToken = req.body.fcmToken || req.body.fcm_token || req.body.token;
    if (!fcmToken) {
      return res.status(400).json({ success: false, error: { message: 'FCM token required' } });
    }
    await fdb.update('users', req.user.id, {
      fcm_token: fcmToken,
      fcm_token_updated_at: new Date().toISOString(),
    });
    await fdb.insert('fcm_tokens', {
      user_id: req.user.id,
      fcm_token: fcmToken,
      device_info: req.body.device_info || null,
      created_at: new Date().toISOString(),
    });
    return success(res, { message: 'FCM device token registered' });
  } catch (err) {
    next(err);
  }
});

// POST /api/notifications/:id/read
router.post('/:id/read', async (req, res, next) => {
  try {
    const result = await notificationService.markRead(req.params.id, req.user.id);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/notifications/:id/acknowledge
router.post('/:id/acknowledge', async (req, res, next) => {
  try {
    const result = await notificationService.acknowledge(req.params.id, req.user.id);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/notifications/read-all
router.post('/read-all', async (req, res, next) => {
  try {
    const result = await notificationService.markAllRead(req.user.id);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
