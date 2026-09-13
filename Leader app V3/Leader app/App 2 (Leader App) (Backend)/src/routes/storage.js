const express = require('express');
const router = express.Router();
const multer = require('multer');
const { authenticate } = require('../middleware/auth');
const storageService = require('../services/storageService');
const { success } = require('../utils/response');

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 25 * 1024 * 1024 }, // 25MB max
});

/**
 * POST /api/storage/upload
 * Upload file to Firebase Storage
 */
router.post('/upload', authenticate, upload.single('file'), async (req, res, next) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        error: { code: 'BAD_REQUEST', message: 'No file provided for upload' },
      });
    }
    const metadata = {
      folder: req.body.folder || 'documents',
      referenceType: req.body.reference_type,
      referenceId: req.body.reference_id,
      userId: req.user?.id,
    };
    const fileRecord = await storageService.uploadFile(req.file, metadata);
    return success(res, fileRecord, 201);
  } catch (err) {
    next(err);
  }
});

/**
 * GET /api/storage/files
 * List files
 */
router.get('/files', authenticate, async (req, res, next) => {
  try {
    const files = await storageService.listFiles(req.query.reference_type, req.query.reference_id);
    return success(res, files);
  } catch (err) {
    next(err);
  }
});

/**
 * GET /api/storage/files/:id
 * Get file details
 */
router.get('/files/:id', authenticate, async (req, res, next) => {
  try {
    const file = await storageService.getFile(req.params.id);
    if (!file) {
      return res.status(404).json({ success: false, error: { message: 'File not found' } });
    }
    return success(res, file);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
