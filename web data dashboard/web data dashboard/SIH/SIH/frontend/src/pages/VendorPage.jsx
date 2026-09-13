import React, { useState } from 'react';
import PageHeader from '../components/PageHeader';
import KPIStatCard from '../components/KPIStatCard';
import DataTable from '../components/DataTable';
import StatusBadge from '../components/StatusBadge';
import { Store, CheckCircle, Clock, AlertTriangle, Plus, Star, ShieldCheck, MapPin, Building } from 'lucide-react';

export default function VendorPage({ vendors = [], onViewDetail, onVerifyVendor }) {
  const [showAddModal, setShowAddModal] = useState(false);
  const [categoryFilter, setCategoryFilter] = useState('all');

  // Top KPIs
  const totalCount = vendors.length;
  const verifiedCount = vendors.filter(v => (v.verification_status || '').toLowerCase() === 'verified').length;
  const pendingCount = vendors.filter(v => (v.verification_status || '').toLowerCase() === 'pending').length;
  const otherCount = totalCount - verifiedCount - pendingCount;

  // Filter by category
  const filteredVendors = vendors.filter(v => {
    if (categoryFilter === 'all') return true;
    const listings = (v.listings || '').toLowerCase();
    const name = (v.business_name || '').toLowerCase();
    if (categoryFilter === 'stay') return listings.includes('tent') || listings.includes('villa') || listings.includes('stay') || listings.includes('resort');
    if (categoryFilter === 'water') return listings.includes('scuba') || listings.includes('boat') || listings.includes('kayak') || listings.includes('water');
    if (categoryFilter === 'adventure') return listings.includes('paragliding') || listings.includes('trek') || listings.includes('safari') || listings.includes('gear');
    return true;
  });

  const columns = [
    {
      key: 'business_name',
      label: 'Vendor Business',
      render: (val, row) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div 
            style={{
              width: '38px',
              height: '38px',
              borderRadius: '10px',
              backgroundColor: 'var(--primary-light)',
              color: 'var(--primary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0
            }}
          >
            <Store size={18} />
          </div>
          <div>
            <div style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{val}</div>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{row.vendor_id}</div>
          </div>
        </div>
      )
    },
    {
      key: 'owner_contact',
      label: 'Owner / Contact',
      render: (val) => (
        <span style={{ fontSize: '0.825rem', color: 'var(--text-primary)', fontWeight: 500 }}>
          {val || '—'}
        </span>
      )
    },
    {
      key: 'kyc_info',
      label: 'KYC & Compliance',
      render: (val) => (
        <div style={{ maxWidth: '240px', fontSize: '0.75rem', color: 'var(--text-secondary)', lineHeight: 1.3 }}>
          {val || 'Documents Submitted'}
        </div>
      )
    },
    {
      key: 'listings',
      label: 'Inventory / Services',
      render: (val) => (
        <span style={{ fontSize: '0.8rem', color: 'var(--primary)', fontWeight: 500 }}>
          {val || 'General Services'}
        </span>
      )
    },
    {
      key: 'booking_requests_count',
      label: 'Fulfillment',
      render: (val, row) => (
        <div style={{ fontSize: '0.78rem' }}>
          <span style={{ fontWeight: 700, color: '#10b981' }}>{row.accepted_bookings_count || 0}</span>
          <span style={{ color: 'var(--text-muted)' }}> / {val || 0} Req</span>
        </div>
      )
    },
    {
      key: 'verification_status',
      label: 'KYC Status',
      isBadge: true
    },
    {
      key: 'actions',
      label: 'Compliance Action',
      render: (_, row) => {
        const isPending = (row.verification_status || '').toLowerCase() === 'pending';
        return (
          <div style={{ display: 'flex', gap: '6px' }}>
            {isPending ? (
              <button
                type="button"
                className="btn btn-sm btn-success"
                style={{ padding: '3px 8px', fontSize: '0.72rem' }}
                onClick={() => alert(`Verified vendor: ${row.business_name}`)}
              >
                Approve KYC
              </button>
            ) : (
              <span style={{ fontSize: '0.75rem', color: '#10b981', fontWeight: 600 }}>
                Verified ✓
              </span>
            )}
          </div>
        );
      }
    }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <PageHeader
        title="Vendor Partner Network"
        description="Audit tourism service providers, review regulatory KYC filings, and monitor booking fulfillment."
        breadcrumbs={['TravelHub', 'Vendors']}
        secondaryActions={
          <div style={{ display: 'flex', gap: '6px' }}>
            <button
              type="button"
              className={`btn btn-sm ${categoryFilter === 'all' ? 'btn-primary' : 'btn-outline'}`}
              onClick={() => setCategoryFilter('all')}
            >
              All Categories
            </button>
            <button
              type="button"
              className={`btn btn-sm ${categoryFilter === 'stay' ? 'btn-primary' : 'btn-outline'}`}
              onClick={() => setCategoryFilter('stay')}
            >
              Stays & Resorts
            </button>
            <button
              type="button"
              className={`btn btn-sm ${categoryFilter === 'water' ? 'btn-primary' : 'btn-outline'}`}
              onClick={() => setCategoryFilter('water')}
            >
              Watersports
            </button>
            <button
              type="button"
              className={`btn btn-sm ${categoryFilter === 'adventure' ? 'btn-primary' : 'btn-outline'}`}
              onClick={() => setCategoryFilter('adventure')}
            >
              Adventures
            </button>
          </div>
        }
      />

      {/* 4 KPI Cards */}
      <div className="stats-grid">
        <KPIStatCard
          title="Total Registered Vendors"
          value={totalCount}
          icon={Store}
          trend="+12% MoM"
          isPositive={true}
          comparisonText="National partner network"
          colorScheme="primary"
          sparklineData={[2, 3, 3, 4, 4, 4, 4]}
        />
        <KPIStatCard
          title="Verified Partners"
          value={verifiedCount}
          icon={CheckCircle}
          trend="100% compliant"
          isPositive={true}
          comparisonText="GSTIN & Police cleared"
          colorScheme="success"
          sparklineData={[1, 2, 2, 3, 3, 3, 3]}
        />
        <KPIStatCard
          title="Pending KYC Audit"
          value={pendingCount}
          icon={Clock}
          trend={pendingCount > 0 ? "Action Req" : "Zero"}
          isPositive={pendingCount === 0}
          comparisonText="Awaiting admin verification"
          colorScheme="warning"
        />
        <KPIStatCard
          title="Avg Partner Rating"
          value="4.8 ★"
          icon={Star}
          trend="Top Tier"
          isPositive={true}
          comparisonText="Based on tourist reviews"
          colorScheme="purple"
        />
      </div>

      {/* Vendor Table */}
      <DataTable
        title="Central Vendor Directory"
        subtitle="Audited tourism listings & real-time slot confirmations"
        columns={columns}
        data={filteredVendors}
        onViewDetail={onViewDetail}
        searchPlaceholder="Search by business name, owner, license..."
      />
    </div>
  );
}
