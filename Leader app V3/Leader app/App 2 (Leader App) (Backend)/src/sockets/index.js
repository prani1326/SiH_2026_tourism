const jwt = require('jsonwebtoken');
const config = require('../config');
const fdb = require('../services/firestoreDb');
const authService = require('../services/authService');
const logger = require('../utils/logger');

/**
 * Socket.IO handlers for real-time mobile push events using Firebase Authentication.
 */
function initSockets(io) {
  // Authentication middleware for WebSockets
  io.use(async (socket, next) => {
    const token = socket.handshake.auth?.token || socket.handshake.headers?.authorization?.replace('Bearer ', '');
    if (!token) {
      return next(new Error('Authentication required for socket connection'));
    }

    try {
      let user = null;
      let sessionId = null;

      // 1. Try verifying as JWT session
      try {
        const decoded = jwt.verify(token, config.jwt.secret);
        const session = await fdb.findOne('sessions', [
          ['token', '==', token],
          ['user_id', '==', decoded.userId],
          ['revoked', '==', 0],
        ]);

        if (session && session.expires_at > new Date().toISOString()) {
          user = await fdb.findById('users', decoded.userId);
          sessionId = session.id;
        }
      } catch (jwtErr) {
        // Fall through to Firebase verification
      }

      // 2. If not session JWT, verify as Firebase ID token
      if (!user) {
        user = await authService.verifyFirebaseToken(token);
        sessionId = `socket_${user.id}`;
      }

      if (!user || user.status !== 'active') {
        return next(new Error('User account not found or suspended'));
      }

      socket.user = {
        userId: user.id,
        role: user.role,
        sessionId,
      };
      next();
    } catch (err) {
      next(new Error(`Invalid socket authentication token: ${err.message}`));
    }
  });

  io.on('connection', (socket) => {
    const user = socket.user;
    logger.info('WebSocket client connected via Firebase', { userId: user.userId, role: user.role });

    // Join user-specific room
    socket.join(`user:${user.userId}`);

    // Join role-specific room
    socket.join(`role:${user.role}`);

    // Join general ops command room
    socket.join('ops_leaders');

    // Handle ping/presence & sync status
    socket.on('sync:status', (data) => {
      socket.emit('sync:ack', { status: 'synchronized', timestamp: new Date().toISOString() });
    });

    socket.on('disconnect', (reason) => {
      logger.info('WebSocket client disconnected', { userId: user.userId, reason });
    });
  });
}

module.exports = { initSockets };
