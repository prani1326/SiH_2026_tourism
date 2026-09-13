import React from 'react';
import { SearchX, Plus, RefreshCw } from 'lucide-react';

export default function EmptyState({
  title = 'No records found',
  description = 'Try adjusting your search query or active filter criteria to find what you are looking for.',
  actionLabel = null,
  onAction = null,
  icon: Icon = SearchX
}) {
  return (
    <div className="empty-state-box">
      <div className="empty-state-icon">
        <Icon size={28} />
      </div>
      <h4 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '6px' }}>
        {title}
      </h4>
      <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', maxWidth: '420px', lineHeight: 1.5, marginBottom: actionLabel ? '16px' : '0' }}>
        {description}
      </p>
      {actionLabel && onAction && (
        <button type="button" className="btn btn-primary btn-sm" onClick={onAction}>
          <span>{actionLabel}</span>
        </button>
      )}
    </div>
  );
}
