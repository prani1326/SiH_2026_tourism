const fdb = require('./firestoreDb');
const { v4: uuidv4 } = require('uuid');
const { NotFoundError } = require('../utils/errors');
const { getPagination, getPaginationMeta } = require('../utils/pagination');
const fcmService = require('./fcmService');

class NotificationService {
  async list(userId, query) {
    const { page, limit, offset } = getPagination(query);
    let allNotifications = await fdb.find('notifications');

    // Filter by user_id or system broadcast
    allNotifications = allNotifications.filter(
      (n) => n.user_id === userId || n.user_id === 'all' || !n.user_id
    );

    if (query.read === 'false') {
      allNotifications = allNotifications.filter((n) => !n.read || n.read === 0);
    }
    if (query.read === 'true') {
      allNotifications = allNotifications.filter((n) => n.read === 1 || n.read === true);
    }
    if (query.type) {
      allNotifications = allNotifications.filter((n) => n.type === query.type);
    }

    const total = allNotifications.length;
    allNotifications.sort((a, b) => {
      const pA = a.persistent && !a.acknowledged ? 0 : 1;
      const pB = b.persistent && !b.acknowledged ? 0 : 1;
      if (pA !== pB) return pA - pB;
      return (b.created_at || '') > (a.created_at || '') ? 1 : -1;
    });

    const data = allNotifications.slice(offset, offset + limit);
    const unread = allNotifications.filter((n) => !n.read || n.read === 0).length;

    return { data, pagination: getPaginationMeta(page, limit, total), unread_count: unread };
  }

  async markRead(notificationId, userId) {
    const notification = await fdb.findById('notifications', notificationId);
    if (!notification) throw new NotFoundError('Notification');

    await fdb.update('notifications', notificationId, {
      read: 1,
      read_at: new Date().toISOString(),
    });
    return { message: 'Marked as read' };
  }

  async acknowledge(notificationId, userId) {
    const notification = await fdb.findById('notifications', notificationId);
    if (!notification) throw new NotFoundError('Notification');

    const now = new Date().toISOString();
    await fdb.update('notifications', notificationId, {
      acknowledged: 1,
      acknowledged_at: now,
      read: 1,
      read_at: notification.read_at || now,
    });
    return { message: 'Acknowledged' };
  }

  async markAllRead(userId) {
    const now = new Date().toISOString();
    const all = await fdb.find('notifications');
    const userUnread = all.filter(
      (n) => (n.user_id === userId || n.user_id === 'all') && (!n.read || n.read === 0)
    );

    for (const n of userUnread) {
      await fdb.update('notifications', n.id, {
        read: 1,
        read_at: now,
      });
    }

    return { message: 'All notifications marked as read' };
  }

  async create(userId, data) {
    const id = uuidv4();
    const persistent = ['critical', 'emergency'].includes(data.priority);
    const notificationDoc = {
      id,
      user_id: userId,
      type: data.type,
      title: data.title,
      body: data.body || null,
      priority: data.priority || 'normal',
      persistent: persistent ? 1 : 0,
      source_type: data.source_type || null,
      source_id: data.source_id || null,
      action_url: data.action_url || null,
      read: 0,
      created_at: new Date().toISOString(),
    };

    await fdb.insert('notifications', notificationDoc, id);

    // Send FCM push notification
    if (userId && userId !== 'all') {
      fcmService.sendToUser(userId, data.title, data.body || '', { notificationId: id, ...data }).catch(() => {});
    } else {
      fcmService.sendToOpsLeaders(data.title, data.body || '', { notificationId: id, ...data }).catch(() => {});
    }

    return { id };
  }
}

module.exports = new NotificationService();
