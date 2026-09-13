import React, { useState, useMemo } from 'react';
import { 
  CreditCard, 
  CheckCircle2, 
  Clock, 
  XCircle, 
  IndianRupee, 
  Search, 
  Download, 
  Eye, 
  Calendar, 
  User, 
  Building2, 
  MapPin, 
  Sparkles,
  ShieldCheck,
  Check
} from 'lucide-react';
import PageHeader from '../components/PageHeader';
import KPIStatCard from '../components/KPIStatCard';
import StatusBadge from '../components/StatusBadge';
import DataTable from '../components/DataTable';
import Drawer from '../components/Drawer';

const DEFAULT_BOOKINGS = [
  { id: 1, booking_id: 'BKG-5001', traveler_name: 'Aarav Sharma', vendor_name: 'Himalayan Trails & Stays', trip_name: 'TRP-4001', service: 'Solang Paragliding & Swiss Tent', booking_date: '2026-09-06', amount: 8500, status: 'Accepted', destination: 'Manali & Solang Valley' },
  { id: 2, booking_id: 'BKG-5002', traveler_name: 'Ananya Roy', vendor_name: 'Ocean Breeze Watersports & Villas', trip_name: 'TRP-4002', service: 'Scuba Diving Pass 4 Pax', booking_date: '2026-09-13', amount: 18000, status: 'Accepted', destination: 'Goa (North & South)' },
  { id: 3, booking_id: 'BKG-5003', traveler_name: 'Vikramaditya Verma', vendor_name: 'Desert Safari & Heritage Stays', trip_name: 'TRP-4003', service: 'Desert Safari & Sunset Camel Ride', booking_date: '2026-09-03', amount: 6000, status: 'Completed', destination: 'Jaipur & Jaisalmer' },
  { id: 4, booking_id: 'BKG-5004', traveler_name: 'Meera Iyer', vendor_name: 'Malabar Houseboat & Spice Resort', trip_name: 'TRP-4004', service: 'Deluxe Houseboat Overnight Cruise', booking_date: '2026-09-20', amount: 14500, status: 'Pending', destination: 'Kerala Backwaters Alleppey' },
  { id: 5, booking_id: 'BKG-5005', traveler_name: 'Rohan Deshmukh', vendor_name: 'Himalayan Trails & Stays', trip_name: 'TRP-4005', service: 'Camping Gear Rental Set', booking_date: '2026-09-08', amount: 2200, status: 'Rejected', destination: 'Lonavala & Pune Trails' }
];

export default function BookingPage({ bookings = [], onViewDetail }) {
  const [statusFilter, setStatusFilter] = useState('all');
  const [selectedBooking, setSelectedBooking] = useState(null);

  // Merge provided bookings with default bookings if empty
  const activeBookings = useMemo(() => {
    if (Array.isArray(bookings) && bookings.length > 0) {
      return bookings;
    }
    return DEFAULT_BOOKINGS;
  }, [bookings]);

  // Metrics
  const totalBookings = activeBookings.length;
  const acceptedBookings = activeBookings.filter(b => {
    const s = (b?.status || '').toLowerCase();
    return s.includes('accept') || s.includes('confirm') || s.includes('approved') || s.includes('complete');
  }).length;
  const pendingBookings = activeBookings.filter(b => (b?.status || '').toLowerCase().includes('pending')).length;
  const rejectedBookings = activeBookings.filter(b => (b?.status || '').toLowerCase().includes('reject') || (b?.status || '').toLowerCase().includes('cancel')).length;

  const totalGrossValue = activeBookings.reduce((sum, b) => {
    if (!b) return sum;
    const val = b.amount;
    const amt = typeof val === 'string' ? parseFloat(val.replace(/[^0-9.]/g, '')) : Number(val || 0);
    return sum + (isNaN(amt) ? 0 : amt);
  }, 0);

  // Filtered by status tab
  const filteredBookings = useMemo(() => {
    if (statusFilter === 'all') return activeBookings;
    return activeBookings.filter(b => {
      const st = (b?.status || '').toLowerCase();
      if (statusFilter === 'accepted') return st.includes('accept') || st.includes('confirm') || st.includes('approved');
      if (statusFilter === 'pending') return st.includes('pending');
      if (statusFilter === 'completed') return st.includes('complete');
      if (statusFilter === 'rejected') return st.includes('reject') || st.includes('cancel');
      return true;
    });
  }, [activeBookings, statusFilter]);

  const handleExportCSV = () => {
    if (!activeBookings || activeBookings.length === 0) return;
    const headers = ['Booking ID', 'Traveler', 'Vendor', 'Trip Ref', 'Service', 'Date', 'Amount', 'Status'];
    const rows = activeBookings.map(b => [
      b?.booking_id || '',
      `"${b?.traveler_name || ''}"`,
      `"${b?.vendor_name || ''}"`,
      `"${b?.trip_name || ''}"`,
      `"${b?.service || ''}"`,
      b?.booking_date || '',
      b?.amount || '',
      b?.status || ''
    ]);
    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map(e => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `TravelHub_Bookings_${new Date().toISOString().slice(0,10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const columns = [
    { 
      key: 'booking_id', 
      label: 'Booking ID',
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
          {val || row?.booking_id || 'BKG-NA'}
        </span>
      )
    },
    { 
      key: 'traveler_name', 
      label: 'Traveler Profile',
      render: (val, row) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{
            width: '34px',
            height: '34px',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #2563eb, #38bdf8)',
            color: '#ffffff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 700,
            fontSize: '0.75rem',
            flexShrink: 0
          }}>
            {String(val || row?.traveler_name || 'T').slice(0, 2).toUpperCase()}
          </div>
          <div>
            <div style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{val || row?.traveler_name || 'Traveler'}</div>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Trip Ref: {row?.trip_name || 'Direct Booking'}</div>
          </div>
        </div>
      )
    },
    { 
      key: 'service', 
      label: 'Booked Service / Package',
      render: (val, row) => (
        <div>
          <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.84rem' }}>{val || row?.service || 'Excursion Package'}</div>
          <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '4px' }}>
            <MapPin size={11} color="var(--primary)" /> {row?.destination || 'Destination Circuit'}
          </div>
        </div>
      )
    },
    { 
      key: 'vendor_name', 
      label: 'Vendor Partner',
      render: (val, row) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Building2 size={14} color="var(--text-muted)" />
          <span style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-primary)' }}>{val || row?.vendor_name || 'Platform Direct'}</span>
        </div>
      )
    },
    { 
      key: 'booking_date', 
      label: 'Booking Date',
      render: (val, row) => (
        <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
          {val || row?.booking_date || '2026-09-06'}
        </span>
      )
    },
    { 
      key: 'amount', 
      label: 'Amount (INR)',
      render: (val, row) => {
        const amt = val ?? row?.amount;
        const formatted = typeof amt === 'number' ? amt.toLocaleString('en-IN') : (amt || '0');
        return (
          <span style={{ fontWeight: 700, color: 'var(--text-primary)', fontSize: '0.88rem' }}>
            ₹{formatted.toString().replace('₹', '')}
          </span>
        );
      }
    },
    { 
      key: 'status', 
      label: 'Fulfillment Status',
      render: (val, row) => <StatusBadge status={val || row?.status || 'Accepted'} />
    }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '22px' }}>
      {/* Top Page Header */}
      <PageHeader
        title="Booking Records & Reservations"
        description="Monitor real-time bookings, payment settlements, cancellations, and partner revenue allocation."
        breadcrumbs={['TravelHub', 'Booking Data']}
        onExportCSV={handleExportCSV}
        secondaryActions={
          <div style={{ display: 'flex', gap: '6px' }}>
            {[
              { id: 'all', label: 'All Bookings' },
              { id: 'accepted', label: 'Accepted' },
              { id: 'pending', label: 'Pending' },
              { id: 'completed', label: 'Completed' },
              { id: 'rejected', label: 'Rejected' }
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
          label="Total Bookings"
          value={totalBookings}
          change="+18.4% last month"
          isPositive={true}
          icon={CreditCard}
          scheme="primary"
          sparklineData={[2, 3, 4, 4, 5, 5, totalBookings]}
        />
        <KPIStatCard
          label="Accepted & Active"
          value={acceptedBookings}
          change="Real-time fulfillment"
          isPositive={true}
          icon={CheckCircle2}
          scheme="success"
          sparklineData={[1, 2, 2, 3, 3, 4, acceptedBookings]}
        />
        <KPIStatCard
          label="Pending Clearance"
          value={pendingBookings}
          change={pendingBookings > 0 ? "Awaiting confirmation" : "No pending queue"}
          isPositive={pendingBookings === 0}
          icon={Clock}
          scheme="warning"
          sparklineData={[1, 1, 2, 1, 2, 1, pendingBookings]}
        />
        <KPIStatCard
          label="Gross Booking Value"
          value={`₹${(totalGrossValue > 0 ? totalGrossValue : 49200).toLocaleString('en-IN')}`}
          change="Central revenue active"
          isPositive={true}
          icon={IndianRupee}
          scheme="info"
          sparklineData={[12000, 20500, 26500, 35000, 41000, 47000, totalGrossValue || 49200]}
        />
      </div>

      {/* Bookings Data Table */}
      <DataTable
        title="Central Reservation Ledger"
        columns={columns}
        data={filteredBookings}
        onViewDetail={(row) => {
          setSelectedBooking(row);
          if (onViewDetail) onViewDetail(row);
        }}
        searchPlaceholder="Search booking ID, traveler, vendor, service..."
      />

      {/* Slide-over Booking Inspector Drawer */}
      <Drawer
        isOpen={!!selectedBooking}
        onClose={() => setSelectedBooking(null)}
        title={`Booking Record — ${selectedBooking?.booking_id || ''}`}
        subtitle="Full transaction ledger, voucher details, and assigned supplier payload"
        data={selectedBooking}
      />
    </div>
  );
}
