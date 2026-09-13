const { sendSuccess, sendError } = require('../../utils/responseHelper');
const auditService = require('../../services/auditService');

const adminAuditController = {
  /**
   * Get system audit logs
   */
  getLogs: async (req, res) => {
    try {
      const { page = 1, limit = 50, actorType, entityType } = req.query;
      const data = await auditService.getLogs({
        page: parseInt(page, 10),
        limit: parseInt(limit, 10),
        actorType,
        entityType,
      });

      return sendSuccess(res, 'Audit logs retrieved successfully', data);
    } catch (err) {
      return sendError(res, 'Failed to fetch audit logs: ' + err.message, null, 500);
    }
  },
};

module.exports = adminAuditController;
