const { v4: uuidv4 } = require('uuid');

/**
 * Attaches a unique correlation ID (X-Request-Id) to each incoming request.
 * If client provides an X-Request-Id, sanitizes and preserves it, otherwise generates a UUIDv4.
 */
function requestIdMiddleware(req, res, next) {
  const incomingId = req.headers['x-request-id'];
  const requestId = typeof incomingId === 'string' && /^[a-zA-Z0-9_-]{8,64}$/.test(incomingId)
    ? incomingId
    : uuidv4();

  req.id = requestId;
  res.setHeader('X-Request-Id', requestId);
  next();
}

module.exports = { requestIdMiddleware };
