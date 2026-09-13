const { onRequest } = require('firebase-functions/v2/https');
const { app } = require('./src/index');

exports.api = onRequest(
  {
    cors: true,
    region: 'us-central1',
    memory: '512MiB',
    timeoutSeconds: 60,
    invoker: 'public',
  },
  app
);
