const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const config = require('../../config/env');
const { db } = require('../../config/firebase');
const { COLLECTIONS } = require('../../database/firestoreSchema');
const { sendSuccess, sendError } = require('../../utils/responseHelper');
const auditService = require('../../services/auditService');

const generateAdminToken = (staff) => {
  return jwt.sign(
    {
      staff_id: staff.id,
      role: staff.role,
      email: staff.email,
      name: staff.name,
    },
    config.ADMIN_JWT_SECRET,
    { expiresIn: config.ADMIN_JWT_EXPIRES_IN }
  );
};

const adminAuthController = {
  /**
   * Admin / Staff Login
   */
  login: async (req, res) => {
    try {
      const { email, password } = req.body;
      if (!email || !password) {
        return sendError(res, 'Email and password are required', null, 400);
      }

      const cleanEmail = email.trim().toLowerCase();
      const col = db.collection(COLLECTIONS.STAFF_USERS);
      const snap = await col.where('email', '==', cleanEmail).get();

      if (snap.empty) {
        return sendError(res, 'Invalid credentials', null, 401);
      }

      const staff = snap.docs[0].data();

      const isMatch = bcrypt.compareSync(password, staff.password_hash);
      if (!isMatch) {
        return sendError(res, 'Invalid credentials', null, 401);
      }

      if (staff.status !== 'active') {
        return sendError(res, 'Your staff account has been deactivated', null, 403);
      }

      const numericId = Number(staff.id) || staff.id;
      const staffNormalized = { ...staff, id: numericId };
      const token = generateAdminToken(staffNormalized);

      await auditService.logAction({
        actorType: 'staff',
        actorId: numericId,
        action: 'Staff Login',
        entityType: 'staff_user',
        entityId: numericId,
        ipAddress: req.ip,
      });

      const { password_hash, ...safeStaff } = staffNormalized;

      return sendSuccess(res, 'Admin authentication successful', {
        token,
        user: safeStaff,
      });
    } catch (err) {
      return sendError(res, 'Login failed: ' + err.message, null, 500);
    }
  },

  /**
   * Get current authenticated Admin / Staff profile
   */
  getMe: async (req, res) => {
    try {
      const sId = Number(req.staffId) || req.staffId;
      const col = db.collection(COLLECTIONS.STAFF_USERS);
      let staff = null;

      const doc = await col.doc(String(sId)).get();
      if (doc.exists) {
        staff = doc.data();
      } else {
        const snap = await col.where('id', '==', sId).get();
        if (!snap.empty) staff = snap.docs[0].data();
      }

      if (!staff) {
        return sendError(res, 'Staff user not found', null, 404);
      }

      const { password_hash, ...safeStaff } = staff;
      return sendSuccess(res, 'Staff profile retrieved', safeStaff);
    } catch (err) {
      return sendError(res, 'Failed to fetch staff profile: ' + err.message, null, 500);
    }
  },
};

module.exports = adminAuthController;
