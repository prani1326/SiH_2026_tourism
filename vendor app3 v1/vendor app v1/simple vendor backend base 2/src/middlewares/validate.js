const { sendError } = require('../utils/responseHelper');

/**
 * Higher-order middleware for validating request bodies
 */
const validateBody = (validatorFn) => {
  return (req, res, next) => {
    const error = validatorFn(req.body);
    if (error) {
      return sendError(res, error.message || 'Validation error', error.details || error, 422);
    }
    next();
  };
};

module.exports = {
  validateBody,
};
