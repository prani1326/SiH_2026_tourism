import React, { useMemo } from 'react';
import PageHeader from '../components/PageHeader';
import KPIStatCard from '../components/KPIStatCard';
import DataTable from '../components/DataTable';
import StatusBadge from '../components/StatusBadge';
import { UserCheck, Shield, Award, Clock, MapPin, CheckCircle2, Ticket } from 'lucide-react';

const DEFAULT_LEADERS = [
  {
    id: 1,
    leader_id: 'LDR-3001',
    name: 'Captain Suresh Menon',
    role: 'Chief Field Guide',
    assigned_work: 'Himachal & Solang Circuit Lead',
    approvals_count: 14,
    tickets_handled_count: 8,
    incidents_created_count: 2,
    activity_history: 'Led Solang Valley glacier safety inspection and certified 14 expedition clearances.'
  },
  {
    id: 2,
    leader_id: 'LDR-3002',
    name: 'Priya Sharma',
    role: 'Coastal Operations Lead',
    assigned_work: 'Goa Coastal Watersports & Marine Safety',
    approvals_count: 19,
    tickets_handled_count: 12,
    incidents_created_count: 1,
    activity_history: 'Coordinated scuba diving safety briefing and certified Grand Island boat transfers.'
  },
  {
    id: 3,
    leader_id: 'LDR-3003',
    name: 'Amit Patel',
    role: 'Desert & Heritage Specialist',
    assigned_work: 'Rajasthan & Jaisalmer Trail Lead',
    approvals_count: 11,
    tickets_handled_count: 6,
    incidents_created_count: 0,
    activity_history: 'Supervised Sam Sand Dunes evening camel safari and tourist emergency police liaison.'
  },
  {
    id: 4,
    leader_id: 'LDR-3004',
    name: 'Tenzing Norbu',
    role: 'High Altitude Expedition Guide',
    assigned_work: 'Ladakh & Khardung La Circuit',
    approvals_count: 16,
    tickets_handled_count: 9,
    incidents_created_count: 1,
    activity_history: 'Managed medical emergency oxygen supply and transit evacuation.'
  }
];

export default function LeaderPage({ leaders = [], onViewDetail }) {
  const activeLeaders = useMemo(() => {
    if (Array.isArray(leaders) && leaders.length > 0) {
      return leaders;
    }
    return DEFAULT_LEADERS;
  }, [leaders]);

  const totalCount = activeLeaders.length;
  const activeCount = totalCount;
  const totalApprovals = activeLeaders.reduce((sum, l) => sum + Number(l.approvals_count || 0), 0);
  const totalTicketsHandled = activeLeaders.reduce((sum, l) => sum + Number(l.tickets_handled_count || 0), 0);

  const columns = [
    {
      key: 'name',
      label: 'Leader Profile',
      render: (val, row) => {
        const initials = (val || 'L').split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div 
              className="user-avatar" 
              style={{
                background: 'linear-gradient(135deg, #8b5cf6, #6366f1)',
                width: '38px',
                height: '38px'
              }}
            >
              {initials}
            </div>
            <div>
              <div style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{val}</div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{row.leader_id}</div>
            </div>
          </div>
        );
      }
    },
    {
      key: 'role',
      label: 'Operational Role',
      render: (val) => (
        <span className="badge badge-purple" style={{ fontWeight: 600 }}>
          {val || 'Operations Lead'}
        </span>
      )
    },
    {
      key: 'assigned_work',
      label: 'Assigned Circuit / Zone',
      render: (val) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '5px', fontSize: '0.8rem', color: 'var(--text-primary)' }}>
          <MapPin size={13} color="var(--primary)" />
          <span>{val || 'General Operations'}</span>
        </div>
      )
    },
    {
      key: 'approvals_count',
      label: 'Trip Approvals',
      render: (val) => (
        <span style={{ fontWeight: 700, color: '#10b981', fontSize: '0.85rem' }}>
          {val || 0} Cleared
        </span>
      )
    },
    {
      key: 'tickets_handled_count',
      label: 'Tickets Solved',
      render: (val) => (
        <span style={{ fontWeight: 700, color: '#3b82f6', fontSize: '0.85rem' }}>
          {val || 0} Cases
        </span>
      )
    },
    {
      key: 'status',
      label: 'Ground Status',
      render: () => <StatusBadge status="Active" />
    }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <PageHeader
        title="Leader & Field Ops Registry"
        description="Monitor tour leaders, ground marshals, safety inspectors, and their assigned field circuits."
        breadcrumbs={['TravelHub', 'Leader / Ops']}
      />

      {/* Top 4 Enterprise KPI Stat Cards */}
      <div className="stats-grid">
        <KPIStatCard
          label="Total Field Leads"
          value={totalCount}
          change="100% active on duty"
          isPositive={true}
          icon={UserCheck}
          scheme="primary"
          sparklineData={[2, 3, 3, 3, 4, 4, totalCount]}
        />
        <KPIStatCard
          label="Ground Marshals On Duty"
          value={activeCount}
          change="Assigned to live circuits"
          isPositive={true}
          icon={Shield}
          scheme="success"
          sparklineData={[2, 2, 3, 3, 4, 4, activeCount]}
        />
        <KPIStatCard
          label="Certified Trip Approvals"
          value={totalApprovals}
          change="Pre-dispatch verifications"
          isPositive={true}
          icon={CheckCircle2}
          scheme="info"
          sparklineData={[12, 18, 24, 32, 45, 52, totalApprovals]}
        />
        <KPIStatCard
          label="Escalations Resolved"
          value={totalTicketsHandled}
          change="Zero active disputes"
          isPositive={true}
          icon={Award}
          scheme="warning"
          sparklineData={[5, 8, 12, 16, 22, 28, totalTicketsHandled]}
        />
      </div>

      {/* Central Leader Registry DataTable */}
      <DataTable
        title="Central Tour Leaders & Field Operations Directory"
        subtitle="Certified personnel managing live tourist circuits & medical safety"
        columns={columns}
        data={activeLeaders}
        onViewDetail={onViewDetail}
        searchPlaceholder="Search leader by name, zone, role..."
      />
    </div>
  );
}
