const fs = require('fs');
const path = require('path');
const config = require('../../config/env');
const { db, storage } = require('../../config/firebase');
const { COLLECTIONS } = require('../../database/firestoreSchema');
const { sendSuccess, sendError } = require('../../utils/responseHelper');
const { isValidEmail, isValidMobile, normalizeMobile, isValidCoordinate } = require('../../utils/validators');
const auditService = require('../../services/auditService');
const notificationService = require('../../services/notificationService');

const VALID_BUSINESS_TYPES = [
  'Tour Guide',
  'Tour Operator',
  'Travel Agency',
  'Transport',
  'Homestay',
  'Activity Provider',
  'Other',
];

const kycController = {
  /**
   * Get vendor KYC details and documents
   */
  getKyc: async (req, res) => {
    try {
      const vId = Number(req.vendorId) || req.vendorId;

      const kycSnap = await db.collection(COLLECTIONS.VENDOR_KYC).where('vendor_id', '==', vId).get();
      const kyc = !kycSnap.empty ? kycSnap.docs[0].data() : null;

      const docsSnap = await db.collection(COLLECTIONS.VENDOR_DOCUMENTS).where('vendor_id', '==', vId).get();
      const documents = docsSnap.docs.map((d) => d.data());
      documents.sort((a, b) => new Date(b.uploaded_at) - new Date(a.uploaded_at));

      const vendorSnap = await db.collection(COLLECTIONS.VENDORS).doc(String(vId)).get();
      let vendor = vendorSnap.exists ? vendorSnap.data() : null;
      if (!vendor) {
        const vQuery = await db.collection(COLLECTIONS.VENDORS).where('id', '==', vId).get();
        if (!vQuery.empty) vendor = vQuery.docs[0].data();
      }

      return sendSuccess(res, 'KYC details retrieved', {
        kyc_status: vendor ? vendor.kyc_status : 'pending',
        rejection_reason: vendor ? vendor.rejection_reason : null,
        verification_remarks: vendor ? vendor.verification_remarks : null,
        verified_at: vendor ? vendor.verified_at : null,
        details: kyc || null,
        documents: documents || [],
      });
    } catch (err) {
      return sendError(res, 'Failed to fetch KYC: ' + err.message, null, 500);
    }
  },

  /**
   * Submit / Save initial KYC
   */
  submitKyc: async (req, res) => {
    try {
      const {
        full_name,
        mobile,
        email,
        dob,
        business_name,
        business_type,
        pan_number,
        aadhaar_number,
        gst_number,
        tourism_license_no,
        address,
        city,
        state,
        pincode,
        latitude,
        longitude,
      } = req.body;

      if (!full_name || !mobile || !email || !dob || 
          !business_name || !business_type || !pan_number || !aadhaar_number || !tourism_license_no ||
          !address || !city || !state || !pincode) {
        return sendError(res, 'All details must be completely filled to verify KYC', null, 422);
      }

      if (business_type && !VALID_BUSINESS_TYPES.includes(business_type)) {
        return sendError(res, `Invalid business type. Must be one of: ${VALID_BUSINESS_TYPES.join(', ')}`, null, 422);
      }

      if (email && !isValidEmail(email)) {
        return sendError(res, 'Invalid email format', null, 422);
      }

      if (mobile && !isValidMobile(normalizeMobile(mobile))) {
        return sendError(res, 'Invalid mobile format', null, 422);
      }

      if (latitude && longitude && !isValidCoordinate(latitude, longitude)) {
        return sendError(res, 'Invalid GPS coordinates', null, 422);
      }

      const vId = Number(req.vendorId) || req.vendorId;
      const kycCol = db.collection(COLLECTIONS.VENDOR_KYC);
      const existingSnap = await kycCol.where('vendor_id', '==', vId).get();

      let kycDocId = String(vId);
      if (!existingSnap.empty) {
        kycDocId = existingSnap.docs[0].id;
      }

      const kycPayload = {
        id: Number(kycDocId) || vId,
        vendor_id: vId,
        full_name,
        mobile: mobile ? normalizeMobile(mobile) : null,
        email: email ? email.trim().toLowerCase() : null,
        dob: dob || null,
        business_name,
        business_type,
        pan_number: pan_number ? pan_number.trim().toUpperCase() : null,
        aadhaar_number: aadhaar_number ? aadhaar_number.trim() : null,
        gst_number: gst_number ? gst_number.trim().toUpperCase() : null,
        tourism_license_no: tourism_license_no ? tourism_license_no.trim() : null,
        address: address || null,
        city: city || null,
        state: state || null,
        pincode: pincode || null,
        latitude: latitude ? parseFloat(latitude) : null,
        longitude: longitude ? parseFloat(longitude) : null,
        status: 'pending',
        submitted_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      };

      await kycCol.doc(kycDocId).set(kycPayload, { merge: true });

      // Auto-verify KYC for MVP if completely filled
      const vendorCol = db.collection(COLLECTIONS.VENDORS);
      let vDocRef = vendorCol.doc(String(vId));
      let vDoc = await vDocRef.get();
      if (!vDoc.exists) {
        const vSnap = await vendorCol.where('id', '==', vId).get();
        if (!vSnap.empty) vDocRef = vSnap.docs[0].ref;
      }

      await vDocRef.set({
        kyc_status: 'approved',
        rejection_reason: null,
        verification_remarks: 'Auto-verified successfully',
        verified_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      }, { merge: true });

      await auditService.logAction({
        actorType: 'vendor',
        actorId: vId,
        action: 'KYC Submitted',
        entityType: 'vendor_kyc',
        entityId: kycPayload.id,
        ipAddress: req.ip,
      });

      try {
        await db.collection(COLLECTIONS.APP_ACTIVITIES).add({
          app_source: 'Vendor Mobile App',
          action: 'KYC Submitted',
          details: `Vendor VND-${vId} (${business_name || 'Business'}) submitted KYC details`,
          timestamp: new Date().toISOString(),
        });
      } catch (_) {}

      await notificationService.createNotification({
        recipientType: 'vendor',
        recipientId: vId,
        title: 'KYC Submitted',
        message: 'Your KYC application has been submitted and is currently under review by our admin team.',
        type: 'kyc_submitted',
      });

      return sendSuccess(res, 'KYC verified successfully', kycPayload, 201);
    } catch (err) {
      return sendError(res, 'Failed to submit KYC: ' + err.message, null, 500);
    }
  },

  /**
   * Update KYC details
   */
  updateKyc: async (req, res) => {
    return kycController.submitKyc(req, res);
  },

  /**
   * Upload KYC document
   */
  uploadDocument: async (req, res) => {
    try {
      if (!req.file) {
        return sendError(res, 'No document file uploaded', null, 400);
      }

      const { doc_type } = req.body;
      const validDocTypes = ['pan_card', 'aadhaar_card', 'business_reg', 'gst_cert', 'tourism_license', 'other'];

      if (!doc_type || !validDocTypes.includes(doc_type)) {
        return sendError(res, `doc_type is required and must be one of: ${validDocTypes.join(', ')}`, null, 422);
      }

      const vId = Number(req.vendorId) || req.vendorId;
      let relativePath = `/uploads/documents/${req.file.filename}`;

      // Upload to Firebase Storage if configured
      if (config.FIREBASE_STORAGE_BUCKET) {
        try {
          const destination = `vendors/${vId}/documents/${req.file.filename}`;
          const bucket = storage.bucket();
          const file = bucket.file(destination);
          await file.save(fs.readFileSync(req.file.path), {
            contentType: req.file.mimetype,
            public: true,
          });
          relativePath = `https://storage.googleapis.com/${config.FIREBASE_STORAGE_BUCKET}/${destination}`;
        } catch (_) {}
      }

      const docsCol = db.collection(COLLECTIONS.VENDOR_DOCUMENTS);
      const allDocsSnap = await docsCol.get();
      const nextId = allDocsSnap.size + 1;

      const docRecord = {
        id: nextId,
        vendor_id: vId,
        doc_type,
        file_path: relativePath,
        file_name: req.file.originalname,
        file_size: req.file.size,
        mime_type: req.file.mimetype,
        uploaded_at: new Date().toISOString(),
      };

      await docsCol.doc(String(nextId)).set(docRecord);

      await auditService.logAction({
        actorType: 'vendor',
        actorId: vId,
        action: 'Document Uploaded',
        entityType: 'vendor_document',
        entityId: nextId,
        details: { doc_type, filename: req.file.originalname },
        ipAddress: req.ip,
      });

      return sendSuccess(res, 'Document uploaded successfully', docRecord, 201);
    } catch (err) {
      return sendError(res, 'Failed to upload document: ' + err.message, null, 500);
    }
  },

  /**
   * Get KYC status summary
   */
  getKycStatus: async (req, res) => {
    try {
      const vId = Number(req.vendorId) || req.vendorId;
      const vendorCol = db.collection(COLLECTIONS.VENDORS);

      let vendor = null;
      const vDoc = await vendorCol.doc(String(vId)).get();
      if (vDoc.exists) {
        vendor = vDoc.data();
      } else {
        const snap = await vendorCol.where('id', '==', vId).get();
        if (!snap.empty) vendor = snap.docs[0].data();
      }

      if (!vendor) {
        return sendError(res, 'Vendor not found', null, 404);
      }

      const docsSnap = await db.collection(COLLECTIONS.VENDOR_DOCUMENTS).where('vendor_id', '==', vId).get();
      const documents = docsSnap.docs.map((d) => ({
        id: d.data().id,
        doc_type: d.data().doc_type,
        file_name: d.data().file_name,
        uploaded_at: d.data().uploaded_at,
      }));

      return sendSuccess(res, 'KYC status retrieved', {
        kyc_status: vendor.kyc_status,
        rejection_reason: vendor.rejection_reason || null,
        verification_remarks: vendor.verification_remarks || null,
        verified_at: vendor.verified_at || null,
        submitted_documents: documents,
      });
    } catch (err) {
      return sendError(res, 'Failed to fetch KYC status: ' + err.message, null, 500);
    }
  },
};

module.exports = kycController;
