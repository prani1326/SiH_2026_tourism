import React, { useState, useMemo } from 'react';
import { 
  FileText, 
  Download, 
  Plus, 
  Search, 
  Calendar, 
  Filter, 
  CheckCircle2, 
  Clock, 
  FileSpreadsheet, 
  HardDrive, 
  Sparkles,
  Eye,
  Trash2
} from 'lucide-react';
import PageHeader from '../components/PageHeader';
import KPIStatCard from '../components/KPIStatCard';
import StatusBadge from '../components/StatusBadge';
import DataTable from '../components/DataTable';

export default function ReportsPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('all');
  const [showGenerateModal, setShowGenerateModal] = useState(false);
  const [reports, setReports] = useState([
    {
      id: 'REP-2026-001',
      name: 'Traveler Demographic & Registration Audit Q3',
      category: 'Traveler Report',
      format: 'PDF',
      size: '2.4 MB',
      generated_date: '2026-09-05',
      status: 'Ready'
    },
    {
      id: 'REP-2026-002',
      name: 'Vendor KYC Compliance & Commission Settlement',
      category: 'Vendor Report',
      format: 'CSV',
      size: '840 KB',
      generated_date: '2026-09-04',
      status: 'Ready'
    },
    {
      id: 'REP-2026-003',
      name: 'Monthly Gross Bookings & Revenue Reconciliation',
      category: 'Booking Summary',
      format: 'XLSX',
      size: '1.8 MB',
      generated_date: '2026-09-02',
      status: 'Ready'
    },
    {
      id: 'REP-2026-004',
      name: 'High Altitude Expedition Safety & SOS Incident Log',
      category: 'Incident Report',
      format: 'PDF',
      size: '3.1 MB',
      generated_date: '2026-08-30',
      status: 'Ready'
    },
    {
      id: 'REP-2026-005',
      name: 'Tour Leader Circuit Dispatch & Attendance Roster',
      category: 'Trip Analytics',
      format: 'PDF',
      size: '1.2 MB',
      generated_date: '2026-08-28',
      status: 'Ready'
    },
    {
      id: 'REP-2026-006',
      name: 'Central Database Integrity & Realtime Sync Audit',
      category: 'System Audit',
      format: 'CSV',
      size: '450 KB',
      generated_date: '2026-08-25',
      status: 'Ready'
    }
  ]);

  // Form for Generating Report
  const [newReport, setNewReport] = useState({
    name: '',
    category: 'Booking Summary',
    format: 'PDF',
    date_range: 'Last 30 Days'
  });

  const handleGenerate = (e) => {
    e.preventDefault();
    if (!newReport.name) return;
    const item = {
      id: `REP-${Date.now().toString().slice(-4)}`,
      name: newReport.name,
      category: newReport.category,
      format: newReport.format,
      size: '1.5 MB',
      generated_date: new Date().toISOString().slice(0, 10),
      status: 'Ready'
    };
    setReports([item, ...reports]);
    setShowGenerateModal(false);
    setNewReport({
      name: '',
      category: 'Booking Summary',
      format: 'PDF',
      date_range: 'Last 30 Days'
    });
  };

  const handleDownload = (report) => {
    const dummyContent = `Report: ${report.name}\nCategory: ${report.category}\nDate: ${report.generated_date}\nStatus: ${report.status}\nPlatform: TravelHub Central Data Console`;
    const element = document.createElement("a");
    const file = new Blob([dummyContent], { type: 'text/plain' });
    element.href = URL.createObjectURL(file);
    element.download = `${report.name.replace(/\s+/g, '_')}.${report.format.toLowerCase()}`;
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  };

  const filteredReports = useMemo(() => {
    return reports.filter(r => {
      const q = searchTerm.toLowerCase();
      const matchSearch = r.name.toLowerCase().includes(q) || r.category.toLowerCase().includes(q) || r.id.toLowerCase().includes(q);
      const matchCategory = categoryFilter === 'all' ? true : r.category.toLowerCase().includes(categoryFilter.toLowerCase());
      return matchSearch && matchCategory;
    });
  }, [reports, searchTerm, categoryFilter]);

  const columns = [
    {
      key: 'id',
      label: 'Report ID',
      render: (val, row) => (
        <span style={{ fontFamily: 'monospace', fontWeight: 700, color: 'var(--primary)', backgroundColor: 'var(--primary-light)', padding: '2px 7px', borderRadius: '4px', fontSize: '0.8rem' }}>
          {val || row?.id}
        </span>
      )
    },
    {
      key: 'name',
      label: 'Report Title',
      render: (val, row) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{
            width: '32px',
            height: '32px',
            borderRadius: '6px',
            backgroundColor: (row?.format || 'PDF') === 'PDF' ? 'rgba(239, 68, 68, 0.1)' : (row?.format || '') === 'CSV' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(59, 130, 246, 0.1)',
            color: (row?.format || 'PDF') === 'PDF' ? '#ef4444' : (row?.format || '') === 'CSV' ? '#10b981' : '#3b82f6',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 800,
            fontSize: '0.7rem'
          }}>
            {row?.format || 'PDF'}
          </div>
          <div>
            <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{val || row?.name}</div>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>File Size: {row?.size || '1.2 MB'}</div>
          </div>
        </div>
      )
    },
    {
      key: 'category',
      label: 'Category',
      render: (val, row) => (
        <span style={{ backgroundColor: 'var(--bg-surface-secondary)', color: 'var(--text-secondary)', padding: '3px 8px', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 600, border: '1px solid var(--border-color)' }}>
          {val || row?.category}
        </span>
      )
    },
    {
      key: 'generated_date',
      label: 'Generated On',
      render: (val, row) => (
        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
          {val || row?.generated_date}
        </span>
      )
    },
    {
      key: 'status',
      label: 'Status',
      render: (val, row) => <StatusBadge status={val || row?.status || 'Ready'} />
    },
    {
      key: 'actions',
      label: 'Actions',
      render: (val, row) => (
        <div style={{ display: 'flex', gap: '6px' }}>
          <button
            type="button"
            className="btn btn-primary btn-sm"
            onClick={() => handleDownload(row)}
            style={{ padding: '3px 8px', fontSize: '0.75rem' }}
          >
            <Download size={12} /> Download
          </button>
        </div>
      )
    }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '22px' }}>
      {/* Top Page Header */}
      <PageHeader
        title="Reports & Compliance Archive"
        description="Generate, schedule, and export business audits, partner settlements, and tourist safety ledgers."
        breadcrumbs={[
          { label: 'TravelHub' },
          { label: 'Reports', active: true }
        ]}
        primaryAction={{
          label: 'Generate New Report',
          icon: Plus,
          onClick: () => setShowGenerateModal(true)
        }}
      />

      {/* KPI Cards */}
      <div className="stats-grid">
        <KPIStatCard
          label="Total Generated Reports"
          value={reports.length}
          change="+6 this month"
          isPositive={true}
          icon={FileText}
          scheme="primary"
          sparklineData={[10, 12, 14, 16, 18, 20, reports.length || 24]}
        />
        <KPIStatCard
          label="Daily Scheduled Runs"
          value="4 Active"
          change="Cron active 00:00 UTC"
          isPositive={true}
          icon={Clock}
          scheme="info"
          sparklineData={[4, 4, 4, 4, 4, 4, 4]}
        />
        <KPIStatCard
          label="Total Downloads"
          value="142 Files"
          change="98% delivery rate"
          isPositive={true}
          icon={Download}
          scheme="success"
          sparklineData={[80, 95, 110, 122, 130, 138, 142]}
        />
        <KPIStatCard
          label="Encrypted Cloud Vault"
          value="14.8 MB"
          change="Firestore & Cloud Storage"
          isPositive={true}
          icon={HardDrive}
          scheme="warning"
          sparklineData={[8, 9, 11, 12, 13, 14, 15]}
        />
      </div>

      {/* Filter Toolbar */}
      <div className="card" style={{ padding: '14px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: '1 1 320px' }}>
          <div style={{ position: 'relative', width: '100%', maxWidth: '360px' }}>
            <Search size={15} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input
              type="text"
              className="form-control"
              placeholder="Search reports by title, ID, category..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{ paddingLeft: '34px', fontSize: '0.85rem' }}
            />
          </div>

          <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
            {['all', 'traveler', 'vendor', 'booking', 'incident'].map(cat => (
              <button
                key={cat}
                type="button"
                className={`btn btn-sm ${categoryFilter === cat ? 'btn-primary' : 'btn-secondary'}`}
                style={{ textTransform: 'capitalize', fontSize: '0.78rem' }}
                onClick={() => setCategoryFilter(cat)}
              >
                {cat === 'all' ? 'All Categories' : `${cat}s`}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Reports Table */}
      <DataTable
        title="Archived Compliance & Financial Reports"
        columns={columns}
        data={filteredReports}
      />

      {/* Generate Report Modal */}
      {showGenerateModal && (
        <div className="modal-overlay" onClick={() => setShowGenerateModal(false)}>
          <div className="modal-card" style={{ maxWidth: '520px' }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <FileText size={18} color="var(--primary)" />
                <span>Generate Business Report</span>
              </h3>
              <button 
                type="button" 
                className="btn btn-secondary btn-sm" 
                onClick={() => setShowGenerateModal(false)}
                style={{ padding: '4px 8px' }}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleGenerate}>
              <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div>
                  <label className="form-label">Report Title *</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. Q3 Vendor Payout & Commission Audit"
                    value={newReport.name}
                    onChange={e => setNewReport({ ...newReport, name: e.target.value })}
                    required
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                  <div>
                    <label className="form-label">Report Category</label>
                    <select
                      className="form-control"
                      value={newReport.category}
                      onChange={e => setNewReport({ ...newReport, category: e.target.value })}
                    >
                      <option value="Traveler Report">Traveler Report</option>
                      <option value="Vendor Report">Vendor Performance</option>
                      <option value="Booking Summary">Booking Summary</option>
                      <option value="Trip Analytics">Trip Analytics</option>
                      <option value="Incident Report">Incident & SOS</option>
                      <option value="System Audit">System Audit</option>
                    </select>
                  </div>

                  <div>
                    <label className="form-label">Export Format</label>
                    <select
                      className="form-control"
                      value={newReport.format}
                      onChange={e => setNewReport({ ...newReport, format: e.target.value })}
                    >
                      <option value="PDF">PDF Document (.pdf)</option>
                      <option value="CSV">Comma Separated (.csv)</option>
                      <option value="XLSX">Excel Workbook (.xlsx)</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label className="form-label">Date Window</label>
                  <select
                    className="form-control"
                    value={newReport.date_range}
                    onChange={e => setNewReport({ ...newReport, date_range: e.target.value })}
                  >
                    <option value="Last 7 Days">Last 7 Days</option>
                    <option value="Last 30 Days">Last 30 Days</option>
                    <option value="Current Quarter (Q3 2026)">Current Quarter (Q3 2026)</option>
                    <option value="Year to Date">Year to Date</option>
                  </select>
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowGenerateModal(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                >
                  Compile & Generate
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
