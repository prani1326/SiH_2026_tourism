import React from 'react';
import { Download, ChevronRight } from 'lucide-react';

export default function PageHeader({
  title,
  description,
  breadcrumbs = ['Dashboard'],
  primaryAction = null,
  secondaryActions = null,
  onExport = null,
  exportLabel = 'Export CSV',
  onExportCSV = null
}) {
  const handleExport = onExport || onExportCSV;

  return (
    <div className="page-header-container">
      <div className="page-title-box">
        {breadcrumbs && Array.isArray(breadcrumbs) && breadcrumbs.length > 0 && (
          <div className="page-breadcrumb">
            {breadcrumbs.map((crumb, idx) => {
              const isLast = idx === breadcrumbs.length - 1;
              const text = typeof crumb === 'string' ? crumb : (crumb?.label || 'Page');
              return (
                <React.Fragment key={idx}>
                  <span className={isLast ? 'active' : ''}>{text}</span>
                  {!isLast && <ChevronRight size={12} />}
                </React.Fragment>
              );
            })}
          </div>
        )}
        <h1 className="page-main-heading">{title}</h1>
        {description && <p className="page-description">{description}</p>}
      </div>

      <div className="page-actions-bar">
        {secondaryActions}

        {handleExport && (
          <button 
            type="button" 
            className="btn btn-outline" 
            onClick={handleExport}
            title="Download CSV report"
          >
            <Download size={14} />
            <span>{exportLabel}</span>
          </button>
        )}

        {/* Support both React element and config object for primaryAction */}
        {React.isValidElement(primaryAction) ? (
          primaryAction
        ) : primaryAction && typeof primaryAction === 'object' && primaryAction.label ? (
          <button
            type="button"
            className="btn btn-primary"
            onClick={primaryAction.onClick}
          >
            {primaryAction.icon && React.createElement(primaryAction.icon, { size: 15 })}
            <span>{primaryAction.label}</span>
          </button>
        ) : null}
      </div>
    </div>
  );
}
