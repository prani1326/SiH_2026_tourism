const express = require('express');
const router = express.Router();
const partnerService = require('../services/partnerService');
const { authenticate } = require('../middleware/auth');
const { validate } = require('../middleware/validator');
const { partnerActionSchema } = require('../utils/validators');
const { success, paginated } = require('../utils/response');
const { auditLog } = require('../middleware/auditLogger');

router.use(authenticate);

// GET /api/partners
router.get('/', async (req, res, next) => {
  try {
    const result = await partnerService.list(req.query);
    return paginated(res, result.data, result.pagination);
  } catch (err) {
    next(err);
  }
});

// GET /api/partners/:id
router.get('/:id', async (req, res, next) => {
  try {
    const partner = await partnerService.getById(req.params.id);
    return success(res, partner);
  } catch (err) {
    next(err);
  }
});

// GET /api/partners/:id/listings
router.get('/:id/listings', async (req, res, next) => {
  try {
    const listings = await partnerService.getListings(req.params.id);
    return success(res, listings);
  } catch (err) {
    next(err);
  }
});

// POST /api/partners/:id/approve
router.post('/:id/approve', auditLog('partner', 'approve'), async (req, res, next) => {
  try {
    const result = await partnerService.approvePartner(req.params.id, req.user, req.body);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/partners/:id/reject
router.post('/:id/reject', validate(partnerActionSchema), auditLog('partner', 'reject'), async (req, res, next) => {
  try {
    const result = await partnerService.rejectPartner(req.params.id, req.user, req.body);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/partners/:id/suspend
router.post('/:id/suspend', validate(partnerActionSchema), auditLog('partner', 'suspend'), async (req, res, next) => {
  try {
    const result = await partnerService.suspendPartner(req.params.id, req.user, req.body);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// GET /api/partners/:id/complaints
router.get('/:id/complaints', async (req, res, next) => {
  try {
    const complaints = await partnerService.getComplaints(req.params.id);
    return success(res, complaints);
  } catch (err) {
    next(err);
  }
});

// POST /api/partners/:id/flag
router.post('/:id/flag', validate(partnerActionSchema), auditLog('partner', 'flag'), async (req, res, next) => {
  try {
    const result = await partnerService.flagPartner(req.params.id, req.user, req.body.reason);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/partners/listings/:id/approve
router.post('/listings/:id/approve', auditLog('listing', 'approve'), async (req, res, next) => {
  try {
    const result = await partnerService.approveListing(req.params.id, req.user);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/partners/listings/:id/reject
router.post('/listings/:id/reject', validate(partnerActionSchema), auditLog('listing', 'reject'), async (req, res, next) => {
  try {
    const result = await partnerService.rejectListing(req.params.id, req.user, req.body.reason);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/partners/listings/:id/suspend
router.post('/listings/:id/suspend', validate(partnerActionSchema), auditLog('listing', 'suspend'), async (req, res, next) => {
  try {
    const result = await partnerService.suspendListing(req.params.id, req.user, req.body.reason);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
