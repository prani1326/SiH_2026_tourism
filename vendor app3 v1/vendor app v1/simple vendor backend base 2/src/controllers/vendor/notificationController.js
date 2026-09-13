const { sendSuccess, sendError } = require('../../utils/responseHelper');
const notificationService = require('../../services/notificationService');

const notificationController = {
  /**
   * Get vendor notifications
   */
  getNotifications: async (req, res) => {
    try {
      const { page = 1, limit = 20, unread } = req.query;
      const data = await notificationService.getNotifications('vendor', req.vendorId, {
        page: parseInt(page, 10),
        limit: parseInt(limit, 10),
        unreadOnly: unread === 'true' || unread === '1',
      });

      return sendSuccess(res, 'Notifications retrieved successfully', data);
    } catch (err) {
      return sendError(res, 'Failed to fetch notifications: ' + err.message, null, 500);
    }
  },

  /**
   * Mark single notification as read
   */
  markAsRead: async (req, res) => {
    try {
      const { id } = req.params;
      const updated = await notificationService.markAsRead(id, 'vendor', req.vendorId);

      if (!updated) {
        return sendError(res, 'Notification not found or unauthorized', null, 404);
      }

      return sendSuccess(res, 'Notification marked as read');
    } catch (err) {
      return sendError(res, 'Failed to update notification: ' + err.message, null, 500);
    }
  },

  /**
   * Mark all notifications as read
   */
  markAllAsRead: async (req, res) => {
    try {
      const count = await notificationService.markAllAsRead('vendor', req.vendorId);
      return sendSuccess(res, `${count} notification(s) marked as read`);
    } catch (err) {
      return sendError(res, 'Failed to update notifications: ' + err.message, null, 500);
    }
  },
};

module.exports = notificationController;
