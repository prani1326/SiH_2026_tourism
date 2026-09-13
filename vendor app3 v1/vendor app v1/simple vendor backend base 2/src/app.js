const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
const path = require('path');
const rateLimit = require('express-rate-limit');
const config = require('./config/env');
const apiRoutes = require('./routes');
const errorHandler = require('./middlewares/errorHandler');
const { sendError, sendSuccess } = require('./utils/responseHelper');

const app = express();

// Global Middlewares
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

if (config.NODE_ENV !== 'test') {
  app.use(morgan('dev'));
}

// Rate Limiter for Auth endpoints
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // Limit each IP to 100 requests per window
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    message: 'Too many authentication attempts, please try again after 15 minutes.',
  },
});

app.use('/api/vendor/auth', authLimiter);
app.use('/api/admin/auth', authLimiter);

// Static uploads directory serving
app.use('/uploads', express.static(path.resolve(config.UPLOAD_PATH)));

// API Health Check
app.get('/api/health', (req, res) => {
  return sendSuccess(res, 'Travel Vendor Backend Service is healthy and active', {
    timestamp: new Date().toISOString(),
    uptime_seconds: Math.floor(process.uptime()),
    environment: config.NODE_ENV,
  });
});

// Master API Routes
app.use('/api', apiRoutes);

// 404 Handler
app.use((req, res) => {
  return sendError(res, `Route ${req.method} ${req.originalUrl} not found`, null, 404);
});

// Global Error Handler
app.use(errorHandler);

module.exports = app;
