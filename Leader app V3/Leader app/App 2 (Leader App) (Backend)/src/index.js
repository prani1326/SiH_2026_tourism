const express = require('express');
const http = require('http');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const path = require('path');
const fs = require('fs');
const { Server } = require('socket.io');
const config = require('./config');
const firebase = require('./config/firebase');
const { requestIdMiddleware } = require('./middleware/requestId');
const { errorHandler } = require('./middleware/errorHandler');
const routes = require('./routes');
const { initSockets } = require('./sockets');
const logger = require('./utils/logger');

// Validate environment configuration
config.validate();

// Ensure data directory exists for backups / exports
const dataDir = path.resolve(__dirname, '..', 'data');
if (!fs.existsSync(dataDir)) {
  fs.mkdirSync(dataDir, { recursive: true });
}

// Initialize Express
const app = express();
const server = http.createServer(app);

// Configure CORS origin whitelist
const corsOrigins = config.cors.origins === '*'
  ? '*'
  : config.cors.origins.split(',').map((origin) => origin.trim());

const corsOptions = {
  origin: corsOrigins,
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'X-Request-Id', 'X-Device-Info'],
  exposedHeaders: ['X-Request-Id'],
  credentials: true,
  maxAge: 86400,
};

// Initialize Socket.IO
const io = new Server(server, {
  cors: {
    origin: corsOrigins,
    methods: ['GET', 'POST'],
    credentials: true,
  },
});

// Make io accessible to routes
app.set('io', io);

// Request Correlation ID (must be first)
app.use(requestIdMiddleware);

// Security Headers via Helmet
app.use(
  helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        scriptSrc: ["'self'"],
        styleSrc: ["'self'", "'unsafe-inline'"],
        imgSrc: ["'self'", 'data:', 'https:'],
        connectSrc: ["'self'", 'ws:', 'wss:'],
      },
    },
    crossOriginResourcePolicy: { policy: 'cross-origin' },
  })
);

app.use(cors(corsOptions));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Request logging
if (config.nodeEnv !== 'test') {
  app.use(morgan(config.log.level));
}

// Kubernetes Liveness Probe
app.get('/api/health/live', (req, res) => {
  res.status(200).json({ status: 'alive', timestamp: new Date().toISOString() });
});

// Kubernetes Readiness Probe
app.get('/api/health/ready', async (req, res) => {
  const isDbReady = await firebase.ping();
  if (isDbReady) {
    return res.status(200).json({ status: 'ready', database: 'connected', provider: 'firestore', timestamp: new Date().toISOString() });
  }
  return res.status(503).json({ status: 'unready', database: 'disconnected', timestamp: new Date().toISOString() });
});

// Comprehensive Health check
app.get('/api/health', async (req, res) => {
  const isDbHealthy = await firebase.ping();
  const memUsage = process.memoryUsage();

  res.status(isDbHealthy ? 200 : 503).json({
    success: isDbHealthy,
    data: {
      status: isDbHealthy ? 'healthy' : 'degraded',
      version: '1.0.0',
      uptime: Math.floor(process.uptime()),
      timestamp: new Date().toISOString(),
      environment: config.nodeEnv,
      database: isDbHealthy ? 'connected' : 'disconnected',
      provider: 'firestore',
      memory: {
        rssMb: Math.round(memUsage.rss / 1024 / 1024),
        heapUsedMb: Math.round(memUsage.heapUsed / 1024 / 1024),
      },
    },
  });
});

// API routes
app.use('/api', routes);

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    success: false,
    error: {
      code: 'NOT_FOUND',
      message: `Route ${req.method} ${req.originalUrl} not found`,
      requestId: req.id,
    },
  });
});

// Global error handler
app.use(errorHandler);

// Initialize WebSockets
initSockets(io);

// Start server
if (process.env.NODE_ENV !== 'test') {
  server.listen(config.port, '0.0.0.0', () => {
    logger.info(`LEADER Ops Backend online on port ${config.port} (0.0.0.0) [Firestore Engine]`, {
      port: config.port,
      environment: config.nodeEnv,
      api: `http://localhost:${config.port}/api`,
      emulatorApi: `http://10.0.2.2:${config.port}/api`,
      health: `http://localhost:${config.port}/api/health`,
    });
  });
}

// Graceful shutdown
const shutdown = (signal) => {
  logger.info(`${signal} received. Initiating graceful shutdown...`);

  // Close active WebSockets
  io.close(() => {
    logger.info('WebSocket connections terminated.');
  });

  server.close(() => {
    logger.info('HTTP server closed.');
    process.exit(0);
  });

  setTimeout(() => {
    logger.error('Forced shutdown after 10s timeout.');
    process.exit(1);
  }, 10000);
};

process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT', () => shutdown('SIGINT'));

module.exports = { app, server, io };
