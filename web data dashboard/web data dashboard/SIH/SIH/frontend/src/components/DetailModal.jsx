import React from 'react';
import { X } from 'lucide-react';

export default function DetailModal({ title, data, onClose }) {
  if (!data) return null;

  const entries = Object.entries(data);

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">{title || 'Data Inspector'}</h3>
          <button className="btn btn-outline" style={{ padding: '4px 8px' }} onClick={onClose}>
            <X size={16} />
          </button>
        </div>
        <div className="modal-grid">
          {entries.map(([key, val]) => (
            <div key={key} className={`modal-field ${typeof val === 'string' && val.length > 40 ? 'full-width' : ''}`}>
              <span className="field-label">{key.replace(/_/g, ' ')}</span>
              <span className="field-value">{val !== null && val !== undefined ? String(val) : 'N/A'}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

