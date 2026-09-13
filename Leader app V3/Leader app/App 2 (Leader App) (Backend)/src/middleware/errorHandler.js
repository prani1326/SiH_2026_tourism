const { AppError } = require('../utils/errors');
const logger = require('../utils/logger');

/**
 * Global error handler middleware.
 * Standardizes responses, logs via structured logger, and tags with correlation requestId.
 */
function errorHandler(err, req, res, _next) {
  const requestId = req.id || 'unknown';

  // Log non-operational errors or 500s as errors; operational ones as warnings
  if (err.isOperational && err.statusCode < 500) {
    logger.warn(`Handled operational error: ${err.message}`, {
      requestId,
      code: err.code,
      statusCode: err.statusCode,
      url: req.originalUrl,
      method: req.method,
    });
  } else {
    logger.error(`Unhandled or internal error: ${err.message}`, {
      requestId,
      error: err.stack,
      url: req.originalUrl,
      method: req.method,
    });
  }

  // Base error payload
  const errorPayload = {
    code: err.code || 'INTERNAL_ERROR',
    message: err.message || 'An unexpected error occurred',
    requestId,
  };

  // Operational errors (our custom errors)
  if (err.isOperational) {
    if (err.details) errorPayload.details = err.details;
    return res.status(err.statusCode || 400).json({
      success: false,
      errorCode: err.code || 'BAD_REQUEST',
      message: err.message,
      error: errorPayload,
    });
  }

  // Joi validation errors
  if (err.isJoi || err.name === 'ValidationError') {
    return res.status(400).json({
      success: false,
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Validation failed',
        requestId,
        details: err.details
          ? err.details.map((d) => ({ field: d.path?.join('.'), message: d.message }))
          : [{ message: err.message }],
      },
    });
  }

  // JWT errors
  if (err.name === 'JsonWebTokenError' || err.name === 'TokenExpiredError') {
    return res.status(401).json({
      success: false,
      error: {
        code: 'UNAUTHORIZED',
        message: err.name === 'TokenExpiredError' ? 'Token expired' : 'Invalid token',
        requestId,
      },
    });
  }

  // Firestore conflict / duplicate errors
  if (err.code === 6 || err.code === 'ALREADY_EXISTS' || err.code === 'SQLITE_CONSTRAINT') {
    return res.status(409).json({
      success: false,
      error: {
        code: 'CONFLICT',
        message: 'A record with this unique data already exists',
        requestId,
      },
    });
  }

  // Firestore permission denied
  if (err.code === 7 || err.code === 'PERMISSION_DENIED') {
    return res.status(403).json({
      success: false,
      error: {
        code: 'FORBIDDEN',
        message: 'Permission denied to access resource',
        requestId,
      },
    });
  }

  // Unknown internal server errors (500)
  return res.status(500).json({
    success: false,
    error: {
      code: 'INTERNAL_ERROR',
      message: process.env.NODE_ENV === 'production'
        ? 'An internal error occurred. Please report the requestId to support.'
        : err.message || 'An unexpected error occurred',
      requestId,
    },
  });
}

module.exports = { errorHandler };
