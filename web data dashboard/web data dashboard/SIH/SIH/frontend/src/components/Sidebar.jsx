import React from 'react';
import { 
  ChevronLeft, 
  ChevronRight
} from 'lucide-react';
import { SECTIONS } from '../constants/navigation';

export default function Sidebar({
  activeSection,
  setActiveSection,
  collapsed = false,
  setCollapsed,
  mobileOpen = false,
  setMobileOpen,
  badgeCounts = {}
}) {
  const categories = ['MAIN', 'OPERATIONS', 'INTELLIGENCE & SYSTEM'];

  return (
    <>
      {/* Mobile Drawer Backdrop */}
      <div 
        className={`mobile-drawer-backdrop ${mobileOpen ? 'active' : ''}`}
        onClick={() => setMobileOpen && setMobileOpen(false)}
      />

      <aside className={`sidebar ${collapsed ? 'collapsed' : ''} ${mobileOpen ? 'mobile-open' : ''}`}>
        {/* Brand Header */}
        <div className="sidebar-header">
          <div className="brand-wrapper">
            <div className="brand-icon-box">
              TH
            </div>
            {!collapsed && (
              <div className="brand-info">
                <span className="brand-name">Travel<span>Hub</span></span>
                <span className="brand-sub">Operations Console</span>
              </div>
            )}
          </div>

          <button
            type="button"
            className="sidebar-toggle-btn"
            style={{ display: collapsed ? 'none' : 'flex' }}
            onClick={() => setCollapsed && setCollapsed(!collapsed)}
            title={collapsed ? 'Expand Sidebar' : 'Collapse Sidebar'}
          >
            <ChevronLeft size={16} />
          </button>
        </div>

        {/* Navigation List */}
        <nav className="sidebar-nav">
          {categories.map((cat) => {
            const catSections = SECTIONS.filter(s => s.category === cat);
            return (
              <div key={cat} style={{ marginBottom: '8px' }}>
                {!collapsed && (
                  <div className="nav-category-title">{cat}</div>
                )}
                {catSections.map((sec) => {
                  const Icon = sec.icon;
                  const isActive = activeSection === sec.id;
                  const badgeCount = sec.badgeKey ? badgeCounts[sec.badgeKey] : 0;

                  return (
                    <button
                      key={sec.id}
                      type="button"
                      className={`nav-item ${isActive ? 'active' : ''}`}
                      onClick={() => {
                        setActiveSection(sec.id);
                        if (setMobileOpen) setMobileOpen(false);
                      }}
                      title={collapsed ? sec.label : undefined}
                    >
                      <div className="nav-left">
                        <Icon className="nav-icon" />
                        {!collapsed && <span className="nav-label">{sec.label}</span>}
                      </div>

                      {!collapsed && badgeCount > 0 && (
                        <span className={`nav-badge ${
                          sec.badgeKey === 'safety' ? 'nav-badge-danger' : 
                          sec.badgeKey === 'approvals' ? 'nav-badge-warning' : 'nav-badge-info'
                        }`}>
                          {badgeCount}
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            );
          })}
        </nav>

        {/* Collapsed Expand Trigger at Bottom */}
        {collapsed && (
          <div style={{ padding: '12px', display: 'flex', justifyContent: 'center' }}>
            <button
              type="button"
              className="sidebar-toggle-btn"
              onClick={() => setCollapsed && setCollapsed(false)}
              title="Expand Sidebar"
            >
              <ChevronRight size={16} />
            </button>
          </div>
        )}

        {/* Console Health Footer */}
        {!collapsed && (
          <div className="sidebar-footer">
            <div className="live-status-pill">
              <span className="pulse-dot" />
              <span>Console Online (v2.4)</span>
            </div>
          </div>
        )}
      </aside>
    </>
  );
}
