import React, { useState } from 'react';
import PageHeader from '../components/PageHeader';
import KPIStatCard from '../components/KPIStatCard';
import DataTable from '../components/DataTable';
import StatusBadge from '../components/StatusBadge';
import { Users, UserCheck, Clock, UserX, Plus, Shield, MapPin, Mail, Phone } from 'lucide-react';

export default function TravelerPage({ travelers = [], onViewDetail, onAddTraveler }) {
  const [filterStatus, setFilterStatus] = useState('all');
  const [showAddModal, setShowAddModal] = useState(false);
  const [newTravelerName, setNewTravelerName] = useState('');
  const [newTravelerEmail, setNewTravelerEmail] = useState('');
  const [newTravelerPhone, setNewTravelerPhone] = useState('+91 ');
  const [newTravelerProfile, setNewTravelerProfile] = useState('Solo Backpacker');
  const [blockedTravelers, setBlockedTravelers] = useState({});

  // Top KPIs
  const totalCount = travelers.length;
  const verifiedCount = travelers.filter(t => (t.trip_status || '').toLowerCase() !== 'idle').length;
  const pendingCount = totalCount - verifiedCount;
  const blockedCount = Object.keys(blockedTravelers).length;

  const toggleBlock = (travelerId) => {
    setBlockedTravelers(prev => ({
      ...prev,
      [travelerId]: !prev[travelerId]
    }));
  };

  const columns = [
    {
      key: 'name',
      label: 'Traveler Profile',
      render: (val, row) => {
        const initials = (val || 'T').split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();
        const isBlocked = blockedTravelers[row.traveler_id];
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div 
              className="user-avatar" 
              style={{
                background: isBlocked ? 'var(--danger)' : 'linear-gradient(135deg, #2563eb, #38bdf8)',
                width: '36px',
                height: '36px'
              }}
            >
              {initials}
            </div>
            <div>
              <div style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{val}</div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{row.traveler_id}</div>
            </div>
          </div>
        );
      }
    },
    {
      key: 'email',
      label: 'Contact Details',
      render: (val, row) => (
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.825rem', color: 'var(--text-primary)' }}>
            <Mail size={13} color="var(--text-muted)" />
            <span>{val || '—'}</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '2px' }}>
            <Phone size={12} color="var(--text-muted)" />
            <span>{row.phone || '—'}</span>
          </div>
        </div>
      )
    },
    {
      key: 'profile_info',
      label: 'Traveler Persona',
      render: (val) => (
        <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
          {val || 'General Traveler'}
        </span>
      )
    },
    {
      key: 'trips_count',
      label: 'Total Trips',
      render: (val) => (
        <span className="badge badge-secondary" style={{ fontWeight: 700 }}>
          {val || 0} Tours
        </span>
      )
    },
    {
      key: 'trip_status',
      label: 'Status',
      isBadge: true,
      render: (val, row) => {
        if (blockedTravelers[row.traveler_id]) {
          return <StatusBadge status="Blocked" />;
        }
        return <StatusBadge status={val || 'Active'} />;
      }
    },
    {
      key: 'created_at',
      label: 'Registered On',
      render: (val) => (
        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
          {val ? new Date(val).toLocaleDateString() : 'Recent'}
        </span>
      )
    },
    {
      key: 'actions',
      label: 'Safety Control',
      render: (_, row) => {
        const isBlocked = blockedTravelers[row.traveler_id];
        return (
          <button
            type="button"
            className={`btn btn-sm ${isBlocked ? 'btn-success' : 'btn-outline'}`}
            style={{ padding: '3px 8px', fontSize: '0.72rem' }}
            onClick={() => toggleBlock(row.traveler_id)}
          >
            {isBlocked ? 'Unblock' : 'Block Access'}
          </button>
        );
      }
    }
  ];

  const handleAddSubmit = (e) => {
    e.preventDefault();
    if (onAddTraveler) {
      onAddTraveler({
        name: newTravelerName,
        email: newTravelerEmail,
        phone: newTravelerPhone,
        profile_info: newTravelerProfile
      });
    }
    setShowAddModal(false);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <PageHeader
        title="Traveler Management Desk"
        description="Verify tourist identities, track ongoing trip itineraries, and manage mobile app permissions."
        breadcrumbs={['TravelHub', 'Travelers']}
        primaryAction={
          <button 
            type="button" 
            className="btn btn-primary"
            onClick={() => setShowAddModal(true)}
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <Plus size={15} />
            <span>Add Traveler</span>
          </button>
        }
      />

      {/* 4 KPI Cards */}
      <div className="stats-grid">
        <KPIStatCard
          title="Total Travelers"
          value={totalCount}
          icon={Users}
          trend="+18% MoM"
          isPositive={true}
          comparisonText="Registered accounts"
          colorScheme="primary"
          sparklineData={[5, 8, 9, 12, 14, 18, 20]}
        />
        <KPIStatCard
          title="Active Travelers"
          value={verifiedCount}
          icon={UserCheck}
          trend="In-field"
          isPositive={true}
          comparisonText="Currently on tour"
          colorScheme="success"
          sparklineData={[2, 3, 5, 4, 6, 7, 8]}
        />
        <KPIStatCard
          title="Idle / Inactive"
          value={pendingCount}
          icon={Clock}
          trend="Standby"
          isPositive={null}
          comparisonText="No active bookings"
          colorScheme="warning"
        />
        <KPIStatCard
          title="Blocked Accounts"
          value={blockedCount}
          icon={UserX}
          trend={blockedCount > 0 ? "Restricted" : "Clean"}
          isPositive={blockedCount === 0}
          comparisonText="Security flagged"
          colorScheme={blockedCount > 0 ? "danger" : "success"}
        />
      </div>

      {/* Main Table */}
      <DataTable
        title="Central Traveler Registry"
        subtitle="Live synchronization with Traveler Mobile App"
        columns={columns}
        data={travelers}
        onViewDetail={onViewDetail}
        searchPlaceholder="Search travelers by name, email, phone..."
      />

      {/* Add Traveler Modal */}
      {showAddModal && (
        <div className="modal-backdrop" onClick={() => setShowAddModal(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header" style={{ padding: '18px 24px', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between' }}>
              <h3 className="modal-title">Register New Traveler</h3>
              <button type="button" className="btn btn-outline btn-icon" onClick={() => setShowAddModal(false)}>✕</button>
            </div>
            <form onSubmit={handleAddSubmit} style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label className="field-label">Full Name</label>
                <input className="form-control" style={{ width: '100%' }} value={newTravelerName} onChange={e => setNewTravelerName(e.target.value)} required placeholder="e.g. Maya Patel" />
              </div>
              <div>
                <label className="field-label">Email Address</label>
                <input type="email" className="form-control" style={{ width: '100%' }} value={newTravelerEmail} onChange={e => setNewTravelerEmail(e.target.value)} required placeholder="maya.p@example.com" />
              </div>
              <div>
                <label className="field-label">Mobile Number</label>
                <input className="form-control" style={{ width: '100%' }} value={newTravelerPhone} onChange={e => setNewTravelerPhone(e.target.value)} required />
              </div>
              <div>
                <label className="field-label">Traveler Persona / Notes</label>
                <input className="form-control" style={{ width: '100%' }} value={newTravelerProfile} onChange={e => setNewTravelerProfile(e.target.value)} />
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button type="button" className="btn btn-outline" onClick={() => setShowAddModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save Traveler</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
