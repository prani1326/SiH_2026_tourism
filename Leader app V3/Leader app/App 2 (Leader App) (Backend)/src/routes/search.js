const express = require('express');
const router = express.Router();
const searchService = require('../services/searchService');
const { authenticate } = require('../middleware/auth');
const { success } = require('../utils/response');

router.use(authenticate);

// GET /api/search?q=xyz
router.get('/', async (req, res, next) => {
  try {
    const result = await searchService.search(req.query);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
