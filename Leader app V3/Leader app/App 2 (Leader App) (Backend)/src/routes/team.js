const express = require('express');
const router = express.Router();
const teamService = require('../services/teamService');
const { authenticate } = require('../middleware/auth');
const { authorize } = require('../middleware/rbac');
const { validate } = require('../middleware/validator');
const { teamAvailabilitySchema, teamApproveSchema, assignSchema } = require('../utils/validators');
const { success, paginated, created } = require('../utils/response');
const { auditLog } = require('../middleware/auditLogger');

router.use(authenticate);

// GET /api/team
router.get('/', async (req, res, next) => {
  try {
    const result = await teamService.list(req.query);
    return paginated(res, result.data, result.pagination);
  } catch (err) {
    next(err);
  }
});

// GET /api/team/pending-approvals
router.get('/pending-approvals', authorize('super_admin'), async (req, res, next) => {
  try {
    const pending = await teamService.getPendingApprovals();
    return success(res, pending);
  } catch (err) {
    next(err);
  }
});

// GET /api/team/:id
router.get('/:id', async (req, res, next) => {
  try {
    const member = await teamService.getById(req.params.id);
    return success(res, member);
  } catch (err) {
    next(err);
  }
});

// POST /api/team/:id/assign
router.post('/:id/assign', async (req, res, next) => {
  try {
    const { case_type, case_id } = req.body;
    const result = await teamService.assignCase(req.params.id, case_type, case_id, req.user);
    return created(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/team/assignments/:id/reassign
router.post('/assignments/:id/reassign', async (req, res, next) => {
  try {
    const { new_user_id } = req.body;
    const result = await teamService.reassignCase(req.params.id, new_user_id, req.user);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// PATCH /api/team/:id/availability
router.patch('/:id/availability', validate(teamAvailabilitySchema), async (req, res, next) => {
  try {
    const result = await teamService.updateAvailability(req.params.id, req.body.available, req.body.region);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/team/:id/approve
router.post('/:id/approve', authorize('super_admin'), validate(teamApproveSchema), auditLog('user', 'approve_account'), async (req, res, next) => {
  try {
    const result = await teamService.approveAccount(req.params.id, req.user, req.body.role, req.body.region);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
