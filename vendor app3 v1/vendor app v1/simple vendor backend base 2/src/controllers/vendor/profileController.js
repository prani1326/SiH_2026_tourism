const path = require('path');
const fs = require('fs');
const config = require('../../config/env');
const { db, storage } = require('../../config/firebase');
const { COLLECTIONS } = require('../../database/firestoreSchema');
const { sendSuccess, sendError } = require('../../utils/responseHelper');
const { isValidEmail } = require('../../utils/validators');
const auditService = require('../../services/auditService');

const profileController = {
  /**
   * Get vendor profile
   */
  getProfile: async (req, res) => {
    try {
      const vId = Number(req.vendorId) || req.vendorId;
      const vendorCol = db.collection(COLLECTIONS.VENDORS);
      let vendor = null;

      const doc = await vendorCol.doc(String(vId)).get();
      if (doc.exists) {
        vendor = doc.data();
      } else {
        const snap = await vendorCol.where('id', '==', vId).get();
        if (!snap.empty) vendor = snap.docs[0].data();
      }

      if (!vendor) {
        return sendError(res, 'Profile not found', null, 404);
      }

      const { password_hash, ...vendorSafe } = vendor;
      return sendSuccess(res, 'Profile retrieved successfully', vendorSafe);
    } catch (err) {
      return sendError(res, 'Failed to fetch profile: ' + err.message, null, 500);
    }
  },

  /**
   * Update vendor profile
   */
  updateProfile: async (req, res) => {
    try {
      const { name, email } = req.body;
      const vId = Number(req.vendorId) || req.vendorId;
      const vendorCol = db.collection(COLLECTIONS.VENDORS);

      let docRef = vendorCol.doc(String(vId));
      let doc = await docRef.get();
      if (!doc.exists) {
        const snap = await vendorCol.where('id', '==', vId).get();
        if (snap.empty) {
          return sendError(res, 'Vendor not found', null, 404);
        }
        docRef = snap.docs[0].ref;
        doc = snap.docs[0];
      }

      const existing = doc.data();
      let updatedName = existing.name;
      let updatedEmail = existing.email;

      if (name && name.trim()) {
        updatedName = name.trim();
      }

      if (email && email.trim()) {
        const cleanEmail = email.trim().toLowerCase();
        if (!isValidEmail(cleanEmail)) {
          return sendError(res, 'Invalid email format', null, 422);
        }
        if (cleanEmail !== existing.email) {
          const emailSnap = await vendorCol.where('email', '==', cleanEmail).get();
          let conflict = false;
          emailSnap.forEach((d) => {
            if (String(d.data().id) !== String(vId)) conflict = true;
          });
          if (conflict) {
            return sendError(res, 'Email already in use by another account', null, 409);
          }
          updatedEmail = cleanEmail;
        }
      }

      const updates = {
        name: updatedName,
        email: updatedEmail,
        updated_at: new Date().toISOString(),
      };

      await docRef.set(updates, { merge: true });
      const updatedData = (await docRef.get()).data();
      const { password_hash, ...vendorSafe } = updatedData;

      await auditService.logAction({
        actorType: 'vendor',
        actorId: vId,
        action: 'Vendor Profile Updated',
        entityType: 'vendor',
        entityId: vId,
        ipAddress: req.ip,
      });

      return sendSuccess(res, 'Profile updated successfully', vendorSafe);
    } catch (err) {
      return sendError(res, 'Failed to update profile: ' + err.message, null, 500);
    }
  },

  /**
   * Upload profile photo
   */
  uploadPhoto: async (req, res) => {
    try {
      if (!req.file) {
        return sendError(res, 'No profile image file uploaded', null, 400);
      }

      const vId = Number(req.vendorId) || req.vendorId;
      let relativePath = `/uploads/profiles/${req.file.filename}`;

      // Upload to Firebase Storage bucket if configured
      if (config.FIREBASE_STORAGE_BUCKET) {
        try {
          const destination = `vendors/${vId}/profiles/${req.file.filename}`;
          const bucket = storage.bucket();
          const file = bucket.file(destination);
          await file.save(fs.readFileSync(req.file.path), {
            contentType: req.file.mimetype,
            public: true,
          });
          relativePath = `https://storage.googleapis.com/${config.FIREBASE_STORAGE_BUCKET}/${destination}`;
        } catch (_) {}
      }

      const vendorCol = db.collection(COLLECTIONS.VENDORS);
      let docRef = vendorCol.doc(String(vId));
      let doc = await docRef.get();
      if (!doc.exists) {
        const snap = await vendorCol.where('id', '==', vId).get();
        if (snap.empty) return sendError(res, 'Vendor not found', null, 404);
        docRef = snap.docs[0].ref;
      }

      await docRef.set({
        profile_photo: relativePath,
        updated_at: new Date().toISOString(),
      }, { merge: true });

      await auditService.logAction({
        actorType: 'vendor',
        actorId: vId,
        action: 'Profile Photo Uploaded',
        entityType: 'vendor',
        entityId: vId,
        details: { filename: req.file.filename, path: relativePath },
        ipAddress: req.ip,
      });

      return sendSuccess(res, 'Profile photo uploaded successfully', {
        profile_photo: relativePath,
      });
    } catch (err) {
      return sendError(res, 'Failed to upload photo: ' + err.message, null, 500);
    }
  },
};

module.exports = profileController;
