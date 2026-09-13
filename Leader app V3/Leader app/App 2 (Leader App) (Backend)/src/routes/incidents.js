const express = require('express');
const router = express.Router();
const incidentService = require('../services/incidentService');
const { authenticate } = require('../middleware/auth');
const { requirePermission } = require('../middleware/rbac');
const { validate } = require('../middleware/validator');
const {
  createIncidentSchema,
  incidentTimelineSchema,
  incidentWorkflowSchema,
  assignSchema,
  noteSchema,
} = require('../utils/validators');
const { success, paginated, created } = require('../utils/response');
const { auditLog } = require('../middleware/auditLogger');
const fdb = require('../services/firestoreDb');

router.use(authenticate);

// GET /api/incidents
router.get('/', async (req, res, next) => {
  try {
    const result = await incidentService.list(req.query);
    return paginated(res, result.data, result.pagination);
  } catch (err) {
    next(err);
  }
});

// POST /api/incidents
router.post('/', requirePermission('incidents:create'), validate(createIncidentSchema), auditLog('incident', 'create'), async (req, res, next) => {
  try {
    const result = await incidentService.create(req.body, req.user);
    const io = req.app.get('io');
    if (io) io.emit('incident:new', { incidentId: result.id, severity: req.body.severity, type: req.body.type });
    return created(res, result);
  } catch (err) {
    next(err);
  }
});

// GET /api/incidents/:id
router.get('/:id', async (req, res, next) => {
  try {
    const incident = await incidentService.getById(req.params.id);
    return success(res, incident);
  } catch (err) {
    next(err);
  }
});

// PUT /api/incidents/:id/status
router.put('/:id/status', auditLog('incident', 'update_status'), async (req, res, next) => {
  try {
    const status = req.body.status;
    const now = new Date().toISOString();
    await fdb.update('incidents', req.params.id, { status, updated_at: now });
    await incidentService.addTimeline(req.params.id, req.user, `Status updated to ${status}`);
    const updated = await incidentService.getById(req.params.id);
    return success(res, updated);
  } catch (err) {
    next(err);
  }
});

// POST /api/incidents/:id/assign
router.post('/:id/assign', requirePermission('incidents:assign'), validate(assignSchema), auditLog('incident', 'assign'), async (req, res, next) => {
  try {
    const result = await incidentService.assign(req.params.id, req.body.ops_leader_id, req.user);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/incidents/:id/timeline
router.post('/:id/timeline', requirePermission('incidents:timeline'), validate(incidentTimelineSchema), async (req, res, next) => {
  try {
    const result = await incidentService.addTimeline(req.params.id, req.user, req.body.action, req.body.type);
    return created(res, result);
  } catch (err) {
    next(err);
  }
});

// PATCH /api/incidents/:id/workflow
router.patch('/:id/workflow', requirePermission('incidents:workflow'), validate(incidentWorkflowSchema), auditLog('incident', 'workflow_advance'), async (req, res, next) => {
  try {
    const result = await incidentService.advanceWorkflow(req.params.id, req.body.stage, req.user, req.body.notes);
    const io = req.app.get('io');
    if (io) io.emit('incident:update', { incidentId: req.params.id, stage: req.body.stage });
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/incidents/:id/resolve
router.post('/:id/resolve', requirePermission('incidents:resolve'), auditLog('incident', 'resolve'), async (req, res, next) => {
  try {
    const result = await incidentService.resolve(req.params.id, req.user, req.body);
    const io = req.app.get('io');
    if (io) io.emit('incident:resolved', { incidentId: req.params.id });
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// GET /api/incidents/:id/notes
router.get('/:id/notes', async (req, res, next) => {
  try {
    const notes = await incidentService.getNotes(req.params.id);
    return success(res, notes);
  } catch (err) {
    next(err);
  }
});

// POST /api/incidents/:id/notes
router.post('/:id/notes', requirePermission('incidents:notes'), validate(noteSchema), async (req, res, next) => {
  try {
    const result = await incidentService.addNote(req.params.id, req.user, req.body.content, req.body.type);
    return created(res, result);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
