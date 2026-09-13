import React, { useState, useMemo, useEffect } from 'react';
import { 
  MapPin, 
  CheckCircle2, 
  Clock, 
  Plus, 
  Eye, 
  Search, 
  LayoutGrid, 
  List, 
  Phone, 
  Hotel, 
  Utensils, 
  Compass, 
  Sparkles,
  Download,
  Building2,
  Navigation
} from 'lucide-react';
import PageHeader from '../components/PageHeader';
import KPIStatCard from '../components/KPIStatCard';
import StatusBadge from '../components/StatusBadge';
import DataTable from '../components/DataTable';

const DEFAULT_DESTINATIONS = [
  {
    id: 1,
    destination_id: 'DST-1001',
    destination_name: 'Manali & Solang Valley',
    places_pois: 'Hadimba Temple, Solang Valley Adventure Park, Jogini Waterfall, Rohtang Pass',
    hotels: 'Himalayan Heights Resort, Snow Valley Stay, Pinecrest Cottages',
    activities: 'Paragliding, River Rafting, Snow Trekking, Camping',
    restaurants: 'Cafe 1947, Johnson’s Cafe, Chopsticks Restaurant',
    emergency_help_info: 'District Hospital: 01902-252250, Police: 112, Mountain Rescue: +91 9816000000',
    content_status: 'Published',
    created_at: '2026-08-01 10:00:00'
  },
  {
    id: 2,
    destination_id: 'DST-1002',
    destination_name: 'Goa (North & South)',
    places_pois: 'Calangute Beach, Fort Aguada, Dudhsagar Waterfalls, Palolem Beach',
    hotels: 'Taj Exotica, Ocean Palms Resort, Zostel Anjuna',
    activities: 'Scuba Diving, Banana Boat Ride, Casino Cruise, Heritage Walk',
    restaurants: 'Britto’s, Thalassa, Curlies, Martin’s Corner',
    emergency_help_info: 'Tourism Helpline: 1364, Calangute Police: 0832-2278223, Ambulance: 108',
    content_status: 'Published',
    created_at: '2026-08-05 11:30:00'
  },
  {
    id: 3,
    destination_id: 'DST-1003',
    destination_name: 'Jaipur & Jaisalmer',
    places_pois: 'Amber Fort, Hawa Mahal, Sam Sand Dunes, Jaisalmer Fort',
    hotels: 'Chokhi Dhani Resort, Suryagarh Palace, Jaisalmer Desert Camp',
    activities: 'Desert Safari, Quad Biking, Parasailing, Heritage Walking Tour',
    restaurants: '1135 AD, Rawat Mishthan Bhandar, The Trio',
    emergency_help_info: 'Tourist Police Helpline: 0141-2821000, SMS Emergency: 1090',
    content_status: 'Published',
    created_at: '2026-08-10 14:20:00'
  }
];

export default function ContentPage({ destinations = [], onViewDetail }) {
  const [viewMode, setViewMode] = useState('table'); // Default to 'table' matching Traveler & Vendor pages
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [showAddModal, setShowAddModal] = useState(false);

  // Use props if available, otherwise use default destinations so page is NEVER empty
  const activeDestinations = (Array.isArray(destinations) && destinations.length > 0) 
    ? destinations 
    : DEFAULT_DESTINATIONS;

  const [localDestinations, setLocalDestinations] = useState(activeDestinations);

  useEffect(() => {
    if (Array.isArray(destinations) && destinations.length > 0) {
      setLocalDestinations(destinations);
    }
  }, [destinations]);

  // Top KPIs
  const totalCount = localDestinations.length;
  const publishedCount = localDestinations.filter(d => 
    (d?.content_status || 'Published').toLowerCase() === 'published'
  ).length;
  const pendingCount = totalCount - publishedCount;
  const activePoisCount = localDestinations.reduce((sum, d) => {
    const pois = (d?.places_pois || '').split(',').filter(Boolean);
    return sum + (pois.length > 0 ? pois.length : 4);
  }, 0);

  // Filtered destinations
  const filtered = useMemo(() => {
    return localDestinations.filter(d => {
      if (!d) return false;
      const q = searchTerm.toLowerCase();
      const name = (d.destination_name || '').toLowerCase();
      const pois = (d.places_pois || '').toLowerCase();
      const hotels = (d.hotels || '').toLowerCase();
      const acts = (d.activities || '').toLowerCase();
      const matchesSearch = name.includes(q) || pois.includes(q) || hotels.includes(q) || acts.includes(q);

      const status = (d.content_status || 'Published').toLowerCase();
      const matchesStatus = 
        statusFilter === 'all' ? true :
        statusFilter === 'published' ? status === 'published' :
        statusFilter === 'pending' ? (status.includes('pending') || status.includes('draft') || status.includes('review')) :
        true;

      return matchesSearch && matchesStatus;
    });
  }, [localDestinations, searchTerm, statusFilter]);

  // Form for New Destination
  const [newDest, setNewDest] = useState({
    destination_name: '',
    places_pois: '',
    hotels: '',
    activities: '',
    restaurants: '',
    emergency_help_info: '',
    content_status: 'Published'
  });

  const handleCreateDestination = (e) => {
    e.preventDefault();
    if (!newDest.destination_name) return;
    const item = {
      destination_id: `DST-${Date.now().toString().slice(-4)}`,
      created_at: new Date().toISOString(),
      ...newDest
    };
    setLocalDestinations([item, ...localDestinations]);
    setShowAddModal(false);
    setNewDest({
      destination_name: '',
      places_pois: '',
      hotels: '',
      activities: '',
      restaurants: '',
      emergency_help_info: '',
      content_status: 'Published'
    });
  };

  const getDestinationPhoto = (name) => {
    const n = (name || '').toLowerCase();
    if (n.includes('goa') || n.includes('beach')) return 'https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?w=600&auto=format&fit=crop&q=80';
    if (n.includes('ladakh') || n.includes('mountain') || n.includes('himalaya')) return 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600&auto=format&fit=crop&q=80';
    if (n.includes('manali') || n.includes('shimla') || n.includes('snow')) return 'https://images.unsplash.com/photo-1548013146-72479768bada?w=600&auto=format&fit=crop&q=80';
    if (n.includes('kerala') || n.includes('backwaters')) return 'https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?w=600&auto=format&fit=crop&q=80';
    if (n.includes('jaipur') || n.includes('rajasthan') || n.includes('palace')) return 'https://images.unsplash.com/photo-1599661046289-e31897846e41?w=600&auto=format&fit=crop&q=80';
    return 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=600&auto=format&fit=crop&q=80';
  };

  const columns = [
    { 
      key: 'destination_name', 
      label: 'Destination Circuit',
      render: (val, row) => {
        const name = val || row?.destination_name || 'Unnamed Circuit';
        const destId = row?.destination_id || (row?.id ? `DST-${row.id}` : 'DST-LIVE');
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div 
              style={{
                width: '38px',
                height: '38px',
                borderRadius: '10px',
                background: 'linear-gradient(135deg, #0ea5e9, #2563eb)',
                color: '#ffffff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0
              }}
            >
              <MapPin size={18} />
            </div>
            <div>
              <div style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{name}</div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{destId}</div>
            </div>
          </div>
        );
      }
    },
    { 
      key: 'places_pois', 
      label: 'Points of Interest (POIs)',
      render: (val) => (
        <div style={{ maxWidth: '240px', fontSize: '0.78rem', color: 'var(--text-primary)', lineHeight: 1.35 }}>
          {val || '—'}
        </div>
      )
    },
    { 
      key: 'hotels', 
      label: 'Hotels & Stays',
      render: (val) => (
        <div style={{ maxWidth: '200px', fontSize: '0.78rem', color: 'var(--text-secondary)', lineHeight: 1.35 }}>
          {val || '—'}
        </div>
      )
    },
    { 
      key: 'activities', 
      label: 'Activities & Tours',
      render: (val) => (
        <div style={{ maxWidth: '200px', fontSize: '0.78rem', color: 'var(--primary)', fontWeight: 500, lineHeight: 1.35 }}>
          {val || '—'}
        </div>
      )
    },
    { 
      key: 'restaurants', 
      label: 'Dining & Food',
      render: (val) => (
        <div style={{ maxWidth: '180px', fontSize: '0.78rem', color: 'var(--text-secondary)', lineHeight: 1.35 }}>
          {val || '—'}
        </div>
      )
    },
    { 
      key: 'emergency_help_info', 
      label: 'Emergency / SOS',
      render: (val) => (
        <div style={{ 
          fontSize: '0.75rem', 
          color: 'var(--danger)', 
          fontWeight: 600, 
          display: 'flex', 
          alignItems: 'center', 
          gap: '4px',
          maxWidth: '220px',
          lineHeight: 1.3
        }}>
          <Phone size={12} style={{ flexShrink: 0 }} />
          <span>{val || '112 / 100'}</span>
        </div>
      )
    },
    { 
      key: 'content_status', 
      label: 'Status', 
      isBadge: true,
      render: (val) => <StatusBadge status={val || 'Published'} />
    }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Top Page Header */}
      <PageHeader
        title="Content & Destination Repository"
        description="Curate tourism destination circuits, manage points of interest (POIs), hotels, activities, and emergency hotlines."
        breadcrumbs={['TravelHub', 'Content Data']}
        secondaryActions={
          <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
            <button
              type="button"
              className={`btn btn-sm ${viewMode === 'table' ? 'btn-primary' : 'btn-outline'}`}
              onClick={() => setViewMode('table')}
              style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.78rem' }}
            >
              <List size={14} />
              <span>Table</span>
            </button>
            <button
              type="button"
              className={`btn btn-sm ${viewMode === 'grid' ? 'btn-primary' : 'btn-outline'}`}
              onClick={() => setViewMode('grid')}
              style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.78rem' }}
            >
              <LayoutGrid size={14} />
              <span>Cards</span>
            </button>
          </div>
        }
        primaryAction={
          <button 
            type="button" 
            className="btn btn-primary"
            onClick={() => setShowAddModal(true)}
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <Plus size={15} />
            <span>Add Destination</span>
          </button>
        }
      />

      {/* 4 KPI Cards */}
      <div className="stats-grid">
        <KPIStatCard
          title="Total Destinations"
          value={totalCount}
          icon={Compass}
          trend="+15% MoM"
          isPositive={true}
          comparisonText="National tourism circuits"
          colorScheme="primary"
          sparklineData={[1, 2, 2, 3, 3, 3, totalCount]}
        />
        <KPIStatCard
          title="Published Content"
          value={publishedCount}
          icon={CheckCircle2}
          trend="100% Live"
          isPositive={true}
          comparisonText="Mobile app synced"
          colorScheme="success"
          sparklineData={[1, 1, 2, 2, 3, 3, publishedCount]}
        />
        <KPIStatCard
          title="Curated POIs"
          value={activePoisCount}
          icon={MapPin}
          trend="Attractions"
          isPositive={true}
          comparisonText="Monitored tourist sites"
          colorScheme="purple"
          sparklineData={[5, 8, 10, 12, 14, 16, activePoisCount]}
        />
        <KPIStatCard
          title="Emergency Help Info"
          value={`${totalCount} Verified`}
          icon={Phone}
          trend="24/7 Hotlines"
          isPositive={true}
          comparisonText="Tourist police & hospitals"
          colorScheme="danger"
        />
      </div>

      {/* Main View: Table (Default) or Cards */}
      {viewMode === 'table' ? (
        <DataTable
          title="Central Content & Destination Directory"
          subtitle="Real-time destination inventory and mobile app content distribution"
          columns={columns}
          data={filtered}
          onViewDetail={onViewDetail}
          searchPlaceholder="Search destinations by name, attractions, hotels..."
        />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {/* Card View Search Toolbar */}
          <div className="card" style={{ padding: '14px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
            <div style={{ position: 'relative', width: '100%', maxWidth: '360px' }}>
              <Search size={15} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="text"
                className="form-control"
                placeholder="Search destinations..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                style={{ paddingLeft: '34px', fontSize: '0.85rem', width: '100%' }}
              />
            </div>
            <div style={{ display: 'flex', gap: '6px' }}>
              {['all', 'published', 'pending'].map(st => (
                <button
                  key={st}
                  type="button"
                  className={`btn btn-sm ${statusFilter === st ? 'btn-primary' : 'btn-outline'}`}
                  style={{ textTransform: 'capitalize', fontSize: '0.78rem' }}
                  onClick={() => setStatusFilter(st)}
                >
                  {st === 'all' ? 'All Content' : st}
                </button>
              ))}
            </div>
          </div>

          {/* Cards Grid */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
            gap: '20px'
          }}>
            {filtered.map((dest, idx) => (
              <div 
                key={dest.destination_id || dest.destination_name || idx}
                className="card"
                style={{
                  overflow: 'hidden',
                  display: 'flex',
                  flexDirection: 'column',
                  borderRadius: '12px',
                  border: '1px solid var(--border-color)',
                  transition: 'transform 0.2s, box-shadow 0.2s'
                }}
              >
                {/* Photo Banner */}
                <div style={{ position: 'relative', height: '160px', width: '100%', overflow: 'hidden' }}>
                  <img
                    src={getDestinationPhoto(dest.destination_name)}
                    alt={dest.destination_name}
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                  <div style={{ position: 'absolute', top: '12px', right: '12px' }}>
                    <StatusBadge status={dest.content_status || 'Published'} />
                  </div>
                  <div style={{
                    position: 'absolute',
                    bottom: '0',
                    left: '0',
                    right: '0',
                    padding: '12px 16px',
                    background: 'linear-gradient(to top, rgba(0,0,0,0.85) 0%, transparent 100%)',
                    color: '#ffffff'
                  }}>
                    <h3 style={{ fontSize: '1.1rem', fontWeight: 700, margin: 0 }}>
                      {dest.destination_name || 'Circuit'}
                    </h3>
                  </div>
                </div>

                {/* Card Body */}
                <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px', flex: 1 }}>
                  <div>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700, letterSpacing: '0.5px' }}>
                      Points of Interest
                    </div>
                    <div style={{ fontSize: '0.85rem', color: 'var(--text-primary)', marginTop: '2px', fontWeight: 500 }}>
                      {dest.places_pois || 'Attractions & viewpoint trails'}
                    </div>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', paddingTop: '8px', borderTop: '1px solid var(--border-color)' }}>
                    <div>
                      <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Hotel size={12} /> Hotels
                      </div>
                      <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                        {dest.hotels || 'Partner Stays'}
                      </div>
                    </div>
                    <div>
                      <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Utensils size={12} /> Dining
                      </div>
                      <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                        {dest.restaurants || 'Local Cuisine'}
                      </div>
                    </div>
                  </div>

                  <div style={{ 
                    backgroundColor: 'rgba(239, 68, 68, 0.08)', 
                    padding: '8px 12px', 
                    borderRadius: '6px', 
                    display: 'flex', 
                    alignItems: 'center', 
                    gap: '8px',
                    fontSize: '0.75rem',
                    color: 'var(--danger)',
                    marginTop: 'auto'
                  }}>
                    <Phone size={13} style={{ flexShrink: 0 }} />
                    <span><strong>SOS:</strong> {dest.emergency_help_info || '112 / 100'}</span>
                  </div>
                </div>

                {/* Card Footer Actions */}
                <div style={{ 
                  padding: '10px 16px', 
                  backgroundColor: 'var(--bg-surface-secondary)', 
                  borderTop: '1px solid var(--border-color)',
                  display: 'flex',
                  justifyContent: 'flex-end',
                  alignItems: 'center'
                }}>
                  <button
                    type="button"
                    className="btn btn-outline btn-sm"
                    onClick={() => onViewDetail && onViewDetail(dest)}
                    style={{ fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                  >
                    <Eye size={13} /> Inspect
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Add Destination Modal */}
      {showAddModal && (
        <div className="modal-backdrop" onClick={() => setShowAddModal(false)}>
          <div className="modal-card" style={{ maxWidth: '560px' }} onClick={e => e.stopPropagation()}>
            <div className="modal-header" style={{ padding: '18px 24px', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between' }}>
              <h3 className="modal-title">Add Tourism Destination / Circuit</h3>
              <button type="button" className="btn btn-outline btn-icon" onClick={() => setShowAddModal(false)}>✕</button>
            </div>
            <form onSubmit={handleCreateDestination} style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label className="field-label">Destination Name *</label>
                <input
                  type="text"
                  className="form-control"
                  style={{ width: '100%' }}
                  placeholder="e.g. Munnar & Alleppey Backwaters"
                  value={newDest.destination_name}
                  onChange={e => setNewDest({ ...newDest, destination_name: e.target.value })}
                  required
                />
              </div>
              <div>
                <label className="field-label">Key Attractions / POIs</label>
                <input
                  type="text"
                  className="form-control"
                  style={{ width: '100%' }}
                  placeholder="e.g. Tea Gardens, Anamudi Peak, Mattupetty Dam"
                  value={newDest.places_pois}
                  onChange={e => setNewDest({ ...newDest, places_pois: e.target.value })}
                />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label className="field-label">Hotels & Stays</label>
                  <input
                    type="text"
                    className="form-control"
                    style={{ width: '100%' }}
                    placeholder="e.g. Tea Valley Resort, Misty Hills"
                    value={newDest.hotels}
                    onChange={e => setNewDest({ ...newDest, hotels: e.target.value })}
                  />
                </div>
                <div>
                  <label className="field-label">Dining & Restaurants</label>
                  <input
                    type="text"
                    className="form-control"
                    style={{ width: '100%' }}
                    placeholder="e.g. Rapsy Restaurant, Saravana"
                    value={newDest.restaurants}
                    onChange={e => setNewDest({ ...newDest, restaurants: e.target.value })}
                  />
                </div>
              </div>
              <div>
                <label className="field-label">Key Activities</label>
                <input
                  type="text"
                  className="form-control"
                  style={{ width: '100%' }}
                  placeholder="e.g. Trekking, Boating, Wildlife Safari"
                  value={newDest.activities}
                  onChange={e => setNewDest({ ...newDest, activities: e.target.value })}
                />
              </div>
              <div>
                <label className="field-label">Emergency Helpline Contacts</label>
                <input
                  type="text"
                  className="form-control"
                  style={{ width: '100%' }}
                  placeholder="e.g. 112 / Tourist Police (04865-230300)"
                  value={newDest.emergency_help_info}
                  onChange={e => setNewDest({ ...newDest, emergency_help_info: e.target.value })}
                />
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button type="button" className="btn btn-outline" onClick={() => setShowAddModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save & Publish Destination</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
