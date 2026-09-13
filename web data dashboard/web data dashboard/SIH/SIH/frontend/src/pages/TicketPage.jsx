import React, { useState, useMemo } from 'react';
import { 
  LifeBuoy, 
  AlertCircle, 
  Clock, 
  CheckCircle2, 
  Search, 
  Filter, 
  Plus, 
  Eye, 
  MessageSquare, 
  ShieldAlert, 
  User, 
  Check, 
  Sparkles,
  ArrowUpRight,
  X,
  Send
} from 'lucide-react';
import PageHeader from '../components/PageHeader';
import KPIStatCard from '../components/KPIStatCard';
import StatusBadge from '../components/StatusBadge';
import DataTable from '../components/DataTable';
import Drawer from '../components/Drawer';

const DEFAULT_TICKETS = [
  {
    id: 1,
    ticket_id: 'TCK-6003',
    reporter: 'Traveler: Rohan Deshmukh',
    issue: 'Lost mobile phone near Lonavala trekking trail head',
    category: 'Emergency',
    priority: 'High',
    assigned_leader: 'Amit Patel',
    status: 'Open',
    resolution: 'Ops field guide dispatched to search trail point',
    timestamp: '2026-09-02 18:45:00'
  },
  {
    id: 2,
    ticket_id: 'TCK-6001',
    reporter: 'Traveler: Aarav Sharma',
    issue: 'Weather delay on paragliding slot, timing reschedule request',
    category: 'Booking Request',
    priority: 'Medium',
    assigned_leader: 'Captain Suresh Menon',
    status: 'In Progress',
    resolution: 'Vendor agreed to move slot to 2:00 PM tomorrow',
    timestamp: '2026-09-02 10:30:00'
  },
  {
    id: 3,
    ticket_id: 'TCK-6002',
    reporter: 'Vendor: Ocean Breeze Watersports',
    issue: 'Traveler group 20 mins late for boat departure',
    category: 'Vendor Conflict',
    priority: 'Low',
    assigned_leader: 'Priya Sharma',
    status: 'Resolved',
    resolution: 'Rescheduled transfer to next boat departure batch',
    timestamp: '2026-09-01 14:15:00'
  },
  {
    id: 4,
    ticket_id: 'TCK-6004',
    reporter: 'Traveler: Meera Iyer',
    issue: 'Deluxe Houseboat check-in time confirmation & pick-up coordination',
    category: 'Hospitality',
    priority: 'Low',
    assigned_leader: 'Meera Nair',
    status: 'Resolved',
    resolution: 'Confirmed 12:00 PM check-in with Malabar Houseboat management.',
    timestamp: '2026-08-31 16:20:00'
  }
];

export default function TicketPage({ tickets = [], onViewDetail }) {
  const [statusFilter, setStatusFilter] = useState('all');
  const [selectedTicket, setSelectedTicket] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);

  const activeTickets = useMemo(() => {
    if (Array.isArray(tickets) && tickets.length > 0) {
      return tickets;
    }
    return DEFAULT_TICKETS;
  }, [tickets]);

  const [localTickets, setLocalTickets] = useState(activeTickets);

  React.useEffect(() => {
    if (Array.isArray(tickets) && tickets.length > 0) {
      setLocalTickets(tickets);
    }
  }, [tickets]);

  // KPIs
  const totalTickets = localTickets.length;
  const openTickets = localTickets.filter(t => (t?.status || '').toLowerCase() === 'open').length;
  const inProgressTickets = localTickets.filter(t => (t?.status || '').toLowerCase().includes('progress')).length;
  const resolvedTickets = localTickets.filter(t => (t?.status || '').toLowerCase().includes('resolve') || (t?.status || '').toLowerCase().includes('closed')).length;

  // Filtered by status tab
  const filteredTickets = useMemo(() => {
    if (statusFilter === 'all') return localTickets;
    return localTickets.filter(t => {
      const st = (t?.status || '').toLowerCase();
      if (statusFilter === 'open') return st === 'open';
      if (statusFilter === 'in_progress') return st.includes('progress');
      if (statusFilter === 'resolved') return st.includes('resolve') || st.includes('closed');
      return true;
    });
  }, [localTickets, statusFilter]);

  // Form for New Ticket
  const [newTicket, setNewTicket] = useState({
    reporter: '',
    issue: '',
    category: 'Booking Request',
    priority: 'High',
    assigned_leader: 'Captain Suresh Menon'
  });

  const handleCreateTicket = (e) => {
    e.preventDefault();
    if (!newTicket.issue) return;
    const item = {
      ticket_id: `TCK-${Date.now().toString().slice(-4)}`,
      status: 'Open',
      timestamp: new Date().toISOString().slice(0, 19).replace('T', ' '),
      resolution: 'Ticket logged by Central Operations. Assigned to field guide.',
      ...newTicket
    };
    setLocalTickets([item, ...localTickets]);
    setShowCreateModal(false);
    setNewTicket({
      reporter: '',
      issue: '',
      category: 'Booking Request',
      priority: 'High',
      assigned_leader: 'Captain Suresh Menon'
    });
  };

  const handleUpdateStatus = (ticketId, newStatus) => {
    setLocalTickets(localTickets.map(t => 
      t.ticket_id === ticketId ? { ...t, status: newStatus } : t
    ));
    if (selectedTicket && selectedTicket.ticket_id === ticketId) {
      setSelectedTicket({ ...selectedTicket, status: newStatus });
    }
  };

  const columns = [
    { 
      key: 'ticket_id', 
      label: 'Ticket ID',
      render: (val, row) => (
        <span style={{ 
          fontFamily: 'monospace', 
          fontWeight: 700, 
          color: 'var(--primary)', 
          backgroundColor: 'var(--primary-light)', 
          padding: '3px 8px', 
          borderRadius: '6px',
          fontSize: '0.8rem'
        }}>
          {val || row?.ticket_id || 'TCK-NA'}
        </span>
      )
    },
    { 
      key: 'issue', 
      label: 'Subject / Issue Description',
      render: (val, row) => (
        <div style={{ maxWidth: '320px' }}>
          <div style={{ fontWeight: 600, color: 'var(--text-primary)', lineHeight: 1.3 }}>
            {val || row?.issue || 'Operations Inquiry'}
          </div>
          <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '2px' }}>
            Category: <span style={{ color: 'var(--primary)', fontWeight: 600 }}>{row?.category || 'Support'}</span>
          </div>
        </div>
      )
    },
    { 
      key: 'priority', 
      label: 'Priority',
      render: (val, row) => {
        const p = String(val || row?.priority || 'Medium').toLowerCase();
        const color = 
          p === 'critical' ? '#ef4444' :
          p === 'high' ? '#f97316' :
          p === 'medium' ? '#3b82f6' :
          '#64748b';
        const bg = 
          p === 'critical' ? 'rgba(239, 68, 68, 0.12)' :
          p === 'high' ? 'rgba(249, 115, 22, 0.12)' :
          p === 'medium' ? 'rgba(59, 130, 246, 0.12)' :
          'rgba(100, 116, 139, 0.12)';

        return (
          <span style={{ 
            backgroundColor: bg, 
            color: color, 
            padding: '3px 8px', 
            borderRadius: '12px', 
            fontSize: '0.72rem', 
            fontWeight: 700,
            textTransform: 'uppercase',
            letterSpacing: '0.3px',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '4px'
          }}>
            <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: color }} />
            {val || row?.priority || 'Medium'}
          </span>
        );
      }
    },
    { 
      key: 'reporter', 
      label: 'Reporter Source',
      render: (val, row) => (
        <div style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-primary)' }}>
          {val || row?.reporter || 'Traveler'}
        </div>
      )
    },
    { 
      key: 'assigned_leader', 
      label: 'Assigned Lead Guide',
      render: (val, row) => (
        <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
          {val || row?.assigned_leader || 'Operations Desk'}
        </div>
      )
    },
    { 
      key: 'status', 
      label: 'Ticket State',
      render: (val, row) => <StatusBadge status={val || row?.status || 'Open'} />
    },
    { 
      key: 'timestamp', 
      label: 'Reported At',
      render: (val, row) => (
        <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
          {val || row?.timestamp || 'Recent'}
        </span>
      )
    }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '22px' }}>
      {/* Top Page Header */}
      <PageHeader
        title="Incident & Support Desk"
        description="Track ground escalations, itinerary reschedules, medical emergencies, and vendor dispute resolutions."
        breadcrumbs={['TravelHub', 'Tickets / Incidents']}
        primaryAction={{
          label: 'Create Support Ticket',
          icon: Plus,
          onClick: () => setShowCreateModal(true)
        }}
        secondaryActions={
          <div style={{ display: 'flex', gap: '6px' }}>
            {[
              { id: 'all', label: 'All Tickets' },
              { id: 'open', label: `Open (${openTickets})` },
              { id: 'in_progress', label: `In Progress (${inProgressTickets})` },
              { id: 'resolved', label: `Resolved (${resolvedTickets})` }
            ].map(tab => (
              <button
                key={tab.id}
                type="button"
                className={`btn btn-sm ${statusFilter === tab.id ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => setStatusFilter(tab.id)}
                style={{ fontSize: '0.78rem' }}
              >
                {tab.label}
              </button>
            ))}
          </div>
        }
      />

      {/* KPI Cards */}
      <div className="stats-grid">
        <KPIStatCard
          label="Total Incident Tickets"
          value={totalTickets}
          change="+3 last month"
          isPositive={true}
          icon={LifeBuoy}
          scheme="primary"
          sparklineData={[1, 2, 2, 3, 3, 4, totalTickets]}
        />
        <KPIStatCard
          label="Open & Actionable"
          value={openTickets}
          change={openTickets > 0 ? "Requires ops response" : "Inbox cleared"}
          isPositive={openTickets === 0}
          icon={AlertCircle}
          scheme="danger"
          sparklineData={[2, 2, 1, 2, 1, 1, openTickets]}
        />
        <KPIStatCard
          label="In Progress"
          value={inProgressTickets}
          change="Assigned to field guides"
          isPositive={true}
          icon={Clock}
          scheme="warning"
          sparklineData={[1, 1, 2, 1, 2, 1, inProgressTickets]}
        />
        <KPIStatCard
          label="Resolved & Closed"
          value={resolvedTickets}
          change="92% resolution rate"
          isPositive={true}
          icon={CheckCircle2}
          scheme="success"
          sparklineData={[1, 1, 2, 2, 2, 3, resolvedTickets]}
        />
      </div>

      {/* Tickets Table */}
      <DataTable
        title="Central Escalations & Support Records"
        columns={columns}
        data={filteredTickets}
        onViewDetail={(row) => setSelectedTicket(row)}
        searchPlaceholder="Search ticket ID, issue, reporter, category..."
      />

      {/* Slide-over Ticket Resolution Modal */}
      {selectedTicket && (
        <div className="modal-overlay" onClick={() => setSelectedTicket(null)}>
          <div className="modal-card" style={{ maxWidth: '560px' }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ fontFamily: 'monospace', fontWeight: 700, color: 'var(--primary)', backgroundColor: 'var(--primary-light)', padding: '2px 6px', borderRadius: '4px', fontSize: '0.8rem' }}>
                    {selectedTicket.ticket_id}
                  </span>
                  <StatusBadge status={selectedTicket.status || 'Open'} />
                </div>
                <h3 className="modal-title" style={{ marginTop: '6px' }}>
                  {selectedTicket.issue}
                </h3>
              </div>
              <button type="button" className="modal-close" onClick={() => setSelectedTicket(null)}>
                <X size={18} />
              </button>
            </div>

            <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', padding: '12px', backgroundColor: 'var(--bg-surface-secondary)', borderRadius: '8px' }}>
                <div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Reporter</div>
                  <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.85rem' }}>{selectedTicket.reporter}</div>
                </div>
                <div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Category</div>
                  <div style={{ fontWeight: 600, color: 'var(--primary)', fontSize: '0.85rem' }}>{selectedTicket.category}</div>
                </div>
                <div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Assigned Guide</div>
                  <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.85rem' }}>{selectedTicket.assigned_leader}</div>
                </div>
                <div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Priority Level</div>
                  <div style={{ fontWeight: 700, color: '#f97316', fontSize: '0.85rem' }}>{selectedTicket.priority}</div>
                </div>
              </div>

              <div>
                <div style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '4px' }}>
                  Resolution Notes & Dispatch Log
                </div>
                <div style={{ padding: '12px', backgroundColor: 'var(--bg-surface)', border: '1px solid var(--border-color)', borderRadius: '8px', fontSize: '0.82rem', color: 'var(--text-secondary)', lineHeight: '1.5' }}>
                  {selectedTicket.resolution || 'No operational notes attached yet.'}
                </div>
              </div>

              <div>
                <div style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '6px' }}>
                  Update State
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                  {['Open', 'In Progress', 'Resolved'].map(st => (
                    <button
                      key={st}
                      type="button"
                      className={`btn btn-sm ${selectedTicket.status === st ? 'btn-primary' : 'btn-outline'}`}
                      onClick={() => handleUpdateStatus(selectedTicket.ticket_id, st)}
                    >
                      Mark as {st}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <button type="button" className="btn btn-secondary" onClick={() => setSelectedTicket(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Create Ticket */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-card" style={{ maxWidth: '500px' }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <h3 className="modal-title">Log New Incident / Ticket</h3>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Escalate ground issue to central dispatch</div>
              </div>
              <button type="button" className="modal-close" onClick={() => setShowCreateModal(false)}>
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleCreateTicket}>
              <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div>
                  <label className="form-label">Issue Subject *</label>
                  <input
                    type="text"
                    required
                    className="form-control"
                    placeholder="e.g. Flight delay at Srinagar - cab rescheduling"
                    value={newTicket.issue}
                    onChange={e => setNewTicket({ ...newTicket, issue: e.target.value })}
                  />
                </div>
                <div>
                  <label className="form-label">Reporter *</label>
                  <input
                    type="text"
                    required
                    className="form-control"
                    placeholder="e.g. Traveler: Rahul Sen"
                    value={newTicket.reporter}
                    onChange={e => setNewTicket({ ...newTicket, reporter: e.target.value })}
                  />
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                  <div>
                    <label className="form-label">Category</label>
                    <select
                      className="form-control"
                      value={newTicket.category}
                      onChange={e => setNewTicket({ ...newTicket, category: e.target.value })}
                    >
                      <option value="Booking Request">Booking Request</option>
                      <option value="Logistics">Logistics</option>
                      <option value="Medical">Medical</option>
                      <option value="Hospitality">Hospitality</option>
                      <option value="Emergency">Emergency</option>
                    </select>
                  </div>
                  <div>
                    <label className="form-label">Priority</label>
                    <select
                      className="form-control"
                      value={newTicket.priority}
                      onChange={e => setNewTicket({ ...newTicket, priority: e.target.value })}
                    >
                      <option value="Low">Low</option>
                      <option value="Medium">Medium</option>
                      <option value="High">High</option>
                      <option value="Critical">Critical</option>
                    </select>
                  </div>
                </div>
                <div>
                  <label className="form-label">Assign Guide Lead</label>
                  <select
                    className="form-control"
                    value={newTicket.assigned_leader}
                    onChange={e => setNewTicket({ ...newTicket, assigned_leader: e.target.value })}
                  >
                    <option value="Captain Suresh Menon">Captain Suresh Menon (Himachal)</option>
                    <option value="Priya Sharma">Priya Sharma (Goa)</option>
                    <option value="Amit Patel">Amit Patel (Rajasthan)</option>
                  </select>
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowCreateModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Log Incident Ticket
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
