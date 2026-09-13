const express = require('express');
const router = express.Router();
const supportService = require('../services/supportService');
const { authenticate } = require('../middleware/auth');
const { validate } = require('../middleware/validator');
const {
  ticketReplySchema,
  ticketStatusSchema,
  ticketPrioritySchema,
  assignSchema,
  noteSchema,
} = require('../utils/validators');
const { success, paginated, created } = require('../utils/response');
const { auditLog } = require('../middleware/auditLogger');

router.use(authenticate);

// GET /api/support/tickets
router.get('/tickets', async (req, res, next) => {
  try {
    const result = await supportService.listTickets(req.query);
    return paginated(res, result.data, result.pagination);
  } catch (err) {
    next(err);
  }
});

// GET /api/support/tickets/:id
router.get('/tickets/:id', async (req, res, next) => {
  try {
    const ticket = await supportService.getTicketById(req.params.id);
    return success(res, ticket);
  } catch (err) {
    next(err);
  }
});

// POST /api/support/tickets/:id/assign
router.post('/tickets/:id/assign', validate(assignSchema), auditLog('support_ticket', 'assign'), async (req, res, next) => {
  try {
    const result = await supportService.assignTicket(req.params.id, req.body.ops_leader_id, req.user);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/support/tickets/:id/reply
router.post('/tickets/:id/reply', validate(ticketReplySchema), async (req, res, next) => {
  try {
    const result = await supportService.addReply(req.params.id, req.user, req.body.message, req.body.attachments);
    return created(res, result);
  } catch (err) {
    next(err);
  }
});

const handleStatusUpdate = async (req, res, next) => {
  try {
    const result = await supportService.updateStatus(req.params.id, req.body.status, req.user, req.body.reason);
    return success(res, result);
  } catch (err) {
    next(err);
  }
};

// PATCH & POST /api/support/tickets/:id/status
router.patch('/tickets/:id/status', validate(ticketStatusSchema), auditLog('support_ticket', 'status_change'), handleStatusUpdate);
router.post('/tickets/:id/status', validate(ticketStatusSchema), auditLog('support_ticket', 'status_change'), handleStatusUpdate);

// PATCH /api/support/tickets/:id/priority
router.patch('/tickets/:id/priority', validate(ticketPrioritySchema), auditLog('support_ticket', 'priority_change'), async (req, res, next) => {
  try {
    const result = await supportService.updatePriority(req.params.id, req.body.priority, req.user, req.body.reason);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/support/tickets/:id/resolve
router.post('/tickets/:id/resolve', auditLog('support_ticket', 'resolve'), async (req, res, next) => {
  try {
    const result = await supportService.resolveTicket(req.params.id, req.user, req.body.resolution);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// GET /api/support/tickets/:id/notes
router.get('/tickets/:id/notes', async (req, res, next) => {
  try {
    const notes = await supportService.getNotes(req.params.id);
    return success(res, notes);
  } catch (err) {
    next(err);
  }
});

// POST /api/support/tickets/:id/notes
router.post('/tickets/:id/notes', validate(noteSchema), async (req, res, next) => {
  try {
    const result = await supportService.addNote(req.params.id, req.user, req.body.content, req.body.type);
    return created(res, result);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
