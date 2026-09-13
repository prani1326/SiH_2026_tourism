const { db, messaging } = require('../config/firebase');
const { COLLECTIONS } = require('../database/firestoreSchema');
const logger = require('../utils/logger');

const notificationService = {
  createNotification: async ({ recipientType = 'vendor', recipientId, title, message, type = 'general', fcmToken = null }) => {
    try {
      const rId = Number(recipientId) || recipientId;
      const col = db.collection(COLLECTIONS.NOTIFICATIONS);
      const allSnap = await col.get();
      const nextId = allSnap.size + 1;

      const newNotification = {
        id: nextId,
        recipient_type: recipientType,
        recipient_id: rId,
        title,
        message,
        type,
        is_read: 0,
        created_at: new Date().toISOString(),
      };

      await col.doc(String(nextId)).set(newNotification);

      // Dispatch FCM Push Notification if token is available
      if (fcmToken) {
        try {
          await messaging.send({
            token: fcmToken,
            notification: {
              title,
              body: message,
            },
            data: {
              type,
              notification_id: String(nextId),
            },
          });
          logger.info(`FCM push notification sent to recipient ${recipientId}`);
        } catch (fcmErr) {
          logger.warn(`FCM send failed (non-fatal): ${fcmErr.message}`);
        }
      }

      return newNotification;
    } catch (err) {
      logger.error('Notification creation error: ' + err.message);
    }
  },

  getNotifications: async (recipientType, recipientId, { page = 1, limit = 20, unreadOnly = false } = {}) => {
    const rId = Number(recipientId) || recipientId;
    const pageNum = parseInt(page, 10) || 1;
    const limitNum = parseInt(limit, 10) || 20;

    let query = db.collection(COLLECTIONS.NOTIFICATIONS)
      .where('recipient_type', '==', recipientType)
      .where('recipient_id', '==', rId);

    const snap = await query.get();
    let allDocs = snap.docs.map((d) => d.data());

    if (unreadOnly) {
      allDocs = allDocs.filter((d) => d.is_read === 0 || d.is_read === false);
    }

    const total = allDocs.length;
    const unreadCount = allDocs.filter((d) => d.is_read === 0 || d.is_read === false).length;

    allDocs.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));

    const offset = (pageNum - 1) * limitNum;
    const paged = allDocs.slice(offset, offset + limitNum);

    return {
      notifications: paged,
      total,
      unread_count: unreadCount,
      page: pageNum,
      limit: limitNum,
    };
  },

  markAsRead: async (notificationId, recipientType, recipientId) => {
    const rId = Number(recipientId) || recipientId;
    const docRef = db.collection(COLLECTIONS.NOTIFICATIONS).doc(String(notificationId));
    const doc = await docRef.get();

    if (!doc.exists) return false;
    const data = doc.data();

    if (data.recipient_type !== recipientType || (Number(data.recipient_id) !== rId && String(data.recipient_id) !== String(rId))) {
      return false;
    }

    await docRef.set({ is_read: 1, updated_at: new Date().toISOString() }, { merge: true });
    return true;
  },

  markAllAsRead: async (recipientType, recipientId) => {
    const rId = Number(recipientId) || recipientId;
    const snap = await db.collection(COLLECTIONS.NOTIFICATIONS)
      .where('recipient_type', '==', recipientType)
      .where('recipient_id', '==', rId)
      .get();

    let count = 0;
    const batch = db.batch();

    snap.forEach((doc) => {
      const data = doc.data();
      if (data.is_read === 0 || data.is_read === false) {
        batch.set(db.collection(COLLECTIONS.NOTIFICATIONS).doc(doc.id), { is_read: 1 }, { merge: true });
        count++;
      }
    });

    await batch.commit();
    return count;
  },
};

module.exports = notificationService;
