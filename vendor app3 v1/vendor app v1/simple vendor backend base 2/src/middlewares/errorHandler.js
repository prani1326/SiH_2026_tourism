const multer = require('multer');
const { sendError } = require('../utils/responseHelper');
const logger = require('../utils/logger');

const errorHandler = (err, req, res, next) => {
  logger.error(err.message, { stack: err.stack, path: req.path, method: req.method });

  if (err instanceof multer.MulterError) {
    if (err.code === 'LIMIT_FILE_SIZE') {
      return sendError(res, 'File size is too large. Maximum allowed size exceeded.', { code: err.code }, 400);
    }
    return sendError(res, `Upload error: ${err.message}`, { code: err.code }, 400);
  }

  if (err.name === 'SyntaxError' && err.status === 400 && 'body' in err) {
    return sendError(res, 'Invalid JSON payload in request body', null, 400);
  }

  if (err.message && err.message.includes('Invalid file type') || err.message.includes('Invalid document format')) {
    return sendError(res, err.message, null, 400);
  }

  const statusCode = err.statusCode || 500;
  const message = err.message || 'Internal server error';

  return sendError(res, message, process.env.NODE_ENV === 'development' ? { stack: err.stack } : null, statusCode);
};

module.exports = errorHandler;
