const express = require('express');
const router = express.Router();
const communicationService = require('../services/communicationService');
const { authenticate } = require('../middleware/auth');
const { authorize, requirePermission } = require('../middleware/rbac');
const { validate } = require('../middleware/validator');
const { sendMessageSchema, emergencyBroadcastSchema } = require('../utils/validators');
const { success, created } = require('../utils/response');
const { auditLog } = require('../middleware/auditLogger');

router.use(authenticate);

// GET /api/communications/messages
router.get('/messages', async (req, res, next) => {
  try {
    const messages = await communicationService.getMessages(req.query);
    return success(res, messages);
  } catch (err) {
    next(err);
  }
});

// POST /api/communications/send
router.post('/send', requirePermission('communications:send'), validate(sendMessageSchema), auditLog('communication', 'send_message'), async (req, res, next) => {
  try {
    const result = await communicationService.sendMessage(req.user, req.body);
    return created(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/communications/emergency
router.post('/emergency', authorize('super_admin', 'ops_leader', 'safety_manager'), validate(emergencyBroadcastSchema), auditLog('communication', 'emergency_broadcast'), async (req, res, next) => {
  try {
    const result = await communicationService.sendEmergencyBroadcast(req.user, req.body);
    const io = req.app.get('io');
    if (io) io.emit('broadcast:emergency', { destination: req.body.destination, tripId: req.body.trip_id });
    return created(res, result);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
