const fdb = require('./firestoreDb');
const { v4: uuidv4 } = require('uuid');
const { NotFoundError } = require('../utils/errors');
const fcmService = require('./fcmService');

class CommunicationService {
  async getMessages(query) {
    let allMessages = await fdb.find('messages');

    if (query.tourist_id) {
      allMessages = allMessages.filter((m) => m.tourist_id === query.tourist_id);
    }
    if (query.trip_id) {
      allMessages = allMessages.filter((m) => m.trip_id === query.trip_id);
    }
    if (query.channel) {
      allMessages = allMessages.filter((m) => m.channel === query.channel);
    }

    allMessages.sort((a, b) => ((b.created_at || '') > (a.created_at || '') ? 1 : -1));
    return allMessages.slice(0, 100);
  }

  async sendMessage(user, data) {
    const tourist = await fdb.findById('tourists', data.tourist_id);
    if (!tourist) throw new NotFoundError('Tourist');

    const id = uuidv4();
    const messageDoc = {
      id,
      tourist_id: data.tourist_id,
      trip_id: data.trip_id || null,
      sender_type: 'ops',
      sender_id: user.id,
      sender_name: user.full_name,
      channel: data.channel,
      content_type: data.content_type || 'text',
      content: data.content,
      priority: 'normal',
      status: 'sent',
      created_at: new Date().toISOString(),
    };

    await fdb.insert('messages', messageDoc, id);
    return { id, message: 'Message sent', channel: data.channel };
  }

  async sendEmergencyBroadcast(user, data) {
    let tourists = [];
    const allMembers = await fdb.find('trip_members');
    const allTourists = await fdb.find('tourists');
    const allTrips = await fdb.find('trips');

    if (data.trip_id) {
      const memberTouristIds = allMembers
        .filter((m) => m.trip_id === data.trip_id)
        .map((m) => m.tourist_id);
      tourists = allTourists.filter((t) => memberTouristIds.includes(t.id));
    } else if (data.destination) {
      const destQuery = data.destination.toLowerCase();
      const matchingTripIds = allTrips
        .filter((tr) => tr.status === 'active' && tr.destination && tr.destination.toLowerCase().includes(destQuery))
        .map((tr) => tr.id);
      const memberTouristIds = allMembers
        .filter((m) => matchingTripIds.includes(m.trip_id))
        .map((m) => m.tourist_id);
      tourists = allTourists.filter((t) => memberTouristIds.includes(t.id));
    }

    const now = new Date().toISOString();
    for (const tourist of tourists) {
      const id = uuidv4();
      await fdb.insert(
        'messages',
        {
          id,
          tourist_id: tourist.id,
          trip_id: data.trip_id || null,
          sender_type: 'ops',
          sender_id: user.id,
          sender_name: user.full_name,
          channel: 'emergency',
          content_type: 'emergency_instructions',
          content: data.message,
          priority: data.priority || 'high',
          status: 'sent',
          created_at: now,
        },
        id
      );
    }

    // Push FCM broadcast
    fcmService.sendToOpsLeaders(
      `EMERGENCY BROADCAST: [${data.priority || 'HIGH'}]`,
      data.message,
      { type: 'emergency_broadcast', count: tourists.length }
    ).catch(() => {});

    return { message: `Emergency broadcast sent to ${tourists.length} tourists`, count: tourists.length };
  }
}

module.exports = new CommunicationService();
