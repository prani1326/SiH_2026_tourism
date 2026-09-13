const express = require('express');
const router = express.Router();
const authService = require('../services/authService');
const { authenticate } = require('../middleware/auth');
const { loginLimiter, sensitiveOpLimiter } = require('../middleware/rateLimiter');
const { validate } = require('../middleware/validator');
const {
  loginSchema,
  signupSchema,
  googleAuthSchema,
  verifyOtpSchema,
  forgotPasswordSchema,
  resetPasswordSchema,
  setupMfaSchema,
} = require('../utils/validators');
const { success, created } = require('../utils/response');

// POST /api/auth/signup
router.post('/signup', sensitiveOpLimiter, validate(signupSchema), async (req, res, next) => {
  try {
    const meta = {
      ip: req.ip || req.connection?.remoteAddress,
      device: req.headers['x-device-info'] || req.headers['user-agent'],
      userAgent: req.headers['user-agent'],
    };
    const result = await authService.signup(req.body, meta);
    return res.status(201).json({
      success: true,
      message: result.message,
      token: result.token,
      expiresAt: result.expiresAt,
      user: result.user,
      data: result,
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/auth/google
router.post('/google', loginLimiter, validate(googleAuthSchema), async (req, res, next) => {
  try {
    const meta = {
      ip: req.ip || req.connection?.remoteAddress,
      device: req.headers['x-device-info'] || req.headers['user-agent'],
      userAgent: req.headers['user-agent'],
    };
    const result = await authService.googleAuth(req.body, meta);
    if (result.errorCode === 'NEW_USER') {
      return res.status(200).json(result);
    }
    return res.status(200).json({
      success: true,
      message: result.message,
      token: result.token,
      user: result.user,
      data: result,
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/auth/login
router.post('/login', loginLimiter, validate(loginSchema), async (req, res, next) => {
  try {
    const meta = {
      ip: req.ip || req.connection?.remoteAddress,
      device: req.headers['x-device-info'] || req.headers['user-agent'],
      userAgent: req.headers['user-agent'],
    };
    const result = await authService.login(req.body, meta);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/auth/verify-otp
router.post('/verify-otp', loginLimiter, validate(verifyOtpSchema), async (req, res, next) => {
  try {
    const meta = {
      ip: req.ip || req.connection?.remoteAddress,
      device: req.headers['x-device-info'] || req.headers['user-agent'],
      userAgent: req.headers['user-agent'],
    };
    const result = await authService.verifyOtp(req.body, meta);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/auth/verify-token (Firebase ID Token verification)
router.post('/verify-token', async (req, res, next) => {
  try {
    const idToken = req.body.idToken || req.body.id_token || req.headers.authorization?.replace('Bearer ', '');
    const user = await authService.verifyFirebaseToken(idToken);
    return success(res, { user, message: 'Firebase token verified successfully' });
  } catch (err) {
    next(err);
  }
});

// POST /api/auth/refresh
router.post('/refresh', async (req, res, next) => {
  try {
    const token = req.headers.authorization?.replace('Bearer ', '') || req.body.token;
    const result = await authService.refreshToken(token);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/auth/forgot-password
router.post('/forgot-password', sensitiveOpLimiter, validate(forgotPasswordSchema), async (req, res, next) => {
  try {
    const result = await authService.forgotPassword(req.body);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/auth/reset-password
router.post('/reset-password', sensitiveOpLimiter, validate(resetPasswordSchema), async (req, res, next) => {
  try {
    const result = await authService.resetPassword(req.body);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// POST /api/auth/logout
router.post('/logout', authenticate, async (req, res, next) => {
  try {
    const result = await authService.logout(req.token);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// GET /api/auth/me
router.get('/me', authenticate, async (req, res, next) => {
  try {
    const profile = await authService.getMe(req.user.id);
    return success(res, profile);
  } catch (err) {
    next(err);
  }
});

// GET /api/auth/sessions
router.get('/sessions', authenticate, async (req, res, next) => {
  try {
    const sessions = await authService.getSessions(req.user.id);
    return success(res, sessions);
  } catch (err) {
    next(err);
  }
});

// DELETE /api/auth/sessions/:id
router.delete('/sessions/:id', authenticate, async (req, res, next) => {
  try {
    const result = await authService.revokeSession(req.user.id, req.params.id);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

// GET /api/auth/login-history
router.get('/login-history', authenticate, async (req, res, next) => {
  try {
    const limit = parseInt(req.query.limit, 10) || 50;
    const history = await authService.getLoginHistory(req.user.id, limit);
    return success(res, history);
  } catch (err) {
    next(err);
  }
});

// POST /api/auth/setup-mfa
router.post('/setup-mfa', authenticate, validate(setupMfaSchema), async (req, res, next) => {
  try {
    const result = await authService.setupMfa(req.user.id, req.body.password);
    return success(res, result);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
