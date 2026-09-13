const fdb = require('./firestoreDb');
const { v4: uuidv4 } = require('uuid');
const { NotFoundError, BadRequestError } = require('../utils/errors');
const { getPagination, getPaginationMeta } = require('../utils/pagination');

class TeamService {
  async list(query) {
    const { page, limit, offset } = getPagination(query);
    let allUsers = await fdb.find('users');
    allUsers = allUsers.filter((u) => u.status !== 'deactivated');

    if (query.role) {
      allUsers = allUsers.filter((u) => u.role === query.role);
    }
    if (query.region) {
      allUsers = allUsers.filter((u) => u.region === query.region);
    }
    if (query.status) {
      allUsers = allUsers.filter((u) => u.status === query.status);
    }

    const total = allUsers.length;
    allUsers.sort((a, b) => ((a.full_name || '') > (b.full_name || '') ? 1 : -1));
    const paged = allUsers.slice(offset, offset + limit);

    const allAvailability = await fdb.find('team_availability');
    const data = paged.map((u) => {
      const ta = allAvailability.find((a) => a.user_id === u.id) || {};
      return {
        id: u.id,
        email: u.email,
        phone: u.phone,
        full_name: u.full_name,
        role: u.role,
        status: u.status,
        region: u.region,
        last_login: u.last_login,
        created_at: u.created_at,
        available: ta.available,
        workload_score: ta.workload_score,
        active_cases: ta.active_cases,
        performance_score: ta.performance_score,
        last_activity: ta.last_activity,
      };
    });

    return { data, pagination: getPaginationMeta(page, limit, total) };
  }

  async getById(id) {
    const user = await fdb.findById('users', id);
    if (!user) throw new NotFoundError('Team Member');

    const ta = (await fdb.findOne('team_availability', [['user_id', '==', id]])) || {};

    const allAssignments = await fdb.find('team_assignments', [
      ['user_id', '==', id],
      ['status', '==', 'active'],
    ]);
    allAssignments.sort((a, b) => ((b.created_at || '') > (a.created_at || '') ? 1 : -1));

    const recentActivity = await fdb.find('audit_log', [['user_id', '==', id]], {
      orderBy: ['created_at', 'desc'],
      limit: 20,
    });

    return {
      id: user.id,
      email: user.email,
      phone: user.phone,
      full_name: user.full_name,
      role: user.role,
      status: user.status,
      region: user.region,
      last_login: user.last_login,
      created_at: user.created_at,
      available: ta.available,
      workload_score: ta.workload_score,
      active_cases: ta.active_cases,
      performance_score: ta.performance_score,
      last_activity: ta.last_activity,
      active_cases_detail: allAssignments,
      recent_activity: recentActivity,
    };
  }

  async assignCase(userId, caseType, caseId, assignedBy) {
    const user = await fdb.findById('users', userId);
    if (!user) throw new NotFoundError('Team Member');

    const id = uuidv4();
    const now = new Date().toISOString();

    await fdb.insert(
      'team_assignments',
      {
        id,
        user_id: userId,
        case_type: caseType,
        case_id: caseId,
        status: 'active',
        assigned_by: assignedBy.id,
        created_at: now,
      },
      id
    );

    const activeCount = await fdb.count('team_assignments', [
      ['user_id', '==', userId],
      ['status', '==', 'active'],
    ]);

    const existingTa = await fdb.findOne('team_availability', [['user_id', '==', userId]]);
    if (existingTa) {
      await fdb.update('team_availability', existingTa.id, {
        active_cases: activeCount,
        last_activity: now,
        updated_at: now,
      });
    } else {
      const taId = uuidv4();
      await fdb.insert(
        'team_availability',
        {
          id: taId,
          user_id: userId,
          available: 1,
          active_cases: activeCount,
          last_activity: now,
          updated_at: now,
        },
        taId
      );
    }

    return { id, message: `Case assigned to ${user.full_name}` };
  }

  async reassignCase(assignmentId, newUserId, user) {
    const assignment = await fdb.findById('team_assignments', assignmentId);
    if (!assignment) throw new NotFoundError('Assignment');

    const newUser = await fdb.findById('users', newUserId);
    if (!newUser) throw new NotFoundError('Team Member');

    await fdb.update('team_assignments', assignmentId, { status: 'reassigned' });

    const id = uuidv4();
    await fdb.insert(
      'team_assignments',
      {
        id,
        user_id: newUserId,
        case_type: assignment.case_type,
        case_id: assignment.case_id,
        status: 'active',
        assigned_by: user.id,
        created_at: new Date().toISOString(),
      },
      id
    );

    return { id, message: `Case reassigned to ${newUser.full_name}` };
  }

  async updateAvailability(userId, available, region) {
    const existing = await fdb.findOne('team_availability', [['user_id', '==', userId]]);
    const now = new Date().toISOString();

    if (existing) {
      const updates = {
        available: available ? 1 : 0,
        updated_at: now,
      };
      if (region) updates.region = region;
      await fdb.update('team_availability', existing.id, updates);
    } else {
      const id = uuidv4();
      await fdb.insert(
        'team_availability',
        {
          id,
          user_id: userId,
          available: available ? 1 : 0,
          region: region || null,
          updated_at: now,
        },
        id
      );
    }

    return { message: 'Availability updated' };
  }

  async getPendingApprovals() {
    const users = await fdb.find('users', [['status', '==', 'pending_approval']]);
    users.sort((a, b) => ((b.created_at || '') > (a.created_at || '') ? 1 : -1));
    return users.map((u) => ({
      id: u.id,
      email: u.email,
      phone: u.phone,
      full_name: u.full_name,
      role: u.role,
      created_at: u.created_at,
    }));
  }

  async approveAccount(userId, approver, role, region) {
    const user = await fdb.findById('users', userId);
    if (!user || user.status !== 'pending_approval') {
      throw new NotFoundError('Pending Account');
    }

    const now = new Date().toISOString();
    await fdb.update('users', userId, {
      status: 'active',
      role,
      region: region || null,
      approved_by: approver.id,
      approved_at: now,
      updated_at: now,
    });

    const taId = uuidv4();
    await fdb.insert(
      'team_availability',
      {
        id: taId,
        user_id: userId,
        available: 1,
        region: region || null,
        updated_at: now,
      },
      taId
    );

    return { message: 'Account approved' };
  }
}

module.exports = new TeamService();
