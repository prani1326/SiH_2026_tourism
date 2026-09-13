import React from 'react';

export default function StatCard({ title, value, icon: Icon, color = 'primary' }) {
  return (
    <div className="stat-card">
      <div>
        <div className="stat-title">{title}</div>
        <div className="stat-value">{value}</div>
      </div>
      <div className={`stat-icon-wrapper`}>
        {Icon && <Icon size={22} />}
      </div>
    </div>
  );
}

