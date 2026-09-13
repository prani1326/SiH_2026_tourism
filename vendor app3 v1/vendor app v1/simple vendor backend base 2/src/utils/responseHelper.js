/**
 * Standard API response helper
 */
const sendSuccess = (res, message = 'Request successful', data = {}, statusCode = 200) => {
  return res.status(statusCode).json({
    success: true,
    message,
    data,
  });
};

const sendError = (res, message = 'Something went wrong', error = {}, statusCode = 500) => {
  return res.status(statusCode).json({
    success: false,
    message,
    error: typeof error === 'string' ? { detail: error } : error,
  });
};

module.exports = {
  sendSuccess,
  sendError,
};
