const { sendError } = require('../utils/responseHelper');

/**
 * Middleware to restrict operational features to vendors with approved KYC status
 */
const requireKycApproved = (req, res, next) => {
  if (!req.vendor) {
    return sendError(res, 'Unauthorized access', null, 401);
  }

  if (req.vendor.kyc_status !== 'approved') {
    return sendError(
      res,
      'Your KYC verification is pending or rejected. Operational access (listings, bookings, trips, tracking, wallet) is disabled until KYC is approved.',
      {
        kyc_status: req.vendor.kyc_status,
        rejection_reason: req.vendor.rejection_reason || null,
        verification_remarks: req.vendor.verification_remarks || null,
      },
      403
    );
  }

  next();
};

module.exports = { requireKycApproved };
