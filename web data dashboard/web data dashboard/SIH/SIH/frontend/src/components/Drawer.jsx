import React from 'react';
import { X, Copy, Check } from 'lucide-react';
import StatusBadge from './StatusBadge';

export default function Drawer({
  isOpen,
  onClose,
  title = 'Record Inspector',
  subtitle = 'Central database record overview',
  data = null,
  actions = null
}) {
  const [copiedKey, setCopiedKey] = React.useState(null);

  if (!isOpen || !data) return null;

  const handleCopy = (text, key) => {
    navigator.clipboard.writeText(String(text));
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 2000);
  };

  // Format label from object key
  const formatLabel = (k) => {
    return k
      .replace(/_/g, ' ')
      .replace(/\b\w/g, c => c.toUpperCase());
  };

  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <div className="drawer-panel" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="drawer-header">
          <div>
            <h3 className="drawer-title">{title}</h3>
            <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>{subtitle}</p>
          </div>
          <button 
            type="button" 
            className="btn btn-outline btn-icon"
            onClick={onClose}
          >
            <X size={18} />
          </button>
        </div>

        {/* Body */}
        <div className="drawer-body">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {Object.entries(data).map(([key, value]) => {
              if (key === 'id') return null; // skip internal id
              const valStr = value !== null && value !== undefined ? String(value) : '—';
              const isStatus = key.toLowerCase().includes('status') || key.toLowerCase().includes('priority');

              return (
                <div 
                  key={key} 
                  style={{
                    padding: '12px 14px',
                    borderRadius: '8px',
                    backgroundColor: 'var(--bg-surface-secondary)',
                    border: '1px solid var(--border-color)',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '4px'
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                      {formatLabel(key)}
                    </span>
                    <button
                      type="button"
                      onClick={() => handleCopy(valStr, key)}
                      style={{
                        background: 'transparent',
                        border: 'none',
                        color: 'var(--text-muted)',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '4px',
                        fontSize: '0.7rem'
                      }}
                      title="Copy to clipboard"
                    >
                      {copiedKey === key ? <Check size={12} color="#10b981" /> : <Copy size={12} />}
                    </button>
                  </div>

                  <div style={{ fontSize: '0.9rem', color: 'var(--text-primary)', fontWeight: 500, wordBreak: 'break-word' }}>
                    {isStatus ? <StatusBadge status={valStr} /> : valStr}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Footer */}
        <div className="drawer-footer">
          <button type="button" className="btn btn-outline" onClick={onClose}>
            Close
          </button>
          {actions}
        </div>
      </div>
    </div>
  );
}
