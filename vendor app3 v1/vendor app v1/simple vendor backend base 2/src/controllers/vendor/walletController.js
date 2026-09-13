const { sendSuccess, sendError } = require('../../utils/responseHelper');
const walletService = require('../../services/walletService');

const walletController = {
  /**
   * Get vendor wallet overview
   */
  getWallet: async (req, res) => {
    try {
      const summary = await walletService.getSummary(req.vendorId);
      const recentTransactions = await walletService.getTransactions(req.vendorId, { page: 1, limit: 10 });

      return sendSuccess(res, 'Wallet details retrieved', {
        ...summary,
        recent_transactions: recentTransactions.transactions,
      });
    } catch (err) {
      return sendError(res, 'Failed to fetch wallet: ' + err.message, null, 500);
    }
  },

  /**
   * Get paginated transaction history
   */
  getTransactions: async (req, res) => {
    try {
      const { page = 1, limit = 20, type } = req.query;
      const data = await walletService.getTransactions(req.vendorId, {
        page: parseInt(page, 10),
        limit: parseInt(limit, 10),
        type,
      });

      return sendSuccess(res, 'Transactions retrieved successfully', data);
    } catch (err) {
      return sendError(res, 'Failed to fetch transactions: ' + err.message, null, 500);
    }
  },

  /**
   * Get single transaction details
   */
  getTransactionById: async (req, res) => {
    try {
      const { id } = req.params;
      const transaction = await walletService.getTransactionById(req.vendorId, id);

      if (!transaction) {
        return sendError(res, 'Transaction not found or unauthorized', null, 404);
      }

      return sendSuccess(res, 'Transaction details retrieved', transaction);
    } catch (err) {
      return sendError(res, 'Failed to fetch transaction: ' + err.message, null, 500);
    }
  },
};

module.exports = walletController;
