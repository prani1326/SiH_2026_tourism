import React, { useState, useMemo } from 'react';
import { 
  ShieldAlert, 
  AlertTriangle, 
  CheckCircle2, 
  Clock, 
  Phone, 
  MapPin, 
  Radio, 
  User, 
  Search, 
  Plus, 
  Eye, 
  Send, 
  ShieldCheck, 
  Activity,
  HeartPulse,
  Flame,
  CloudLightning,
  X,
  Navigation
} from 'lucide-react';
import PageHeader from '../components/PageHeader';
import KPIStatCard from '../components/KPIStatCard';
import StatusBadge from '../components/StatusBadge';
import DataTable from '../components/DataTable';
import Drawer from '../components/Drawer';

const DEFAULT_SAFETY_EVENTS = [
  {
    id: 1,
    event_id: 'EMG-7001',
    event_type: 'SOS Event',
    user_name: 'Rohan Deshmukh',
    contact_number: '+91 9556677889',
    location: 'Lonavala Tiger Point Trail (Lat: 18.755, Long: 73.408)',
    active_incidents: 'Phone dropped on steep trail, traveler safe with group. Ground rescue guide dispatched.',
    resolution_status: 'Dispatched',
    created_at: '2026-09-02 18:40:00'
  },
  {
    id: 2,
    event_id: 'EMG-7002',
    event_type: 'Lost-phone Event',
    user_name: 'Vikramaditya Verma',
    contact_number: '+91 9988776655',
    location: 'Jaisalmer Fort Inner Market',
    active_incidents: 'Phone lost in market, recovered by tourist police assistance booth.',
    resolution_status: 'Resolved',
    created_at: '2026-09-01 21:10:00'
  },
  {
    id: 3,
    event_id: 'EMG-7003',
    event_type: 'Emergency Request',
    user_name: 'Ananya Roy',
    contact_number: '+91 9123456789',
    location: 'Calangute Beach Road, Goa',
    active_incidents: 'First-aid assistance required for minor ankle sprain. Administered by local lifeguard booth.',
    resolution_status: 'Resolved',
    created_at: '2026-08-30 16:20:00'
  },
  {
    id: 4,
    event_id: 'EMG-7004',
    event_type: 'Acute Altitude Sickness',
    user_name: 'Rahul Sen',
    contact_number: '+91 9876543210',
    location: 'Khardung La Pass, Ladakh',
    active_incidents: 'Oxygen saturation drop to 78%. High-altitude military transit medical camp notified.',
    resolution_status: 'Active',
    created_at: '2026-09-06 14:20:00'
  }
];

export default function SafetyPage({ safetyEvents = [], onViewDetail }) {
  const [statusFilter, setStatusFilter] = useState('all');
  const [selectedIncident, setSelectedIncident] = useState(null);
  const [showBroadcastModal, setShowBroadcastModal] = useState(false);
  const [broadcastMsg, setBroadcastMsg] = useState('Safety advisory: Heavy rainfall forecast along Kullu-Manali highway. All tours adhere to safe shelter protocols.');
  const [broadcastSent, setBroadcastSent] = useState(false);

  const activeEvents = useMemo(() => {
    if (Array.isArray(safetyEvents) && safetyEvents.length > 0) {
      return safetyEvents;
    }
    return DEFAULT_SAFETY_EVENTS;
  }, [safetyEvents]);

  const [localEvents, setLocalEvents] = useState(activeEvents);

  React.useEffect(() => {
    if (Array.isArray(safetyEvents) && safetyEvents.length > 0) {
      setLocalEvents(safetyEvents);
    }
  }, [safetyEvents]);

  // Metrics
  const totalIncidents = localEvents.length;
  const activeSOS = localEvents.filter(e => {
    const s = (e?.resolution_status || '').toLowerCase();
    return s === 'active' || s === 'open';
  }).length;
  const dispatchedCount = localEvents.filter(e => {
    const s = (e?.resolution_status || '').toLowerCase();
    return s.includes('dispatch') || s.includes('investigat');
  }).length;
  const resolvedCount = localEvents.filter(e => (e?.resolution_status || '').toLowerCase().includes('resolve')).length;

  // Filtered by status tab
  const filteredEvents = useMemo(() => {
    if (statusFilter === 'all') return localEvents;
    return localEvents.filter(e => {
      const st = (e?.resolution_status || 'Active').toLowerCase();
      if (statusFilter === 'active') return st === 'active' || st === 'open';
      if (statusFilter === 'dispatched') return st.includes('dispatch') || st.includes('investigat');
      if (statusFilter === 'resolved') return st.includes('resolve');
      return true;
    });
  }, [localEvents, statusFilter]);

  const handleResolve = (eventId) => {
    setLocalEvents(localEvents.map(ev => 
      ev.event_id === eventId ? { ...ev, resolution_status: 'Resolved' } : ev
    ));
    if (selectedIncident && selectedIncident.event_id === eventId) {
      setSelectedIncident({ ...selectedIncident, resolution_status: 'Resolved' });
    }
  };

  const handleSendBroadcast = (e) => {
    e.preventDefault();
    setBroadcastSent(true);
    setTimeout(() => {
      setBroadcastSent(false);
      setShowBroadcastModal(false);
    }, 2500);
  };

  const columns = [
    { 
      key: 'event_id', 
      label: 'Incident ID',
      render: (val, row) => (
        <span style={{ 
          fontFamily: 'monospace', 
          fontWeight: 700, 
          color: 'var(--danger)', 
          backgroundColor: 'rgba(239, 68, 68, 0.1)', 
          padding: '3px 8px', 
          borderRadius: '6px',
          fontSize: '0.8rem'
        }}>
          {val || row?.event_id || 'EVT-NA'}
        </span>
      )
    },
    { 
      key: 'event_type', 
      label: 'Incident Category',
      render: (val, row) => {
        const typeStr = String(val || row?.event_type || 'SOS').toLowerCase();
        let Icon = ShieldAlert;
        if (typeStr.includes('medic') || typeStr.includes('sickness') || typeStr.includes('health')) Icon = HeartPulse;
        if (typeStr.includes('weather') || typeStr.includes('flood') || typeStr.includes('storm')) Icon = CloudLightning;
        if (typeStr.includes('lost') || typeStr.includes('phone')) Icon = Phone;

        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div style={{
              width: '28px',
              height: '28px',
              borderRadius: '6px',
              backgroundColor: 'rgba(239, 68, 68, 0.1)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <Icon size={15} color="var(--danger)" />
            </div>
            <strong style={{ color: 'var(--text-primary)', fontSize: '0.84rem' }}>
              {val || row?.event_type || 'Emergency SOS'}
            </strong>
          </div>
        );
      }
    },
    { 
      key: 'user_name', 
      label: 'Traveler & Contact',
      render: (val, row) => (
        <div>
          <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{val || row?.user_name || 'Traveler'}</div>
          <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '4px', marginTop: '2px' }}>
            <Phone size={11} color="var(--primary)" /> {row?.contact_number || '+91 9876543210'}
          </div>
        </div>
      )
    },
    { 
      key: 'location', 
      label: 'Live GPS Coordinates / Trail',
      render: (val, row) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '5px', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
          <MapPin size={13} color="#ef4444" style={{ flexShrink: 0 }} />
          <span>{val || row?.location || 'High Altitude Pass'}</span>
        </div>
      )
    },
    { 
      key: 'active_incidents', 
      label: 'Situation Report',
      render: (val, row) => (
        <div style={{ maxWidth: '280px', fontSize: '0.78rem', color: 'var(--text-secondary)', lineHeight: 1.3 }}>
          {val || row?.active_incidents || 'Monitoring in progress.'}
        </div>
      )
    },
    { 
      key: 'resolution_status', 
      label: 'Dispatch State',
      render: (val, row) => <StatusBadge status={val || row?.resolution_status || 'Active'} />
    },
    { 
      key: 'created_at', 
      label: 'Timestamp',
      render: (val, row) => (
        <span style={{ fontSize: '0.76rem', color: 'var(--text-muted)' }}>
          {val || row?.created_at || 'Recent'}
        </span>
      )
    }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '22px' }}>
      {/* Top Page Header */}
      <PageHeader
        title="Safety Operations & SOS Emergency Radar"
        description="Monitor real-time SOS distress alerts, emergency team dispatches, high-altitude medical issues, and issue territory weather warnings."
        breadcrumbs={['TravelHub', 'Safety / Emergency']}
        primaryAction={{
          label: 'Broadcast Region Warning',
          icon: Radio,
          onClick: () => setShowBroadcastModal(true)
        }}
        secondaryActions={
          <div style={{ display: 'flex', gap: '6px' }}>
            {[
              { id: 'all', label: 'All Incidents' },
              { id: 'active', label: `Active SOS (${activeSOS})` },
              { id: 'dispatched', label: `Dispatched (${dispatchedCount})` },
              { id: 'resolved', label: `Resolved (${resolvedCount})` }
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
          label="Total Tracked Incidents"
          value={totalIncidents}
          change="Logged this season"
          isPositive={true}
          icon={ShieldAlert}
          scheme="primary"
          sparklineData={[1, 2, 2, 3, 3, 4, totalIncidents]}
        />
        <KPIStatCard
          label="Active SOS Alerts"
          value={activeSOS}
          change={activeSOS > 0 ? "Immediate attention" : "All clear on radar"}
          isPositive={activeSOS === 0}
          icon={AlertTriangle}
          scheme="danger"
          sparklineData={[1, 2, 1, 2, 1, 1, activeSOS]}
        />
        <KPIStatCard
          label="Emergency Response Dispatched"
          value={dispatchedCount}
          change="Guides on ground"
          isPositive={true}
          icon={Navigation}
          scheme="warning"
          sparklineData={[0, 1, 1, 2, 1, 1, dispatchedCount]}
        />
        <KPIStatCard
          label="Safely Resolved"
          value={resolvedCount}
          change="Zero casualties"
          isPositive={true}
          icon={CheckCircle2}
          scheme="success"
          sparklineData={[1, 1, 2, 2, 2, 3, resolvedCount]}
        />
      </div>

      {/* Live Safety Radar Alert Banner */}
      {activeSOS > 0 && (
        <div style={{
          backgroundColor: 'rgba(239, 68, 68, 0.08)',
          border: '1px solid rgba(239, 68, 68, 0.3)',
          borderRadius: '10px',
          padding: '14px 20px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '12px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{ width: '10px', height: '10px', borderRadius: '50%', backgroundColor: '#ef4444', animation: 'pulse 1.5s infinite' }} />
            <div>
              <strong style={{ color: '#ef4444', fontSize: '0.9rem' }}>Active Emergency Signal In Progress</strong>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                Field guide assigned. High altitude oxygen evacuation underway at Khardung La Pass.
              </div>
            </div>
          </div>
          <button
            type="button"
            className="btn btn-sm btn-danger"
            onClick={() => setStatusFilter('active')}
          >
            Isolate Active SOS
          </button>
        </div>
      )}

      {/* Safety Events Table */}
      <DataTable
        title="Central Safety & SOS Event Log"
        columns={columns}
        data={filteredEvents}
        onViewDetail={(row) => setSelectedIncident(row)}
        searchPlaceholder="Search event ID, traveler, category, location..."
      />

      {/* Slide-over Incident Resolution Drawer / Modal */}
      {selectedIncident && (
        <div className="modal-overlay" onClick={() => setSelectedIncident(null)}>
          <div className="modal-card" style={{ maxWidth: '560px' }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ fontFamily: 'monospace', fontWeight: 700, color: 'var(--danger)', backgroundColor: 'rgba(239, 68, 68, 0.1)', padding: '2px 6px', borderRadius: '4px', fontSize: '0.8rem' }}>
                    {selectedIncident.event_id}
                  </span>
                  <StatusBadge status={selectedIncident.resolution_status || 'Active'} />
                </div>
                <h3 className="modal-title" style={{ marginTop: '6px' }}>
                  {selectedIncident.event_type}
                </h3>
              </div>
              <button type="button" className="modal-close" onClick={() => setSelectedIncident(null)}>
                <X size={18} />
              </button>
            </div>

            <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', padding: '12px', backgroundColor: 'var(--bg-surface-secondary)', borderRadius: '8px' }}>
                <div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Traveler</div>
                  <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.85rem' }}>{selectedIncident.user_name}</div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{selectedIncident.contact_number}</div>
                </div>
                <div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Location</div>
                  <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.85rem' }}>{selectedIncident.location}</div>
                </div>
              </div>

              <div>
                <div style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '4px' }}>
                  Incident Description & Field Dispatch
                </div>
                <div style={{ padding: '12px', backgroundColor: 'var(--bg-surface)', border: '1px solid var(--border-color)', borderRadius: '8px', fontSize: '0.82rem', color: 'var(--text-secondary)', lineHeight: '1.5' }}>
                  {selectedIncident.active_incidents}
                </div>
              </div>

              {selectedIncident.resolution_status !== 'Resolved' && (
                <div style={{ marginTop: '4px' }}>
                  <button
                    type="button"
                    className="btn btn-success btn-sm"
                    onClick={() => handleResolve(selectedIncident.event_id)}
                  >
                    <CheckCircle2 size={14} />
                    <span>Mark Incident as Safely Resolved</span>
                  </button>
                </div>
              )}
            </div>

            <div className="modal-footer">
              <button type="button" className="btn btn-secondary" onClick={() => setSelectedIncident(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Broadcast Warning */}
      {showBroadcastModal && (
        <div className="modal-overlay" onClick={() => setShowBroadcastModal(false)}>
          <div className="modal-card" style={{ maxWidth: '480px' }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <h3 className="modal-title">Regional Weather / Safety Warning</h3>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  Sends broadcast SMS and in-app alerts to all tourists in zone
                </div>
              </div>
              <button type="button" className="modal-close" onClick={() => setShowBroadcastModal(false)}>
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleSendBroadcast}>
              <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {broadcastSent && (
                  <div style={{ padding: '10px 14px', backgroundColor: 'rgba(16, 185, 129, 0.1)', color: '#065f46', borderRadius: '6px', fontSize: '0.82rem', fontWeight: 600 }}>
                    Emergency broadcast sent to 8 registered tourists in Kullu-Manali sector!
                  </div>
                )}
                <div>
                  <label className="form-label">Warning Content *</label>
                  <textarea
                    rows={4}
                    required
                    className="form-control"
                    value={broadcastMsg}
                    onChange={e => setBroadcastMsg(e.target.value)}
                  />
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowBroadcastModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-danger" disabled={broadcastSent}>
                  <Send size={14} />
                  <span>Transmit Broadcast</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
