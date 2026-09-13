const { db } = require('../../config/firebase');
const { COLLECTIONS } = require('../../database/firestoreSchema');
const { sendSuccess, sendError } = require('../../utils/responseHelper');
const auditService = require('../../services/auditService');
const notificationService = require('../../services/notificationService');

const adminVendorController = {
  /**
   * List all vendors with KYC status filters, search, and pagination
   */
  getVendors: async (req, res) => {
    try {
      const { kyc_status, status, search, page = 1, limit = 20 } = req.query;
      const pageNum = parseInt(page, 10) || 1;
      const limitNum = parseInt(limit, 10) || 20;

      const vendorsCol = db.collection(COLLECTIONS.VENDORS);
      const allVendorsSnap = await vendorsCol.get();

      let vendors = [];
      let pendingKyc = 0;
      let approvedKyc = 0;
      let rejectedKyc = 0;

      for (const vDoc of allVendorsSnap.docs) {
        const v = vDoc.data();
        if (v.kyc_status === 'pending') pendingKyc++;
        if (v.kyc_status === 'approved') approvedKyc++;
        if (v.kyc_status === 'rejected') rejectedKyc++;

        // Fetch KYC business info
        const kycSnap = await db.collection(COLLECTIONS.VENDOR_KYC).where('vendor_id', '==', Number(v.id) || v.id).get();
        let business_name = null;
        let business_type = null;
        let city = null;
        let state = null;
        if (!kycSnap.empty) {
          const kd = kycSnap.docs[0].data();
          business_name = kd.business_name;
          business_type = kd.business_type;
          city = kd.city;
          state = kd.state;
        }

        const docSnap = await db.collection(COLLECTIONS.VENDOR_DOCUMENTS).where('vendor_id', '==', Number(v.id) || v.id).get();
        const listSnap = await db.collection(COLLECTIONS.LISTINGS).where('vendor_id', '==', Number(v.id) || v.id).get();
        const bookSnap = await db.collection(COLLECTIONS.BOOKINGS).where('vendor_id', '==', Number(v.id) || v.id).get();

        const { password_hash, ...vendorSafe } = v;

        vendors.push({
          ...vendorSafe,
          business_name,
          business_type,
          city,
          state,
          document_count: docSnap.size,
          listing_count: listSnap.size,
          booking_count: bookSnap.size,
        });
      }

      if (kyc_status) {
        vendors = vendors.filter((v) => v.kyc_status === kyc_status);
      }

      if (status) {
        vendors = vendors.filter((v) => v.status === status);
      }

      if (search && search.trim()) {
        const q = search.trim().toLowerCase();
        vendors = vendors.filter((v) => {
          return (
            (v.name && v.name.toLowerCase().includes(q)) ||
            (v.email && v.email.toLowerCase().includes(q)) ||
            (v.mobile && v.mobile.includes(q)) ||
            (v.business_name && v.business_name.toLowerCase().includes(q))
          );
        });
      }

      vendors.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));

      const total = vendors.length;
      const offset = (pageNum - 1) * limitNum;
      const paged = vendors.slice(offset, offset + limitNum);

      return sendSuccess(res, 'Vendors retrieved successfully', {
        vendors: paged,
        counts: {
          total: allVendorsSnap.size,
          pending: pendingKyc,
          approved: approvedKyc,
          rejected: rejectedKyc,
        },
        page: pageNum,
        limit: limitNum,
      });
    } catch (err) {
      return sendError(res, 'Failed to list vendors: ' + err.message, null, 500);
    }
  },

  /**
   * Get single vendor application with full KYC data and uploaded documents
   */
  getVendorById: async (req, res) => {
    try {
      const { id } = req.params;

      const vendorsCol = db.collection(COLLECTIONS.VENDORS);
      let vDoc = await vendorsCol.doc(String(id)).get();
      let vendor = null;

      if (vDoc.exists) {
        vendor = vDoc.data();
      } else {
        const snap = await vendorsCol.where('id', '==', Number(id) || id).get();
        if (!snap.empty) vendor = snap.docs[0].data();
      }

      if (!vendor) {
        return sendError(res, 'Vendor not found', null, 404);
      }

      const vId = Number(vendor.id) || vendor.id;

      let verified_by_name = null;
      if (vendor.verified_by) {
        const staffDoc = await db.collection(COLLECTIONS.STAFF_USERS).doc(String(vendor.verified_by)).get();
        if (staffDoc.exists) verified_by_name = staffDoc.data().name;
      }

      const kycSnap = await db.collection(COLLECTIONS.VENDOR_KYC).where('vendor_id', '==', vId).get();
      const kyc = !kycSnap.empty ? kycSnap.docs[0].data() : null;

      const docsSnap = await db.collection(COLLECTIONS.VENDOR_DOCUMENTS).where('vendor_id', '==', vId).get();
      const documents = docsSnap.docs.map((d) => d.data());
      documents.sort((a, b) => new Date(b.uploaded_at) - new Date(a.uploaded_at));

      const listSnap = await db.collection(COLLECTIONS.LISTINGS).where('vendor_id', '==', vId).get();
      const listings = listSnap.docs.map((d) => d.data());

      const wSnap = await db.collection(COLLECTIONS.WALLETS).where('vendor_id', '==', vId).get();
      const wallet = !wSnap.empty ? wSnap.docs[0].data() : null;

      const { password_hash, ...vendorSafe } = vendor;

      return sendSuccess(res, 'Vendor details retrieved', {
        vendor: { ...vendorSafe, verified_by_name },
        kyc: kyc || null,
        documents: documents || [],
        listings: listings || [],
        wallet: wallet || null,
      });
    } catch (err) {
      return sendError(res, 'Failed to fetch vendor: ' + err.message, null, 500);
    }
  },

  /**
   * Verify KYC: Approve or Reject vendor application
   */
  verifyKyc: async (req, res) => {
    try {
      const { id } = req.params;
      const { action, remarks, rejection_reason } = req.body;

      if (!action || !['approve', 'reject'].includes(action.toLowerCase())) {
        return sendError(res, "Action is required and must be either 'approve' or 'reject'", null, 422);
      }

      const vendorsCol = db.collection(COLLECTIONS.VENDORS);
      let vDocRef = vendorsCol.doc(String(id));
      let vDoc = await vDocRef.get();

      if (!vDoc.exists) {
        const snap = await vendorsCol.where('id', '==', Number(id) || id).get();
        if (!snap.empty) {
          vDocRef = snap.docs[0].ref;
          vDoc = snap.docs[0];
        }
      }

      if (!vDoc.exists) {
        return sendError(res, 'Vendor not found', null, 404);
      }

      const vendor = vDoc.data();
      const isApprove = action.toLowerCase() === 'approve';

      if (!isApprove && (!rejection_reason || !rejection_reason.trim())) {
        return sendError(res, 'Rejection reason is required when rejecting KYC application', null, 422);
      }

      const newKycStatus = isApprove ? 'approved' : 'rejected';
      const reason = isApprove ? null : rejection_reason.trim();
      const verificationRemarks = remarks ? remarks.trim() : (isApprove ? 'KYC verified and approved by admin' : reason);

      await vDocRef.set({
        kyc_status: newKycStatus,
        status: 'active',
        rejection_reason: reason,
        verification_remarks: verificationRemarks,
        verified_at: new Date().toISOString(),
        verified_by: req.staffId,
        updated_at: new Date().toISOString(),
      }, { merge: true });

      // Update vendor_kyc if exists
      const vId = Number(vendor.id) || vendor.id;
      const kycSnap = await db.collection(COLLECTIONS.VENDOR_KYC).where('vendor_id', '==', vId).get();
      if (!kycSnap.empty) {
        await kycSnap.docs[0].ref.set({
          status: newKycStatus,
          updated_at: new Date().toISOString(),
        }, { merge: true });
      }

      await auditService.logAction({
        actorType: 'staff',
        actorId: req.staffId,
        action: isApprove ? 'KYC Approved' : 'KYC Rejected',
        entityType: 'vendor',
        entityId: vId,
        details: {
          vendor_name: vendor.name,
          action: newKycStatus,
          remarks: verificationRemarks,
          rejection_reason: reason,
        },
        ipAddress: req.ip,
      });

      await notificationService.createNotification({
        recipientType: 'vendor',
        recipientId: vId,
        title: isApprove ? 'KYC Approved!' : 'KYC Application Rejected',
        message: isApprove
          ? 'Congratulations! Your KYC application has been verified and approved. You can now create tour listings and accept bookings.'
          : `Your KYC application was rejected. Reason: ${reason}. You may update and resubmit your details.`,
        type: isApprove ? 'kyc_approved' : 'kyc_rejected',
      });

      const updatedVendor = (await vDocRef.get()).data();
      const { password_hash, ...safe } = updatedVendor;

      return sendSuccess(res, `Vendor KYC ${newKycStatus} successfully`, safe);
    } catch (err) {
      return sendError(res, 'Failed to verify KYC: ' + err.message, null, 500);
    }
  },
};

module.exports = adminVendorController;
