import React from 'react';

export default function LoadingSkeleton({ type = 'table', count = 5 }) {
  if (type === 'cards') {
    return (
      <div className="stats-grid">
        {[1, 2, 3, 4].map(i => (
          <div key={i} className="kpi-card" style={{ height: '140px' }}>
            <div className="skeleton" style={{ width: '40%', height: '16px', marginBottom: '16px' }} />
            <div className="skeleton" style={{ width: '60%', height: '32px', marginBottom: '12px' }} />
            <div className="skeleton" style={{ width: '80%', height: '14px' }} />
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="table-card">
      <div style={{ padding: '20px 24px', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between' }}>
        <div className="skeleton" style={{ width: '220px', height: '24px' }} />
        <div className="skeleton" style={{ width: '140px', height: '32px' }} />
      </div>
      <div style={{ padding: '20px 24px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
        {Array.from({ length: count }).map((_, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div className="skeleton" style={{ width: '36px', height: '36px', borderRadius: '50%' }} />
            <div className="skeleton" style={{ flex: 1, height: '20px' }} />
            <div className="skeleton" style={{ width: '120px', height: '20px' }} />
            <div className="skeleton" style={{ width: '80px', height: '24px', borderRadius: '12px' }} />
          </div>
        ))}
      </div>
    </div>
  );
}
