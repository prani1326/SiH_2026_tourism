const jwt = require('jsonwebtoken');
const config = require('../config/env');
const { db, auth } = require('../config/firebase');
const { COLLECTIONS } = require('../database/firestoreSchema');
const { sendError } = require('../utils/responseHelper');
const logger = require('../utils/logger');

/**
 * Authenticate Vendor using Firebase Authentication ID Token (or JWT)
 */
const authenticateVendor = async (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return sendError(res, 'Authentication token missing or invalid format', null, 401);
  }

  const token = authHeader.split(' ')[1];
  let vendor = null;
  let decodedFirebase = null;

  // 1. Attempt Firebase ID Token verification
  try {
    decodedFirebase = await auth.verifyIdToken(token);
  } catch (fbErr) {
    // Not a Firebase ID token or failed; will try local JWT fallback below
  }

  if (decodedFirebase) {
    const uid = decodedFirebase.uid;
    const email = decodedFirebase.email ? decodedFirebase.email.toLowerCase() : null;

    // Look up vendor in Firestore
    const vendorCol = db.collection(COLLECTIONS.VENDORS);
    let snap = await vendorCol.where('firebase_uid', '==', uid).get();

    if (snap.empty && email) {
      snap = await vendorCol.where('email', '==', email).get();
    }

    if (snap.empty) {
      const docDirect = await vendorCol.doc(String(uid)).get();
      if (docDirect.exists) {
        vendor = { id: docDirect.id, ...docDirect.data() };
      }
    } else {
      const doc = snap.docs[0];
      vendor = { id: doc.id, ...doc.data() };
    }

    if (!vendor && email) {
      // Auto-provision or find by mobile
      const phone = decodedFirebase.phone_number;
      if (phone) {
        const phoneSnap = await vendorCol.where('mobile', '==', phone.replace(/\D/g, '').slice(-10)).get();
        if (!phoneSnap.empty) {
          const doc = phoneSnap.docs[0];
          vendor = { id: doc.id, ...doc.data() };
        }
      }
    }
  }

  // 2. Fallback to JWT verification if Firebase ID token verification didn't resolve a vendor
  if (!vendor) {
    try {
      const decodedJwt = jwt.verify(token, config.JWT_SECRET);
      if (decodedJwt.role === 'vendor' && decodedJwt.vendor_id) {
        const doc = await db.collection(COLLECTIONS.VENDORS).doc(String(decodedJwt.vendor_id)).get();
        if (doc.exists) {
          vendor = { id: doc.id, ...doc.data() };
        } else {
          // Query by numeric id if string mismatch
          const numSnap = await db.collection(COLLECTIONS.VENDORS).where('id', '==', Number(decodedJwt.vendor_id)).get();
          if (!numSnap.empty) {
            vendor = { id: numSnap.docs[0].id, ...numSnap.docs[0].data() };
          }
        }
      }
    } catch (jwtErr) {
      if (jwtErr.name === 'TokenExpiredError') {
        return sendError(res, 'Token has expired. Please log in again.', null, 401);
      }
    }
  }

  if (!vendor) {
    return sendError(res, 'Invalid or unrecognized authentication token', null, 401);
  }

  if (vendor.status === 'suspended' || vendor.status === 'inactive') {
    return sendError(res, `Account is currently ${vendor.status}`, null, 403);
  }

  // Ensure vendor.id is normalized
  const numericId = Number(vendor.id) || (vendor.vendor_id ? Number(vendor.vendor_id) : vendor.id);
  req.vendor = { ...vendor, id: numericId };
  req.vendorId = numericId;
  req.firebaseUser = decodedFirebase;
  next();
};

/**
 * Authenticate Admin / Staff
 */
const authenticateStaff = async (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return sendError(res, 'Admin authentication token missing', null, 401);
  }

  const token = authHeader.split(' ')[1];
  let staff = null;

  // 1. Try Firebase Auth
  try {
    const decodedFb = await auth.verifyIdToken(token);
    if (decodedFb && decodedFb.email) {
      const snap = await db.collection(COLLECTIONS.STAFF_USERS).where('email', '==', decodedFb.email.toLowerCase()).get();
      if (!snap.empty) {
        staff = { id: snap.docs[0].id, ...snap.docs[0].data() };
      }
    }
  } catch (_) {}

  // 2. Try Admin JWT
  if (!staff) {
    try {
      const decodedJwt = jwt.verify(token, config.ADMIN_JWT_SECRET);
      if (['admin', 'staff'].includes(decodedJwt.role) && decodedJwt.staff_id) {
        const doc = await db.collection(COLLECTIONS.STAFF_USERS).doc(String(decodedJwt.staff_id)).get();
        if (doc.exists) {
          staff = { id: doc.id, ...doc.data() };
        } else {
          const numSnap = await db.collection(COLLECTIONS.STAFF_USERS).where('id', '==', Number(decodedJwt.staff_id)).get();
          if (!numSnap.empty) {
            staff = { id: numSnap.docs[0].id, ...numSnap.docs[0].data() };
          }
        }
      }
    } catch (err) {
      if (err.name === 'TokenExpiredError') {
        return sendError(res, 'Admin token has expired. Please log in again.', null, 401);
      }
    }
  }

  if (!staff) {
    return sendError(res, 'Invalid admin authentication credentials', null, 401);
  }

  if (staff.status !== 'active') {
    return sendError(res, 'Staff account is deactivated', null, 403);
  }

  const numericId = Number(staff.id) || staff.id;
  req.staff = { ...staff, id: numericId };
  req.staffId = numericId;
  next();
};

/**
 * Enforce Admin role only
 */
const requireAdmin = (req, res, next) => {
  if (!req.staff || req.staff.role !== 'admin') {
    return sendError(res, 'Administrator access required', null, 403);
  }
  next();
};

module.exports = {
  authenticateVendor,
  authenticateStaff,
  requireAdmin,
};
