import React from 'react';
import { CheckCircle2, Clock, XCircle, AlertCircle, ShieldAlert, Sparkles } from 'lucide-react';

export default function StatusBadge({ status }) {
  if (!status) return <span className="badge badge-secondary">—</span>;

  const s = String(status).trim().toLowerCase();

  if (['verified', 'accepted', 'approved', 'active', 'completed', 'published', 'resolved', 'paid'].includes(s)) {
    return (
      <span className="badge badge-success">
        <CheckCircle2 size={12} />
        <span>{status}</span>
      </span>
    );
  }

  if (['pending', 'pending approval', 'in progress', 'planned', 'dispatched', 'in transit'].includes(s)) {
    return (
      <span className="badge badge-warning">
        <Clock size={12} />
        <span>{status}</span>
      </span>
    );
  }

  if (['rejected', 'cancelled', 'blocked', 'critical', 'high', 'failed'].includes(s)) {
    return (
      <span className="badge badge-danger">
        <XCircle size={12} />
        <span>{status}</span>
      </span>
    );
  }

  if (['open', 'idle', 'draft', 'on trip'].includes(s)) {
    return (
      <span className="badge badge-info">
        <Sparkles size={12} />
        <span>{status}</span>
      </span>
    );
  }

  if (['emergency', 'sos alert', 'sos'].includes(s)) {
    return (
      <span className="badge badge-danger" style={{ animation: 'pulse-ring 1.5s infinite' }}>
        <ShieldAlert size={12} />
        <span>{status}</span>
      </span>
    );
  }

  return <span className="badge badge-secondary">{status}</span>;
}
