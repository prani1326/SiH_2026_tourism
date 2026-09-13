const app = require('./app');
const config = require('./config/env');
const logger = require('./utils/logger');

const server = app.listen(config.PORT, '0.0.0.0', () => {
  logger.info(`=======================================================`);
  logger.info(`  Travel Company Vendor / Guide Backend Service Started `);
  logger.info(`  Environment : ${config.NODE_ENV}`);
  logger.info(`  Port        : ${config.PORT}`);
  logger.info(`  Binding Host: 0.0.0.0 (All interfaces & 10.0.2.2)`);
  logger.info(`  API Base    : http://localhost:${config.PORT}/api`);
  logger.info(`  Health Check: http://localhost:${config.PORT}/api/health`);
  logger.info(`=======================================================`);
});

// Graceful shutdown
process.on('SIGTERM', () => {
  logger.info('SIGTERM received. Shutting down gracefully...');
  server.close(() => {
    logger.info('Process terminated.');
  });
});

process.on('SIGINT', () => {
  logger.info('SIGINT received. Shutting down gracefully...');
  server.close(() => {
    logger.info('Process terminated.');
  });
});

module.exports = server;
