/**
 * Standardized API response helpers.
 * Every response follows: { success, data, meta?, error? }
 */

function success(res, data, statusCode = 200, meta = null) {
  const response = { success: true, data };
  if (meta) response.meta = meta;
  return res.status(statusCode).json(response);
}

function created(res, data, meta = null) {
  return success(res, data, 201, meta);
}

function paginated(res, data, pagination) {
  return success(res, data, 200, { pagination });
}

function error(res, message, statusCode = 500, code = 'INTERNAL_ERROR', details = null) {
  const response = {
    success: false,
    error: { code, message },
  };
  if (details) response.error.details = details;
  return res.status(statusCode).json(response);
}

function noContent(res) {
  return res.status(204).send();
}

module.exports = { success, created, paginated, error, noContent };
