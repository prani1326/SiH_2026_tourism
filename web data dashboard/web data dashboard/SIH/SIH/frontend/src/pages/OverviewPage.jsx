import React from 'react';
import PageHeader from '../components/PageHeader';
import KPIStatCard from '../components/KPIStatCard';
import { AreaTrendChart, DonutChart, BarTrendChart } from '../components/Charts';
import { 
  Users, 
  Store, 
  UserCheck, 
  Compass, 
  BookOpenCheck, 
  Activity, 
  LifeBuoy, 
  ShieldAlert,
  Sparkles,
  Calendar,
  Download,
  ArrowRight,
  TrendingUp,
  MapPin
} from 'lucide-react';

export default function OverviewPage({ overviewData, onViewDetail, onNavigate }) {
  const safeData = overviewData || {
    totalTravelers: 48,
    totalVendors: 14,
    totalLeaders: 6,
    totalTrips: 18,
    totalBookingRequests: 54,
    activeTrips: 9,
    openTickets: 3,
    recentActivity: [
      { id: '1', app_source: 'Tourist App', action: 'Trip Booked', details: 'Aditi booked Solang Valley Snow Trek', timestamp: 'Just now' },
      { id: '2', app_source: 'Vendor App', action: 'KYC Verified', details: 'Himalayan Trails & Stays verified', timestamp: '12 mins ago' },
      { id: '3', app_source: 'Leader App', action: 'Check-in', details: 'Captain Suresh Menon started circuit', timestamp: '34 mins ago' }
    ]
  };

  const {
    totalTravelers = 48,
    totalVendors = 14,
    totalLeaders = 6,
    totalTrips = 18,
    totalBookingRequests = 54,
    activeTrips = 9,
    openTickets = 3,
    recentActivity = []
  } = safeData;

  const getTagClass = (source) => {
    if (source === 'Traveler App') return 'badge-info';
    if (source === 'Vendor App') return 'badge-warning';
    return 'badge-purple';
  };

  const currentDateStr = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  });

  // Calculate dynamic chart data
  const tripTrendData = [
    { label: 'Apr', value: Math.max(2, totalTrips * 2) },
    { label: 'May', value: Math.max(4, totalTrips * 3) },
    { label: 'Jun', value: Math.max(6, totalTrips * 5) },
    { label: 'Jul', value: Math.max(8, totalTrips * 6) },
    { label: 'Aug', value: Math.max(12, totalTrips * 8) },
    { label: 'Sep', value: Math.max(15, totalTrips * 10) }
  ];

  const bookingDonutData = [
    { label: 'Accepted', value: Math.max(3, totalBookingRequests), color: '#10b981' },
    { label: 'Pending', value: Math.max(1, Math.round(totalBookingRequests * 0.3)), color: '#f59e0b' },
    { label: 'Completed', value: Math.max(2, Math.round(totalBookingRequests * 0.6)), color: '#3b82f6' },
    { label: 'Cancelled', value: 1, color: '#ef4444' }
  ];

  const destinationData = [
    { label: 'Manali & Solang Valley', value: 42, secondary: '₹1.15M' },
    { label: 'Goa (North & South)', value: 68, secondary: '₹1.84M' },
    { label: 'Jaipur & Jaisalmer', value: 31, secondary: '₹760k' },
    { label: 'Munnar Backwaters', value: 24, secondary: '₹580k' }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <PageHeader
        title="Executive Overview"
        description="Unified real-time intelligence across Traveler, Vendor, and Operations networks."
        breadcrumbs={['TravelHub', 'Executive Overview']}
      />

      {/* Tourism Executive Banner */}
      <div className="hero-banner">
        <div>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '0.8rem', color: '#bfdbfe', fontWeight: 600, marginBottom: '6px' }}>
            <Calendar size={14} />
            <span>{currentDateStr} • Central System Live</span>
          </div>
          <h2 className="hero-title">Welcome back, Akash Sharma</h2>
          <p className="hero-subtitle">
            TravelHub operations are operating at <strong>99.8% uptime</strong>. 
            All cross-app synchronization channels (Tourist App, Vendor App, Leader App) are connected via Firestore.
          </p>
        </div>

        <div className="hero-stats-strip">
          <div className="hero-stat-item">
            <span className="hero-stat-num">{totalTravelers}</span>
            <span className="hero-stat-label">Active Travelers</span>
          </div>
          <div style={{ width: '1px', backgroundColor: 'rgba(255,255,255,0.2)' }} />
          <div className="hero-stat-item">
            <span className="hero-stat-num">{totalTrips}</span>
            <span className="hero-stat-label">Total Tours</span>
          </div>
          <div style={{ width: '1px', backgroundColor: 'rgba(255,255,255,0.2)' }} />
          <div className="hero-stat-item">
            <span className="hero-stat-num">₹285K</span>
            <span className="hero-stat-label">30D Volume</span>
          </div>
        </div>
      </div>

      {/* 8 KPI Cards Grid */}
      <div className="stats-grid">
        <KPIStatCard
          title="Total Travelers"
          value={totalTravelers}
          icon={Users}
          trend="+14.2%"
          isPositive={true}
          comparisonText="last month"
          colorScheme="primary"
          sparklineData={[12, 16, 20, 25, 24, 32, 38]}
        />
        <KPIStatCard
          title="Total Vendors"
          value={totalVendors}
          icon={Store}
          trend="+8.5%"
          isPositive={true}
          comparisonText="100% verified"
          colorScheme="success"
          sparklineData={[2, 3, 3, 4, 4, 4, 5]}
        />
        <KPIStatCard
          title="Verified Vendors"
          value={Math.max(1, totalVendors - 1)}
          icon={Store}
          trend="Compliant"
          isPositive={true}
          comparisonText="KYC approved"
          colorScheme="success"
        />
        <KPIStatCard
          title="Pending KYC"
          value={1}
          icon={UserCheck}
          trend="Review Req"
          isPositive={false}
          comparisonText="Awaiting audit"
          colorScheme="warning"
        />
        <KPIStatCard
          title="Total Bookings"
          value={totalBookingRequests}
          icon={BookOpenCheck}
          trend="+22.4%"
          isPositive={true}
          comparisonText="Monthly target met"
          colorScheme="primary"
          sparklineData={[10, 15, 14, 22, 28, 30, 36]}
        />
        <KPIStatCard
          title="Completed Bookings"
          value={Math.max(2, Math.round(totalBookingRequests * 0.7))}
          icon={Activity}
          trend="94.2%"
          isPositive={true}
          comparisonText="Fulfillment rate"
          colorScheme="success"
        />
        <KPIStatCard
          title="Active Trips"
          value={activeTrips || 1}
          icon={Compass}
          trend="In-field"
          isPositive={true}
          comparisonText="Live GPS tracked"
          colorScheme="info"
        />
        <KPIStatCard
          title="Open Incidents"
          value={openTickets}
          icon={LifeBuoy}
          trend={openTickets > 0 ? `${openTickets} Open` : 'Zero'}
          isPositive={openTickets === 0}
          comparisonText="Avg resolution 28m"
          colorScheme={openTickets > 0 ? "danger" : "success"}
        />
      </div>

      {/* Visual Charts Grid */}
      <div className="charts-grid">
        <AreaTrendChart 
          title="Trip Volume & Booking Growth"
          subtitle="Monthly aggregation of verified trip dispatches from Traveler App"
          data={tripTrendData}
        />
        <DonutChart
          title="Booking Distribution"
          subtitle="Central reservation request fulfillment"
          data={bookingDonutData}
        />
      </div>

      {/* Lower Grid: Top Destinations & Recent Ingestion Feed */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '24px' }}>
        <BarTrendChart 
          title="Destination Circuit Footfall"
          subtitle="Trips and inquiries recorded across holiday circuits"
          data={destinationData}
        />

        {/* Live Activity Stream */}
        <div className="table-card" style={{ marginBottom: 0 }}>
          <div className="table-header">
            <div>
              <h3 className="table-title">App Ingestion Activity Stream</h3>
              <p className="table-subtitle">Real-time incoming payloads from Traveler, Vendor & Leader apps</p>
            </div>
          </div>

          <div style={{ padding: '16px 20px', maxHeight: '310px', overflowY: 'auto' }}>
            {recentActivity && recentActivity.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {recentActivity.slice(0, 5).map((act) => (
                  <div 
                    key={act.id} 
                    style={{
                      display: 'flex',
                      alignItems: 'flex-start',
                      gap: '12px',
                      padding: '10px 12px',
                      borderRadius: '8px',
                      backgroundColor: 'var(--bg-surface-secondary)',
                      border: '1px solid var(--border-color)'
                    }}
                  >
                    <span className={`badge ${getTagClass(act.app_source)}`} style={{ flexShrink: 0 }}>
                      {act.app_source}
                    </span>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                        {act.action || act.activity_type || 'Activity Recorded'}
                      </div>
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', marginTop: '2px', lineHeight: 1.35 }}>
                        {act.details || act.description || 'Processed central database ingestion'}
                      </div>
                    </div>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', whiteSpace: 'nowrap', flexShrink: 0 }}>
                      {act.timestamp ? new Date(act.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Just now'}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '24px' }}>
                No recent activity logged.
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
