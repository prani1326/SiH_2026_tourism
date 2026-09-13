const fdb = require('./firestoreDb');
const { v4: uuidv4 } = require('uuid');
const { NotFoundError, BadRequestError } = require('../utils/errors');
const { getPagination, getPaginationMeta } = require('../utils/pagination');

class SupportService {
  async listTickets(query) {
    const { page, limit, offset } = getPagination(query);
    let allTickets = await fdb.find('support_tickets');

    if (query.category) {
      allTickets = allTickets.filter((t) => t.category === query.category);
    }
    if (query.priority) {
      allTickets = allTickets.filter((t) => t.priority === query.priority);
    }
    if (query.status) {
      allTickets = allTickets.filter((t) => t.status === query.status);
    }
    if (query.assigned_ops) {
      allTickets = allTickets.filter((t) => t.assigned_ops_id === query.assigned_ops);
    }
    if (query.tourist_id) {
      allTickets = allTickets.filter((t) => t.tourist_id === query.tourist_id);
    }

    const total = allTickets.length;
    const priorityOrder = { critical: 1, high: 2, medium: 3, low: 4 };
    allTickets.sort((a, b) => {
      const pA = priorityOrder[a.priority] || 4;
      const pB = priorityOrder[b.priority] || 4;
      if (pA !== pB) return pA - pB;
      return (b.created_at || '') > (a.created_at || '') ? 1 : -1;
    });

    const paged = allTickets.slice(offset, offset + limit);

    const data = await Promise.all(
      paged.map(async (t) => {
        const tourist = (await fdb.findById('tourists', t.tourist_id)) || {};
        return {
          ...t,
          tourist_name: tourist.full_name || t.tourist_name,
        };
      })
    );

    return { data, pagination: getPaginationMeta(page, limit, total) };
  }

  async getTicketById(id) {
    const ticket = await fdb.findById('support_tickets', id);
    if (!ticket) throw new NotFoundError('Support Ticket');

    const tourist = (await fdb.findById('tourists', ticket.tourist_id)) || {};
    const messages = await fdb.find('ticket_messages', [['ticket_id', '==', id]], {
      orderBy: ['created_at', 'asc'],
    });
    const notes = await fdb.find('ticket_notes', [['ticket_id', '==', id]], {
      orderBy: ['created_at', 'desc'],
    });

    return {
      ...ticket,
      tourist_name: tourist.full_name || ticket.tourist_name,
      tourist_email: tourist.email,
      tourist_phone: tourist.phone,
      messages,
      notes,
    };
  }

  async assignTicket(id, opsLeaderId, user) {
    await this._getTicket(id);
    const ops = await fdb.findById('users', opsLeaderId);
    if (!ops) throw new NotFoundError('Ops Leader');

    const now = new Date().toISOString();
    await fdb.update('support_tickets', id, {
      assigned_ops_id: ops.id,
      assigned_ops_name: ops.full_name,
      status: 'in_progress',
      updated_at: now,
    });

    return { message: 'Ticket assigned', assigned_to: ops.full_name };
  }

  async addReply(id, user, message, attachments = []) {
    await this._getTicket(id);
    const msgId = uuidv4();
    const now = new Date().toISOString();

    await fdb.insert(
      'ticket_messages',
      {
        id: msgId,
        ticket_id: id,
        sender_type: 'ops',
        sender_id: user.id,
        sender_name: user.full_name,
        message,
        attachments: typeof attachments === 'string' ? attachments : JSON.stringify(attachments),
        created_at: now,
      },
      msgId
    );

    await fdb.update('support_tickets', id, {
      status: 'in_progress',
      updated_at: now,
    });

    return { id: msgId, message: 'Reply added' };
  }

  async updateStatus(id, status, user, reason) {
    await this._getTicket(id);
    const now = new Date().toISOString();
    const updates = {
      status,
      updated_at: now,
    };
    if (status === 'resolved' || status === 'closed') {
      updates.resolved_at = now;
    }

    await fdb.update('support_tickets', id, updates);
    await this._addNote(id, user, `Status changed to '${status}'. ${reason || ''}`, 'general');

    return { message: `Ticket status updated to '${status}'` };
  }

  async updatePriority(id, priority, user, reason) {
    await this._getTicket(id);
    const now = new Date().toISOString();

    await fdb.update('support_tickets', id, {
      priority,
      updated_at: now,
    });
    await this._addNote(id, user, `Priority changed to '${priority}'. ${reason || ''}`, 'general');

    return { message: `Priority updated to '${priority}'` };
  }

  async getNotes(id) {
    await this._getTicket(id);
    return fdb.find('ticket_notes', [['ticket_id', '==', id]], {
      orderBy: ['created_at', 'desc'],
    });
  }

  async addNote(id, user, content, type = 'general') {
    await this._getTicket(id);
    return this._addNote(id, user, content, type);
  }

  async resolveTicket(id, user, resolution) {
    await this._getTicket(id);
    const now = new Date().toISOString();
    await fdb.update('support_tickets', id, {
      status: 'resolved',
      resolution: resolution || 'Resolved',
      resolved_at: now,
      updated_at: now,
    });
    return { message: 'Ticket resolved' };
  }

  async _getTicket(id) {
    const ticket = await fdb.findById('support_tickets', id);
    if (!ticket) throw new NotFoundError('Support Ticket');
    return ticket;
  }

  async _addNote(ticketId, user, content, type) {
    const id = uuidv4();
    await fdb.insert(
      'ticket_notes',
      {
        id,
        ticket_id: ticketId,
        created_by: user.id,
        created_by_name: user.full_name,
        content,
        type,
        created_at: new Date().toISOString(),
      },
      id
    );
    return { id, message: 'Note added' };
  }
}

module.exports = new SupportService();
