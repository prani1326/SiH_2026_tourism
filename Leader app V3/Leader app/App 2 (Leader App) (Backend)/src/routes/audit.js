const express = require('express');
const router = express.Router();
const auditService = require('../services/auditService');
const { authenticate } = require('../middleware/auth');
const { authorize } = require('../middleware/rbac');
const { success, paginated } = require('../utils/response');

router.use(authenticate);

// GET /api/audit
router.get('/', authorize('super_admin', 'ops_leader'), async (req, res, next) => {
  try {
    const query = { ...req.query };
    // Ops Leader can only inspect own audit logs
    if (req.user.role === 'ops_leader') {
      query.user_id = req.user.id;
    }
    const result = await auditService.list(query);
    return paginated(res, result.data, result.pagination);
  } catch (err) {
    next(err);
  }
});

// GET /api/audit/:id
router.get('/:id', authorize('super_admin', 'ops_leader'), async (req, res, next) => {
  try {
    const entry = await auditService.getById(req.params.id);
    return success(res, entry);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
