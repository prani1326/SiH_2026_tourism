const Joi = require('joi');

// ── Auth ──
const loginSchema = Joi.object({
  email: Joi.string().email({ tlds: false }).optional(),
  phone: Joi.string().pattern(/^\+?[1-9]\d{6,14}$/).optional(),
  password: Joi.string().min(8).required(),
}).xor('email', 'phone');

const signupSchema = Joi.object({
  name: Joi.string().min(1).max(200).required(),
  email: Joi.string().email({ tlds: false }).optional(),
  phone: Joi.string().pattern(/^\+?[1-9]\d{6,14}$/).optional(),
  password: Joi.string().min(8).optional(),
  referenceId: Joi.string().min(1).max(100).required(),
  googleIdToken: Joi.string().optional(),
  role: Joi.string().optional(),
}).or('email', 'phone');

const googleAuthSchema = Joi.object({
  idToken: Joi.string().required(),
  referenceId: Joi.string().optional(),
});

const verifyOtpSchema = Joi.object({
  userId: Joi.string().required(),
  otp: Joi.string().length(6).required(),
  tempToken: Joi.string().required(),
});

const forgotPasswordSchema = Joi.object({
  email: Joi.string().email({ tlds: false }).optional(),
  phone: Joi.string().optional(),
}).xor('email', 'phone');

const resetPasswordSchema = Joi.object({
  token: Joi.string().required(),
  newPassword: Joi.string().min(8).required(),
});

const setupMfaSchema = Joi.object({
  password: Joi.string().required(),
});


// ── Tourists ──
const touristNoteSchema = Joi.object({
  content: Joi.string().min(1).max(5000).required(),
  type: Joi.string().valid('general', 'follow_up', 'escalation', 'emergency').default('general'),
});

// ── Bookings ──
const bookingActionSchema = Joi.object({
  reason: Joi.string().max(2000).optional(),
  notes: Joi.string().max(5000).optional(),
});

// ── Support ──
const ticketReplySchema = Joi.object({
  message: Joi.string().min(1).max(10000).required(),
  attachments: Joi.array().items(Joi.string().uri()).max(5).optional(),
});

const ticketStatusSchema = Joi.object({
  status: Joi.string().valid('open', 'in_progress', 'waiting', 'resolved', 'closed').required(),
  reason: Joi.string().max(2000).optional(),
});

const ticketPrioritySchema = Joi.object({
  priority: Joi.string().valid('low', 'medium', 'high', 'critical').required(),
  reason: Joi.string().max(2000).optional(),
});

// ── Incidents ──
const createIncidentSchema = Joi.object({
  type: Joi.string().valid(
    'medical', 'accident', 'theft', 'lost_tourist', 'missing_person',
    'harassment', 'security_issue', 'natural_disaster', 'transport_accident',
    'hotel_issue', 'partner_misconduct', 'other_emergency'
  ).required(),
  severity: Joi.string().valid('low', 'medium', 'high', 'critical', 'emergency').required(),
  tourist_id: Joi.string().optional(),
  trip_id: Joi.string().optional(),
  location: Joi.string().max(500).optional(),
  description: Joi.string().min(1).max(10000).required(),
  gps_lat: Joi.number().min(-90).max(90).optional(),
  gps_lng: Joi.number().min(-180).max(180).optional(),
});

const incidentTimelineSchema = Joi.object({
  action: Joi.string().min(1).max(5000).required(),
  type: Joi.string().valid('update', 'action', 'contact', 'escalation', 'resolution').default('update'),
});

const incidentWorkflowSchema = Joi.object({
  stage: Joi.string().valid(
    'reported', 'verified', 'assigned', 'tourist_contacted',
    'emergency_contact_notified', 'local_assistance_coordinated',
    'monitoring', 'resolved', 'report_created'
  ).required(),
  notes: Joi.string().max(5000).optional(),
});

// ── SOS ──
const sosActionSchema = Joi.object({
  action_type: Joi.string().valid(
    'acknowledge', 'call_tourist', 'message_tourist', 'contact_emergency',
    'escalate', 'dispatch_help', 'response_started', 'resolve'
  ).required(),
  notes: Joi.string().max(5000).optional(),
});

const sosTimelineSchema = Joi.object({
  entry: Joi.string().min(1).max(5000).required(),
  type: Joi.string().valid('action', 'update', 'escalation', 'resolution').default('update'),
});

// ── Safety ──
const lostTouristActionSchema = Joi.object({
  action_type: Joi.string().valid(
    'contact_tourist', 'contact_group', 'set_meeting_point',
    'share_location', 'escalate', 'mark_found', 'close'
  ).required(),
  notes: Joi.string().max(5000).optional(),
  meeting_point: Joi.string().max(500).optional(),
  location: Joi.string().max(500).optional(),
});

const lostPhoneActionSchema = Joi.object({
  action_type: Joi.string().valid(
    'verify_identity', 'provide_web_access', 'contact_trusted',
    'help_recover', 'freeze_actions', 'emergency_comms'
  ).required(),
  notes: Joi.string().max(5000).optional(),
});

// ── Partners ──
const partnerActionSchema = Joi.object({
  reason: Joi.string().max(2000).required(),
  notes: Joi.string().max(5000).optional(),
});

// ── Communications ──
const sendMessageSchema = Joi.object({
  tourist_id: Joi.string().required(),
  trip_id: Joi.string().optional(),
  channel: Joi.string().valid('chat', 'push', 'email', 'sms', 'emergency').required(),
  content_type: Joi.string().valid('text', 'image', 'document', 'location', 'emergency_instructions').default('text'),
  content: Joi.string().min(1).max(10000).required(),
});

const emergencyBroadcastSchema = Joi.object({
  trip_id: Joi.string().optional(),
  destination: Joi.string().optional(),
  message: Joi.string().min(1).max(5000).required(),
  priority: Joi.string().valid('high', 'critical', 'emergency').required(),
}).or('trip_id', 'destination');

// ── Assign ──
const assignSchema = Joi.object({
  ops_leader_id: Joi.string().optional(),
  vendor_id: Joi.string().optional(),
  vendor_name: Joi.string().optional(),
  notes: Joi.string().max(2000).allow('', null).optional(),
  reason: Joi.string().max(2000).allow('', null).optional(),
}).or('ops_leader_id', 'vendor_id', 'vendor_name');

// ── Notes ──
const noteSchema = Joi.object({
  content: Joi.string().min(1).max(5000).required(),
  type: Joi.string().valid('general', 'follow_up', 'escalation', 'emergency', 'contact', 'refund').default('general'),
});

// ── Team ──
const teamAvailabilitySchema = Joi.object({
  available: Joi.boolean().required(),
  region: Joi.string().max(200).optional(),
});

const teamApproveSchema = Joi.object({
  role: Joi.string().valid(
    'ops_leader', 'safety_manager', 'support_manager',
    'booking_manager', 'regional_ops', 'analyst'
  ).required(),
  region: Joi.string().max(200).optional(),
});

// ── Reports ──
const reportFilterSchema = Joi.object({
  start_date: Joi.date().iso().optional(),
  end_date: Joi.date().iso().optional(),
  destination: Joi.string().optional(),
  format: Joi.string().valid('json', 'csv').default('json'),
});

// ── Weather ──
const disruptionNotifySchema = Joi.object({
  message: Joi.string().min(1).max(5000).required(),
  channel: Joi.string().valid('push', 'sms', 'email', 'all').default('push'),
});

module.exports = {
  loginSchema,
  signupSchema,
  googleAuthSchema,
  verifyOtpSchema,
  forgotPasswordSchema,
  resetPasswordSchema,
  setupMfaSchema,
  touristNoteSchema,
  bookingActionSchema,
  ticketReplySchema,
  ticketStatusSchema,
  ticketPrioritySchema,
  createIncidentSchema,
  incidentTimelineSchema,
  incidentWorkflowSchema,
  sosActionSchema,
  sosTimelineSchema,
  lostTouristActionSchema,
  lostPhoneActionSchema,
  partnerActionSchema,
  sendMessageSchema,
  emergencyBroadcastSchema,
  assignSchema,
  noteSchema,
  teamAvailabilitySchema,
  teamApproveSchema,
  reportFilterSchema,
  disruptionNotifySchema,
};
