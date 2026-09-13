/**
 * Industrial Structured Logger
 * Provides level-based logging with ISO timestamps, correlation requestId, and redaction.
 */

const LEVELS = {
  error: 0,
  warn: 1,
  info: 2,
  debug: 3,
};

const SENSITIVE_KEYS = new Set([
  'password',
  'password_hash',
  'token',
  'tempToken',
  'mfa_secret',
  'secret',
  'authorization',
]);

function redact(obj, depth = 0) {
  if (!obj || typeof obj !== 'object' || depth > 4) return obj;
  if (Array.isArray(obj)) return obj.map((item) => redact(item, depth + 1));

  const copy = {};
  for (const [key, val] of Object.entries(obj)) {
    if (SENSITIVE_KEYS.has(key.toLowerCase())) {
      copy[key] = '[REDACTED]';
    } else if (typeof val === 'object') {
      copy[key] = redact(val, depth + 1);
    } else {
      copy[key] = val;
    }
  }
  return copy;
}

class Logger {
  constructor() {
    this.currentLevel = process.env.LOG_LEVEL || (process.env.NODE_ENV === 'production' ? 'info' : 'debug');
  }

  _log(level, message, meta = {}) {
    const numericLevel = LEVELS[level] ?? 2;
    const configuredNumeric = LEVELS[this.currentLevel] ?? 2;

    if (numericLevel > configuredNumeric) return;

    const logEntry = {
      timestamp: new Date().toISOString(),
      level: level.toUpperCase(),
      message,
      ...(meta.requestId ? { requestId: meta.requestId } : {}),
      ...(meta.userId ? { userId: meta.userId } : {}),
      ...(Object.keys(meta).length ? { context: redact(meta) } : {}),
    };

    if (process.env.NODE_ENV === 'production') {
      // In production, emit single-line structured JSON for log aggregators (ELK, CloudWatch, Datadog)
      process.stdout.write(JSON.stringify(logEntry) + '\n');
    } else {
      const prefix = `[${logEntry.timestamp}] [${logEntry.level}]`;
      const reqIdPart = logEntry.requestId ? ` [Req:${logEntry.requestId.slice(0, 8)}]` : '';
      if (level === 'error') {
        console.error(`${prefix}${reqIdPart} ${message}`, meta.error || '');
      } else if (level === 'warn') {
        console.warn(`${prefix}${reqIdPart} ${message}`);
      } else {
        console.log(`${prefix}${reqIdPart} ${message}`);
      }
    }
  }

  info(message, meta) {
    this._log('info', message, meta);
  }

  warn(message, meta) {
    this._log('warn', message, meta);
  }

  error(message, meta) {
    this._log('error', message, meta);
  }

  debug(message, meta) {
    this._log('debug', message, meta);
  }
}

module.exports = new Logger();
