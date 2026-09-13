import React, { useState, useMemo } from 'react';
import { 
  Plane, 
  MapPin, 
  Calendar, 
  Users, 
  Plus, 
  Search, 
  CheckCircle2, 
  Clock, 
  Compass, 
  Eye, 
  X, 
  IndianRupee, 
  ShieldCheck, 
  Navigation,
  Sparkles,
  FileText
} from 'lucide-react';
import PageHeader from '../components/PageHeader';
import KPIStatCard from '../components/KPIStatCard';
import StatusBadge from '../components/StatusBadge';
import DataTable from '../components/DataTable';
import Drawer from '../components/Drawer';

const DEFAULT_TRIPS = [
  { 
    id: 1, 
    trip_id: 'TRP-4001', 
    traveler_name: 'Aarav Sharma', 
    destination: 'Manali & Solang Valley', 
    start_date: '2026-09-05', 
    end_date: '2026-09-10', 
    number_of_travelers: 1, 
    budget: 25000, 
    itinerary: 'Day 1: Hadimba Temple, Day 2: Solang Paragliding, Day 3: Rafting, Day 4: Kasol Valley', 
    status: 'Active' 
  },
  { 
    id: 2, 
    trip_id: 'TRP-4002', 
    traveler_name: 'Ananya Roy', 
    destination: 'Goa (North & South)', 
    start_date: '2026-09-12', 
    end_date: '2026-09-17', 
    number_of_travelers: 4, 
    budget: 75000, 
    itinerary: 'Day 1: Beach Day Calangute, Day 2: Scuba Diving Grand Island, Day 3: Dudhsagar Waterfalls Trek', 
    status: 'Planned' 
  },
  { 
    id: 3, 
    trip_id: 'TRP-4003', 
    traveler_name: 'Vikramaditya Verma', 
    destination: 'Jaipur & Jaisalmer', 
    start_date: '2026-09-01', 
    end_date: '2026-09-06', 
    number_of_travelers: 2, 
    budget: 45000, 
    itinerary: 'Day 1-2: Jaipur Amber Fort & Hawa Mahal, Day 3-5: Jaisalmer Sam Sand Dunes Camp Safari', 
    status: 'Completed' 
  },
  { 
    id: 4, 
    trip_id: 'TRP-4004', 
    traveler_name: 'Meera Iyer', 
    destination: 'Kerala Backwaters & Alleppey', 
    start_date: '2026-09-20', 
    end_date: '2026-09-25', 
    number_of_travelers: 3, 
    budget: 38000, 
    itinerary: 'Day 1: Cochin heritage walk, Day 2: Deluxe Alleppey Houseboat cruise, Day 3: Munnar tea plantations', 
    status: 'Planned' 
  }
];

export default function TripPage({ trips = [], onViewDetail }) {
  const [statusFilter, setStatusFilter] = useState('all');
  const [selectedTrip, setSelectedTrip] = useState(null);
  const [selectedItineraryTrip, setSelectedItineraryTrip] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);

  // Active trips with fallback
  const activeTrips = useMemo(() => {
    if (Array.isArray(trips) && trips.length > 0) {
      return trips;
    }
    return DEFAULT_TRIPS;
  }, [trips]);

  const [localTrips, setLocalTrips] = useState(activeTrips);

  React.useEffect(() => {
    if (Array.isArray(trips) && trips.length > 0) {
      setLocalTrips(trips);
    }
  }, [trips]);

  // Metrics
  const totalTrips = localTrips.length;
  const activeCount = localTrips.filter(t => {
    const s = (t?.status || '').toLowerCase();
    return s === 'active' || s.includes('transit') || s.includes('ongoing');
  }).length;
  const plannedCount = localTrips.filter(t => {
    const s = (t?.status || '').toLowerCase();
    return s === 'planned' || s.includes('upcoming') || s.includes('scheduled');
  }).length;
  const completedCount = localTrips.filter(t => (t?.status || '').toLowerCase().includes('complete')).length;

  // Filter by status tab
  const filteredTrips = useMemo(() => {
    if (statusFilter === 'all') return localTrips;
    return localTrips.filter(t => {
      const st = (t?.status || '').toLowerCase();
      if (statusFilter === 'active') return st === 'active' || st.includes('transit') || st.includes('ongoing');
      if (statusFilter === 'planned') return st === 'planned' || st.includes('upcoming') || st.includes('scheduled');
      if (statusFilter === 'completed') return st.includes('complete');
      return true;
    });
  }, [localTrips, statusFilter]);

  // Destination Photo Helper
  const getTripPhoto = (dest) => {
    const d = (dest || '').toLowerCase();
    if (d.includes('manali')) return 'https://images.unsplash.com/photo-1548013146-72479768bada?w=200&auto=format&fit=crop&q=80';
    if (d.includes('goa')) return 'https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?w=200&auto=format&fit=crop&q=80';
    if (d.includes('jaipur') || d.includes('jaisalmer')) return 'https://images.unsplash.com/photo-1477587458883-47145ed94245?w=200&auto=format&fit=crop&q=80';
    if (d.includes('kerala')) return 'https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?w=200&auto=format&fit=crop&q=80';
    return 'https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=200&auto=format&fit=crop&q=80';
  };

  // Form for New Trip
  const [newTrip, setNewTrip] = useState({
    destination: '',
    traveler_name: '',
    start_date: '',
    end_date: '',
    number_of_travelers: 2,
    budget: 35000,
    itinerary: 'Day 1: Arrival & briefing. Day 2: Guided tour. Day 3: Leisure & Departure.',
    status: 'Planned'
  });

  const handleCreateTrip = (e) => {
    e.preventDefault();
    if (!newTrip.destination || !newTrip.traveler_name) return;
    const item = {
      trip_id: `TRP-${Date.now().toString().slice(-4)}`,
      ...newTrip
    };
    setLocalTrips([item, ...localTrips]);
    setShowCreateModal(false);
    setNewTrip({
      destination: '',
      traveler_name: '',
      start_date: '',
      end_date: '',
      number_of_travelers: 2,
      budget: 35000,
      itinerary: 'Day 1: Arrival & briefing. Day 2: Guided tour. Day 3: Leisure & Departure.',
      status: 'Planned'
    });
  };

  const columns = [
    { 
      key: 'trip_id', 
      label: 'Trip ID',
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
          {val || row?.trip_id || 'TRP-NA'}
        </span>
      )
    },
    { 
      key: 'destination', 
      label: 'Destination Circuit',
      render: (val, row) => {
        const destName = val || row?.destination || 'Expedition Circuit';
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <img 
              src={getTripPhoto(destName)} 
              alt={destName} 
              style={{ width: '38px', height: '38px', borderRadius: '8px', objectFit: 'cover' }} 
            />
            <div>
              <div style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{destName}</div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                <Calendar size={11} /> {row?.start_date || 'TBD'} → {row?.end_date || 'TBD'}
              </div>
            </div>
          </div>
        );
      }
    },
    { 
      key: 'traveler_name', 
      label: 'Traveler Lead',
      render: (val, row) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <div style={{
            width: '30px',
            height: '30px',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #3b82f6, #06b6d4)',
            color: '#ffffff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 700,
            fontSize: '0.72rem',
            flexShrink: 0
          }}>
            {String(val || 'T').slice(0, 2).toUpperCase()}
          </div>
          <div>
            <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.84rem' }}>{val || 'Traveler'}</div>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{row?.number_of_travelers || 1} Pax</div>
          </div>
        </div>
      )
    },
    { 
      key: 'budget', 
      label: 'Budget (INR)',
      render: (val, row) => {
        const amt = val ?? row?.budget;
        const formatted = typeof amt === 'number' ? amt.toLocaleString('en-IN') : (amt || '0');
        return (
          <span style={{ fontWeight: 700, color: 'var(--text-primary)', fontSize: '0.88rem' }}>
            ₹{formatted.toString().replace('₹', '')}
          </span>
        );
      }
    },
    { 
      key: 'itinerary', 
      label: 'Itinerary Schedule',
      render: (val, row) => (
        <button
          type="button"
          className="btn btn-outline btn-sm"
          style={{ padding: '3px 8px', fontSize: '0.72rem', display: 'flex', alignItems: 'center', gap: '4px' }}
          onClick={() => setSelectedItineraryTrip(row)}
        >
          <FileText size={12} color="var(--primary)" />
          <span>View Plan</span>
        </button>
      )
    },
    { 
      key: 'status', 
      label: 'Trip State',
      render: (val, row) => <StatusBadge status={val || row?.status || 'Planned'} />
    }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '22px' }}>
      {/* Top Page Header */}
      <PageHeader
        title="Circuit & Trip Dispatch Desk"
        description="Monitor multi-day itineraries, group sizes, guide allocations, and real-time transit state across tourism regions."
        breadcrumbs={['TravelHub', 'Trip Data']}
        primaryAction={{
          label: 'Plan New Trip',
          icon: Plus,
          onClick: () => setShowCreateModal(true)
        }}
        secondaryActions={
          <div style={{ display: 'flex', gap: '6px' }}>
            {[
              { id: 'all', label: 'All Circuits' },
              { id: 'active', label: 'Active Live' },
              { id: 'planned', label: 'Planned' },
              { id: 'completed', label: 'Completed' }
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
          label="Total Expeditions"
          value={totalTrips}
          change="+12% demand volume"
          isPositive={true}
          icon={Compass}
          scheme="primary"
          sparklineData={[1, 2, 2, 3, 3, 4, totalTrips]}
        />
        <KPIStatCard
          label="Active on Ground"
          value={activeCount}
          change="Circuits in-transit"
          isPositive={true}
          icon={Navigation}
          scheme="info"
          sparklineData={[0, 1, 1, 1, 2, 1, activeCount]}
        />
        <KPIStatCard
          label="Planned & Scheduled"
          value={plannedCount}
          change="Awaiting start date"
          isPositive={true}
          icon={Calendar}
          scheme="warning"
          sparklineData={[1, 1, 2, 2, 2, 2, plannedCount]}
        />
        <KPIStatCard
          label="Completed Excursions"
          value={completedCount}
          change="Zero stranded tours"
          isPositive={true}
          icon={CheckCircle2}
          scheme="success"
          sparklineData={[0, 1, 1, 1, 2, 2, completedCount]}
        />
      </div>

      {/* Trips Data Table */}
      <DataTable
        title="Central Trip Registry"
        columns={columns}
        data={filteredTrips}
        onViewDetail={(row) => {
          setSelectedTrip(row);
          if (onViewDetail) onViewDetail(row);
        }}
        searchPlaceholder="Search trip ID, destination, traveler..."
      />

      {/* Slide-over Trip Inspector Drawer */}
      <Drawer
        isOpen={!!selectedTrip}
        onClose={() => setSelectedTrip(null)}
        title={`Trip Record — ${selectedTrip?.trip_id || ''}`}
        subtitle="Full tour logistics, assigned suppliers, and travelers manifest"
        data={selectedTrip}
      />

      {/* Modal: View Full Itinerary */}
      {selectedItineraryTrip && (
        <div className="modal-overlay" onClick={() => setSelectedItineraryTrip(null)}>
          <div className="modal-card" style={{ maxWidth: '540px' }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Itinerary Breakdown</div>
                <h3 className="modal-title">{selectedItineraryTrip.destination}</h3>
              </div>
              <button type="button" className="modal-close" onClick={() => setSelectedItineraryTrip(null)}>
                <X size={18} />
              </button>
            </div>
            <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 14px', backgroundColor: 'var(--bg-surface-secondary)', borderRadius: '8px' }}>
                <div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Lead Traveler</div>
                  <div style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{selectedItineraryTrip.traveler_name}</div>
                </div>
                <div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Pax Count</div>
                  <div style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{selectedItineraryTrip.number_of_travelers} Travelers</div>
                </div>
                <div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Duration</div>
                  <div style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{selectedItineraryTrip.start_date} → {selectedItineraryTrip.end_date}</div>
                </div>
              </div>

              <div style={{ marginTop: '6px' }}>
                <div style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '8px' }}>Day-wise Plan</div>
                <div style={{ 
                  backgroundColor: 'var(--bg-surface)', 
                  border: '1px solid var(--border-color)', 
                  borderRadius: '8px', 
                  padding: '14px', 
                  fontSize: '0.84rem', 
                  color: 'var(--text-secondary)',
                  lineHeight: '1.6' 
                }}>
                  {selectedItineraryTrip.itinerary || 'No detailed day-wise itinerary entered.'}
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-primary btn-sm" onClick={() => setSelectedItineraryTrip(null)}>
                Close Preview
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Create Trip */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-card" style={{ maxWidth: '520px' }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <h3 className="modal-title">Schedule New Expedition</h3>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Dispatch tourist circuit to Central Operations</div>
              </div>
              <button type="button" className="modal-close" onClick={() => setShowCreateModal(false)}>
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleCreateTrip}>
              <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div>
                  <label className="form-label">Destination Circuit *</label>
                  <input
                    type="text"
                    required
                    className="form-control"
                    placeholder="e.g. Manali & Solang Valley"
                    value={newTrip.destination}
                    onChange={e => setNewTrip({ ...newTrip, destination: e.target.value })}
                  />
                </div>
                <div>
                  <label className="form-label">Lead Traveler Name *</label>
                  <input
                    type="text"
                    required
                    className="form-control"
                    placeholder="e.g. Aarav Sharma"
                    value={newTrip.traveler_name}
                    onChange={e => setNewTrip({ ...newTrip, traveler_name: e.target.value })}
                  />
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                  <div>
                    <label className="form-label">Start Date</label>
                    <input
                      type="date"
                      className="form-control"
                      value={newTrip.start_date}
                      onChange={e => setNewTrip({ ...newTrip, start_date: e.target.value })}
                    />
                  </div>
                  <div>
                    <label className="form-label">End Date</label>
                    <input
                      type="date"
                      className="form-control"
                      value={newTrip.end_date}
                      onChange={e => setNewTrip({ ...newTrip, end_date: e.target.value })}
                    />
                  </div>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                  <div>
                    <label className="form-label">Travelers (Pax)</label>
                    <input
                      type="number"
                      min="1"
                      className="form-control"
                      value={newTrip.number_of_travelers}
                      onChange={e => setNewTrip({ ...newTrip, number_of_travelers: Number(e.target.value) })}
                    />
                  </div>
                  <div>
                    <label className="form-label">Budget (INR)</label>
                    <input
                      type="number"
                      className="form-control"
                      value={newTrip.budget}
                      onChange={e => setNewTrip({ ...newTrip, budget: Number(e.target.value) })}
                    />
                  </div>
                </div>
                <div>
                  <label className="form-label">Itinerary Description</label>
                  <textarea
                    rows={3}
                    className="form-control"
                    value={newTrip.itinerary}
                    onChange={e => setNewTrip({ ...newTrip, itinerary: e.target.value })}
                  />
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowCreateModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Save & Dispatch Trip
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
