const { messaging, isRealFirebase } = require('../config/firebase');
const logger = require('../utils/logger');
const fdb = require('./firestoreDb');

class FcmService {
  /**
   * Dispatch push notification to device FCM token
   */
  async sendPushNotification(fcmToken, title, body, data = {}) {
    if (!fcmToken) {
      return { success: false, error: 'FCM token required' };
    }

    if (isRealFirebase() && messaging) {
      try {
        const messagePayload = {
          token: fcmToken,
          notification: { title, body },
          data: Object.fromEntries(Object.entries(data).map(([k, v]) => [k, String(v)])),
        };
        const messageId = await messaging.send(messagePayload);
        logger.info(`FCM message dispatched successfully: ${messageId}`);
        return { success: true, messageId };
      } catch (err) {
        logger.error(`FCM message dispatch failed: ${err.message}`);
        return { success: false, error: err.message };
      }
    }

    // Offline / local development fallback
    const simulatedId = `simulated_fcm_${Date.now()}_${Math.random().toString(36).substr(2, 6)}`;
    logger.info(`[SIMULATED FCM] To: ${fcmToken} | Title: "${title}" | Body: "${body}"`);
    return { success: true, messageId: simulatedId, simulated: true };
  }

  /**
   * Send notification to user by User ID (looks up user's registered FCM tokens)
   */
  async sendToUser(userId, title, body, data = {}) {
    const user = await fdb.findById('users', userId);
    if (!user) return { success: false, error: 'User not found' };

    const fcmToken = user.fcm_token;
    if (!fcmToken) {
      return { success: false, error: 'User has no registered FCM token' };
    }

    return this.sendPushNotification(fcmToken, title, body, data);
  }

  /**
   * Send notification to all Ops Leaders
   */
  async sendToOpsLeaders(title, body, data = {}) {
    const opsLeaders = await fdb.find('users', [['role', '==', 'ops_leader']]);
    const results = [];
    for (const leader of opsLeaders) {
      if (leader.fcm_token) {
        const res = await this.sendPushNotification(leader.fcm_token, title, body, data);
        results.push({ userId: leader.id, ...res });
      }
    }
    return results;
  }

  /**
   * Send broadcast notification to topic subscribers
   */
  async sendToTopic(topic, title, body, data = {}) {
    if (!topic) return { success: false, error: 'Topic required' };

    if (isRealFirebase() && messaging) {
      try {
        const messagePayload = {
          topic,
          notification: { title, body },
          data: Object.fromEntries(Object.entries(data).map(([k, v]) => [k, String(v)])),
        };
        const messageId = await messaging.send(messagePayload);
        logger.info(`FCM topic broadcast dispatched: ${topic} (${messageId})`);
        return { success: true, messageId, topic };
      } catch (err) {
        logger.error(`FCM topic broadcast failed: ${err.message}`);
        return { success: false, error: err.message };
      }
    }

    const simulatedId = `simulated_topic_${Date.now()}`;
    logger.info(`[SIMULATED FCM TOPIC] Topic: ${topic} | Title: "${title}" | Body: "${body}"`);
    return { success: true, messageId: simulatedId, topic, simulated: true };
  }
}

module.exports = new FcmService();
