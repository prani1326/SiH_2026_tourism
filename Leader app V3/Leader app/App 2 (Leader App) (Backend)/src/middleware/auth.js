const jwt = require('jsonwebtoken');
const config = require('../config');
const { UnauthorizedError } = require('../utils/errors');
const fdb = require('../services/firestoreDb');
const authService = require('../services/authService');

/**
 * Authentication middleware for Firebase & Ops Leader sessions.
 * Verifies Firebase ID token or session JWT, verifies user account status,
 * and attaches verified user to req.user for RBAC.
 */
async function authenticate(req, res, next) {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      throw new UnauthorizedError('No authentication token provided');
    }

    const token = authHeader.split(' ')[1];

    // 1. Check if token is a standard session JWT
    let isJwt = false;
    let decoded = null;
    try {
      decoded = jwt.verify(token, config.jwt.secret);
      isJwt = true;
    } catch (jwtErr) {
      isJwt = false;
    }

    if (isJwt && decoded && decoded.userId) {
      // Check active session in Firestore
      const now = new Date().toISOString();
      const session = await fdb.findOne('sessions', [
        ['token', '==', token],
        ['user_id', '==', decoded.userId],
        ['revoked', '==', 0],
      ]);

      if (!session || session.expires_at <= now) {
        throw new UnauthorizedError('Session expired or revoked');
      }

      const user = await fdb.findById('users', decoded.userId);
      if (!user || user.status !== 'active') {
        throw new UnauthorizedError('Account not found or inactive');
      }

      req.user = user;
      req.token = token;
      req.sessionId = session.id;
      return next();
    }

    // 2. Otherwise verify as Firebase ID Token
    const user = await authService.verifyFirebaseToken(token);
    req.user = user;
    req.token = token;
    req.sessionId = `fb_${user.id}`;
    return next();
  } catch (err) {
    if (err.name === 'JsonWebTokenError') {
      return next(new UnauthorizedError('Invalid token'));
    }
    if (err.name === 'TokenExpiredError') {
      return next(new UnauthorizedError('Token expired'));
    }
    next(err);
  }
}

module.exports = { authenticate };
