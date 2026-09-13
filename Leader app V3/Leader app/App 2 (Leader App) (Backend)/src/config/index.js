require('dotenv').config();

const config = {
  port: parseInt(process.env.PORT, 10) || 3000,
  nodeEnv: process.env.NODE_ENV || 'development',

  firebase: {
    projectId: process.env.FIREBASE_PROJECT_ID || process.env.GCP_PROJECT || 'leader-ops-production',
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
    privateKey: process.env.FIREBASE_PRIVATE_KEY,
    storageBucket: process.env.FIREBASE_STORAGE_BUCKET,
    serviceAccountKeyPath: process.env.FIREBASE_SERVICE_ACCOUNT_KEY_PATH,
  },

  jwt: {
    secret: process.env.JWT_SECRET || 'dev-secret-change-in-production-minimum-64-characters-long-key',
    expiresIn: process.env.JWT_EXPIRES_IN || '24h',
    refreshExpiresIn: process.env.JWT_REFRESH_EXPIRES_IN || '7d',
  },

  session: {
    timeoutMinutes: parseInt(process.env.SESSION_TIMEOUT_MINUTES, 10) || 480,
  },

  rateLimit: {
    windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS, 10) || 900000,
    maxRequests: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS, 10) || 100,
    loginMax: parseInt(process.env.LOGIN_RATE_LIMIT_MAX, 10) || 5,
  },

  cors: {
    origins: process.env.CORS_ORIGINS || '*',
  },

  log: {
    level: process.env.LOG_LEVEL || 'dev',
  },

  accountLockout: {
    maxAttempts: parseInt(process.env.AUTH_MAX_FAILED_ATTEMPTS, 10) || 5,
    lockDurationMinutes: parseInt(process.env.AUTH_LOCKOUT_MINUTES, 10) || 15,
  },

  roles: {
    SUPER_ADMIN: 'super_admin',
    OPS_LEADER: 'ops_leader',
    SAFETY_MANAGER: 'safety_manager',
    SUPPORT_MANAGER: 'support_manager',
    BOOKING_MANAGER: 'booking_manager',
    REGIONAL_OPS: 'regional_ops',
    ANALYST: 'analyst',
  },

  alertPriority: {
    LOW: 'low',
    MEDIUM: 'medium',
    HIGH: 'high',
    CRITICAL: 'critical',
    EMERGENCY: 'emergency',
  },

  statusLevels: {
    NORMAL: 'normal',
    ATTENTION: 'attention',
    WARNING: 'warning',
    CRITICAL: 'critical',
    EMERGENCY: 'emergency',
    RESOLVED: 'resolved',
  },

  bookingStates: [
    'requested', 'pending', 'confirmed', 'failed',
    'cancelled', 'refund_requested', 'refunded', 'disputed',
  ],

  ticketCategories: [
    'booking', 'payment', 'refund', 'hotel', 'transport',
    'flight', 'lost_item', 'safety', 'emergency', 'itinerary',
    'technical_issue', 'general_inquiry',
  ],

  ticketPriority: ['low', 'medium', 'high', 'critical'],

  incidentTypes: [
    'medical', 'accident', 'theft', 'lost_tourist', 'missing_person',
    'harassment', 'security_issue', 'natural_disaster', 'transport_accident',
    'hotel_issue', 'partner_misconduct', 'other_emergency',
  ],

  incidentWorkflow: [
    'reported', 'verified', 'assigned', 'tourist_contacted',
    'emergency_contact_notified', 'local_assistance_coordinated',
    'monitoring', 'resolved', 'report_created',
  ],

  disruptionTypes: [
    'weather', 'heavy_rain', 'flood', 'storm', 'landslide', 'heatwave',
    'transport_disruption', 'flight_disruption', 'road_closure',
    'destination_closure', 'local_emergency',
  ],

  /**
   * Validate configuration for production safety.
   */
  validate() {
    if (this.nodeEnv === 'production') {
      const defaultSecret = 'dev-secret-change-in-production-minimum-64-characters-long-key';
      if (!process.env.JWT_SECRET || process.env.JWT_SECRET === defaultSecret || process.env.JWT_SECRET.length < 32) {
        throw new Error('FATAL SECURITY ERROR: JWT_SECRET must be set to a secure key (min 32 characters) in production mode.');
      }
      if (this.cors.origins === '*') {
        console.warn('SECURITY WARNING: Wildcard CORS origin (*) is not recommended in production.');
      }
    }
  },
};

module.exports = config;
