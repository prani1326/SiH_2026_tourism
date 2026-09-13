const { ForbiddenError } = require('../utils/errors');

/**
 * Role-based access control middleware factory.
 * Usage: authorize('super_admin', 'ops_leader')
 * Allows request only if user's role is in the allowed list.
 */
function authorize(...allowedRoles) {
  return (req, res, next) => {
    if (!req.user) {
      return next(new ForbiddenError('Authentication required'));
    }
    if (!allowedRoles.includes(req.user.role)) {
      return next(new ForbiddenError(`Role '${req.user.role}' does not have access to this resource`));
    }
    next();
  };
}

/**
 * Granular permission check.
 * Roles and their general access levels.
 */
const ROLE_PERMISSIONS = {
  super_admin: ['*'],
  ops_leader: [
    'dashboard:read', 'alerts:*', 'trips:*', 'tourists:*', 'bookings:*',
    'support:*', 'incidents:*', 'sos:*', 'safety:*', 'weather:*',
    'partners:*', 'communications:*', 'notifications:*', 'reports:read',
    'team:read', 'audit:read_own',
  ],
  safety_manager: [
    'dashboard:read', 'alerts:read', 'alerts:safety', 'trips:read', 'tourists:read',
    'incidents:*', 'sos:*', 'safety:*', 'weather:*', 'partners:safety_audit',
    'notifications:*', 'reports:safety',
  ],
  support_manager: [
    'dashboard:read', 'alerts:read', 'alerts:support', 'trips:read', 'tourists:read',
    'bookings:read', 'support:*', 'communications:*', 'notifications:*', 'reports:support',
  ],
  booking_manager: [
    'dashboard:read', 'alerts:read', 'alerts:booking', 'trips:read', 'tourists:read',
    'bookings:*', 'support:booking', 'notifications:*', 'reports:booking',
  ],
  regional_ops: [
    'dashboard:read', 'alerts:read', 'trips:regional', 'tourists:regional',
    'bookings:regional', 'support:regional', 'incidents:regional', 'sos:regional',
    'safety:regional', 'weather:read', 'partners:regional', 'notifications:*',
    'reports:regional',
  ],
  analyst: [
    'dashboard:read', 'alerts:read', 'trips:read', 'tourists:read',
    'bookings:read', 'support:read', 'incidents:read', 'sos:read',
    'safety:read', 'weather:read', 'partners:read', 'reports:*',
  ],
};

/**
 * Check if a user role has a specific permission.
 */
function hasPermission(role, permission) {
  const perms = ROLE_PERMISSIONS[role] || [];
  if (perms.includes('*')) return true;

  const [resource, action] = permission.split(':');
  return perms.some((p) => {
    if (p === permission) return true;
    if (p === `${resource}:*`) return true;
    return false;
  });
}

/**
 * Permission middleware factory.
 * Usage: requirePermission('incidents:write')
 */
function requirePermission(permission) {
  return (req, res, next) => {
    if (!req.user) {
      return next(new ForbiddenError('Authentication required'));
    }
    if (!hasPermission(req.user.role, permission)) {
      return next(new ForbiddenError(`Permission '${permission}' required`));
    }
    next();
  };
}

module.exports = { authorize, hasPermission, requirePermission, ROLE_PERMISSIONS };
