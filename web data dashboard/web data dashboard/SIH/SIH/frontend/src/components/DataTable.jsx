import React, { useState } from 'react';
import { Eye, Search, ArrowUpDown, ChevronLeft, ChevronRight, Download, Filter } from 'lucide-react';
import StatusBadge from './StatusBadge';
import EmptyState from './EmptyState';

export default function DataTable({
  title,
  subtitle = null,
  columns,
  data = [],
  onViewDetail,
  primaryAction = null,
  onExport = null,
  enableSearch = true,
  searchPlaceholder = 'Search records...'
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const [sortKey, setSortKey] = useState(null);
  const [sortOrder, setSortOrder] = useState('asc'); // 'asc' | 'desc'
  const [currentPage, setCurrentPage] = useState(1);
  const rowsPerPage = 7;

  // Safe Data Filter
  const safeData = Array.isArray(data) ? data : [];
  const filteredData = safeData.filter((row) => {
    if (!row || typeof row !== 'object') return false;
    return Object.values(row).some(
      (val) => val && String(val).toLowerCase().includes(searchTerm.toLowerCase())
    );
  });

  // Sort
  const sortedData = [...filteredData].sort((a, b) => {
    if (!sortKey || !a || !b) return 0;
    const valA = a[sortKey] ?? '';
    const valB = b[sortKey] ?? '';
    if (typeof valA === 'number' && typeof valB === 'number') {
      return sortOrder === 'asc' ? valA - valB : valB - valA;
    }
    return sortOrder === 'asc'
      ? String(valA).localeCompare(String(valB))
      : String(valB).localeCompare(String(valA));
  });

  // Pagination
  const totalPages = Math.ceil(sortedData.length / rowsPerPage) || 1;
  const paginatedData = sortedData.slice((currentPage - 1) * rowsPerPage, currentPage * rowsPerPage);

  const handleSort = (key) => {
    if (sortKey === key) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortKey(key);
      setSortOrder('asc');
    }
  };

  // CSV Export
  const exportToCSV = () => {
    if (!safeData.length) return;
    const headers = columns.map(c => c.label).join(',');
    const rows = filteredData.map(row => 
      columns.map(c => `"${String(row[c.key] ?? '').replace(/"/g, '""')}"`).join(',')
    ).join('\n');

    const blob = new Blob([`${headers}\n${rows}`], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${(title || 'export').toLowerCase().replace(/[\s\/\\]+/g, '_')}_export.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="table-card">
      {/* Table Header with Search and Actions */}
      <div className="table-header">
        <div className="table-title-box">
          <h3 className="table-title">{title} ({filteredData.length})</h3>
          {subtitle && <span className="table-subtitle">{subtitle}</span>}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
          {enableSearch && (
            <div style={{ position: 'relative' }}>
              <Search size={15} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="text"
                className="search-input"
                style={{ paddingLeft: '32px', width: '220px' }}
                placeholder={searchPlaceholder}
                value={searchTerm}
                onChange={(e) => {
                  setSearchTerm(e.target.value);
                  setCurrentPage(1);
                }}
              />
            </div>
          )}

          <button
            type="button"
            className="btn btn-outline btn-sm"
            onClick={onExport || exportToCSV}
            title="Download CSV"
          >
            <Download size={14} />
            <span>Export</span>
          </button>

          {React.isValidElement(primaryAction) ? (
            primaryAction
          ) : primaryAction && typeof primaryAction === 'object' && primaryAction.label ? (
            <button
              type="button"
              className="btn btn-primary btn-sm"
              onClick={primaryAction.onClick}
            >
              {primaryAction.icon && React.createElement(primaryAction.icon, { size: 14 })}
              <span>{primaryAction.label}</span>
            </button>
          ) : null}
        </div>
      </div>

      {/* Data Table */}
      <div className="data-table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              {columns.map((col) => (
                <th
                  key={col.key}
                  onClick={() => handleSort(col.key)}
                  style={{ cursor: 'pointer', userSelect: 'none' }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <span>{col.label}</span>
                    <ArrowUpDown size={12} style={{ opacity: sortKey === col.key ? 1 : 0.3 }} />
                  </div>
                </th>
              ))}
              {onViewDetail && <th style={{ textAlign: 'center' }}>Action</th>}
            </tr>
          </thead>
          <tbody>
            {paginatedData.length === 0 ? (
              <tr>
                <td colSpan={columns.length + (onViewDetail ? 1 : 0)}>
                  <EmptyState 
                    title="No matching records"
                    description={`No records found matching "${searchTerm}". Click reset to clear search.`}
                    actionLabel={searchTerm ? "Clear Search" : null}
                    onAction={() => setSearchTerm('')}
                  />
                </td>
              </tr>
            ) : (
              paginatedData.map((row, idx) => (
                <tr key={row?.id || row?.trip_id || row?.booking_id || row?.event_id || row?.ticket_id || idx}>
                  {columns.map((col) => {
                    const val = row ? row[col.key] : undefined;
                    const isStatus = col.isBadge || col.key.toLowerCase().includes('status');

                    // If custom col.render is provided, call with (val, row)
                    let renderedCell;
                    if (col.render) {
                      renderedCell = col.render(val, row);
                    } else if (isStatus) {
                      renderedCell = <StatusBadge status={val} />;
                    } else {
                      renderedCell = val ?? '—';
                    }

                    return (
                      <td key={col.key}>
                        {renderedCell}
                      </td>
                    );
                  })}
                  {onViewDetail && (
                    <td style={{ textAlign: 'center' }}>
                      <button
                        type="button"
                        className="btn btn-outline btn-sm"
                        onClick={() => onViewDetail(row)}
                        title="View Record Details"
                        style={{ padding: '4px 8px' }}
                      >
                        <Eye size={13} />
                        <span>Inspect</span>
                      </button>
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer */}
      {sortedData.length > 0 && (
        <div className="table-pagination">
          <span>
            Showing {(currentPage - 1) * rowsPerPage + 1} to {Math.min(currentPage * rowsPerPage, sortedData.length)} of {sortedData.length} records
          </span>

          <div className="pagination-controls">
            <button
              type="button"
              className="pagination-btn"
              disabled={currentPage === 1}
              onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
            >
              <ChevronLeft size={14} />
            </button>
            <span style={{ fontSize: '0.8rem', fontWeight: 600 }}>
              {currentPage} / {totalPages}
            </span>
            <button
              type="button"
              className="pagination-btn"
              disabled={currentPage >= totalPages}
              onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
            >
              <ChevronRight size={14} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
