const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const config = require('../../config/env');
const { db, auth } = require('../../config/firebase');
const { COLLECTIONS } = require('../../database/firestoreSchema');
const { sendSuccess, sendError } = require('../../utils/responseHelper');
const { isValidEmail, isValidMobile, normalizeMobile } = require('../../utils/validators');
const auditService = require('../../services/auditService');
const walletService = require('../../services/walletService');

const generateToken = async (vendor) => {
  const payload = {
    vendor_id: vendor.id,
    role: 'vendor',
    email: vendor.email,
    mobile: vendor.mobile,
  };
  return jwt.sign(payload, config.JWT_SECRET, { expiresIn: config.JWT_EXPIRES_IN });
};

const authController = {
  /**
   * Register new vendor
   */
  register: async (req, res) => {
    try {
      const { name, email, mobile, password } = req.body;

      if (!name || !email || !mobile || !password) {
        return sendError(res, 'Name, email, mobile, and password are required', null, 422);
      }

      if (!isValidEmail(email)) {
        return sendError(res, 'Invalid email format', null, 422);
      }

      const cleanMobile = normalizeMobile(mobile);
      if (!isValidMobile(cleanMobile)) {
        return sendError(res, 'Invalid 10-digit mobile number', null, 422);
      }

      if (password.length < 6) {
        return sendError(res, 'Password must be at least 6 characters long', null, 422);
      }

      const vendorCol = db.collection(COLLECTIONS.VENDORS);
      const cleanEmail = email.trim().toLowerCase();

      // Check existing email
      const emailSnap = await vendorCol.where('email', '==', cleanEmail).get();
      if (!emailSnap.empty) {
        return sendError(res, 'Email is already registered', null, 409);
      }

      // Check existing mobile
      const mobileSnap = await vendorCol.where('mobile', '==', cleanMobile).get();
      if (!mobileSnap.empty) {
        return sendError(res, 'Mobile number is already registered', null, 409);
      }

      const salt = bcrypt.genSaltSync(10);
      const password_hash = bcrypt.hashSync(password, salt);

      // Determine next numeric ID
      const allVendorsSnap = await vendorCol.get();
      let maxId = 0;
      allVendorsSnap.forEach((doc) => {
        const d = doc.data();
        const nid = Number(d.id || doc.id);
        if (!isNaN(nid) && nid > maxId) maxId = nid;
      });
      const vendorId = maxId + 1;

      // Optional: create Firebase Auth user
      let firebaseUid = null;
      try {
        const fbUser = await auth.createUser({
          email: cleanEmail,
          phoneNumber: cleanMobile.startsWith('+') ? cleanMobile : `+91${cleanMobile}`,
          displayName: name.trim(),
        });
        firebaseUid = fbUser.uid;
      } catch (_) {}

      const newVendorData = {
        id: vendorId,
        firebase_uid: firebaseUid,
        name: name.trim(),
        email: cleanEmail,
        mobile: cleanMobile,
        password_hash,
        profile_photo: null,
        status: 'active',
        kyc_status: 'pending',
        rejection_reason: null,
        verification_remarks: null,
        verified_at: null,
        verified_by: null,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      };

      await vendorCol.doc(String(vendorId)).set(newVendorData);

      // Initialize vendor wallet
      await walletService.getOrCreateWallet(vendorId);

      const token = await generateToken(newVendorData);
      let firebaseCustomToken = null;
      if (firebaseUid) {
        try {
          firebaseCustomToken = await auth.createCustomToken(firebaseUid, { role: 'vendor', vendor_id: vendorId });
        } catch (_) {}
      }

      await auditService.logAction({
        actorType: 'vendor',
        actorId: vendorId,
        action: 'Vendor Created',
        entityType: 'vendor',
        entityId: vendorId,
        ipAddress: req.ip,
      });

      const { password_hash: _, ...vendorSafe } = newVendorData;

      return sendSuccess(res, 'Vendor registered successfully', {
        token,
        firebase_token: firebaseCustomToken,
        vendor: vendorSafe,
      }, 201);
    } catch (err) {
      return sendError(res, 'Failed to register vendor: ' + err.message, null, 500);
    }
  },

  /**
   * Vendor Login with password
   */
  login: async (req, res) => {
    try {
      const { identifier, email, mobile, password } = req.body;
      const loginId = (identifier || email || mobile || '').trim();

      if (!loginId || !password) {
        return sendError(res, 'Mobile/Email and password are required', null, 400);
      }

      const cleanMobile = normalizeMobile(loginId);
      const vendorCol = db.collection(COLLECTIONS.VENDORS);

      let vendor = null;

      // Query by email
      let snap = await vendorCol.where('email', '==', loginId.toLowerCase()).get();
      if (!snap.empty) {
        vendor = snap.docs[0].data();
      }

      // Query by mobile if not found
      if (!vendor) {
        snap = await vendorCol.where('mobile', '==', cleanMobile).get();
        if (!snap.empty) {
          vendor = snap.docs[0].data();
        }
      }

      if (!vendor) {
        return sendError(res, 'Invalid credentials', null, 401);
      }

      const isMatch = bcrypt.compareSync(password, vendor.password_hash);
      if (!isMatch) {
        return sendError(res, 'Invalid credentials', null, 401);
      }

      if (vendor.status !== 'active') {
        return sendError(res, `Your account is ${vendor.status}. Please contact support.`, null, 403);
      }

      const numericId = Number(vendor.id) || vendor.id;
      const vendorNormalized = { ...vendor, id: numericId };

      const token = await generateToken(vendorNormalized);
      let firebaseCustomToken = null;
      if (vendor.firebase_uid) {
        try {
          firebaseCustomToken = await auth.createCustomToken(vendor.firebase_uid, { role: 'vendor', vendor_id: numericId });
        } catch (_) {}
      }

      await auditService.logAction({
        actorType: 'vendor',
        actorId: numericId,
        action: 'Vendor Login',
        entityType: 'vendor',
        entityId: numericId,
        ipAddress: req.ip,
      });

      const { password_hash, ...vendorSafe } = vendorNormalized;

      return sendSuccess(res, 'Login successful', {
        token,
        firebase_token: firebaseCustomToken,
        vendor: vendorSafe,
      });
    } catch (err) {
      return sendError(res, 'Login failed: ' + err.message, null, 500);
    }
  },

  /**
   * Request OTP for mobile login / verification
   */
  requestOtp: async (req, res) => {
    try {
      const { mobile } = req.body;
      if (!mobile) {
        return sendError(res, 'Mobile number is required', null, 400);
      }

      const cleanMobile = normalizeMobile(mobile);
      if (!isValidMobile(cleanMobile)) {
        return sendError(res, 'Invalid 10-digit mobile number', null, 422);
      }

      const otpCode = process.env.NODE_ENV === 'test' || !process.env.PROD_SMS ? '123456' : Math.floor(100000 + Math.random() * 900000).toString();
      const expiresAt = new Date(Date.now() + 10 * 60 * 1000).toISOString();

      const otpCol = db.collection(COLLECTIONS.OTP_VERIFICATIONS);
      const allOtpSnap = await otpCol.get();
      const nextId = allOtpSnap.size + 1;

      await otpCol.doc(String(nextId)).set({
        id: nextId,
        mobile: cleanMobile,
        otp_code: otpCode,
        expires_at: expiresAt,
        is_verified: 0,
        created_at: new Date().toISOString(),
      });

      return sendSuccess(res, 'OTP sent successfully to ' + cleanMobile, {
        mobile: cleanMobile,
        otp_demo: otpCode,
        expires_in_minutes: 10,
      });
    } catch (err) {
      return sendError(res, 'Failed to request OTP: ' + err.message, null, 500);
    }
  },

  /**
   * Verify OTP & Login/Register
   */
  verifyOtp: async (req, res) => {
    try {
      const { mobile, otp_code, name } = req.body;
      if (!mobile || !otp_code) {
        return sendError(res, 'Mobile number and OTP code are required', null, 400);
      }

      const cleanMobile = normalizeMobile(mobile);
      const otpCol = db.collection(COLLECTIONS.OTP_VERIFICATIONS);
      const snap = await otpCol.where('mobile', '==', cleanMobile).get();

      let validRecord = null;
      let recordDocId = null;

      snap.forEach((doc) => {
        const d = doc.data();
        if (d.otp_code === String(otp_code).trim() && (d.is_verified === 0 || d.is_verified === false)) {
          if (new Date(d.expires_at) > new Date()) {
            validRecord = d;
            recordDocId = doc.id;
          }
        }
      });

      if (!validRecord) {
        return sendError(res, 'Invalid or expired OTP', null, 400);
      }

      await otpCol.doc(recordDocId).set({ is_verified: 1 }, { merge: true });

      const vendorCol = db.collection(COLLECTIONS.VENDORS);
      let vendorSnap = await vendorCol.where('mobile', '==', cleanMobile).get();
      let vendor = null;

      if (!vendorSnap.empty) {
        vendor = vendorSnap.docs[0].data();
      } else {
        const allVendors = await vendorCol.get();
        let maxId = 0;
        allVendors.forEach((doc) => {
          const nid = Number(doc.data().id || doc.id);
          if (!isNaN(nid) && nid > maxId) maxId = nid;
        });
        const newId = maxId + 1;
        const dummyEmail = `vendor_${cleanMobile}@travelvendor.com`;
        const defaultPasswordHash = bcrypt.hashSync(cleanMobile + '_secret', 10);
        const vendorName = name && name.trim() ? name.trim() : `Vendor ${cleanMobile.slice(-4)}`;

        vendor = {
          id: newId,
          name: vendorName,
          email: dummyEmail,
          mobile: cleanMobile,
          password_hash: defaultPasswordHash,
          profile_photo: null,
          status: 'active',
          kyc_status: 'pending',
          rejection_reason: null,
          verification_remarks: null,
          verified_at: null,
          verified_by: null,
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString(),
        };

        await vendorCol.doc(String(newId)).set(vendor);
        await walletService.getOrCreateWallet(newId);

        await auditService.logAction({
          actorType: 'vendor',
          actorId: newId,
          action: 'Vendor Created via OTP',
          entityType: 'vendor',
          entityId: newId,
          ipAddress: req.ip,
        });
      }

      const numericId = Number(vendor.id) || vendor.id;
      const vendorNormalized = { ...vendor, id: numericId };
      const token = await generateToken(vendorNormalized);

      await auditService.logAction({
        actorType: 'vendor',
        actorId: numericId,
        action: 'Vendor Login via OTP',
        entityType: 'vendor',
        entityId: numericId,
        ipAddress: req.ip,
      });

      const { password_hash, ...vendorSafe } = vendorNormalized;

      return sendSuccess(res, 'OTP verified successfully', {
        token,
        vendor: vendorSafe,
      });
    } catch (err) {
      return sendError(res, 'OTP verification failed: ' + err.message, null, 500);
    }
  },

  /**
   * Get current authenticated vendor
   */
  getMe: async (req, res) => {
    try {
      const vId = Number(req.vendorId) || req.vendorId;
      const doc = await db.collection(COLLECTIONS.VENDORS).doc(String(vId)).get();

      if (!doc.exists) {
        // Try searching by numeric id
        const numSnap = await db.collection(COLLECTIONS.VENDORS).where('id', '==', vId).get();
        if (numSnap.empty) {
          return sendError(res, 'Vendor not found', null, 404);
        }
        const { password_hash, ...safe } = numSnap.docs[0].data();
        return sendSuccess(res, 'Vendor profile retrieved', safe);
      }

      const { password_hash, ...safe } = doc.data();
      return sendSuccess(res, 'Vendor profile retrieved', safe);
    } catch (err) {
      return sendError(res, 'Failed to fetch vendor: ' + err.message, null, 500);
    }
  },

  /**
   * Logout vendor
   */
  logout: async (req, res) => {
    try {
      await auditService.logAction({
        actorType: 'vendor',
        actorId: req.vendorId,
        action: 'Vendor Logout',
        entityType: 'vendor',
        entityId: req.vendorId,
        ipAddress: req.ip,
      });

      return sendSuccess(res, 'Logged out successfully');
    } catch (err) {
      return sendError(res, 'Logout failed: ' + err.message, null, 500);
    }
  },
};

module.exports = authController;
