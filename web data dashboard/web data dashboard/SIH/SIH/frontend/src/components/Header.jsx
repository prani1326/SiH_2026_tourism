import React, { useState, useRef, useEffect } from 'react';
import { 
  Menu, 
  Search, 
  Bell, 
  Sun, 
  Moon, 
  Radio, 
  RefreshCw, 
  ChevronDown, 
  User, 
  Shield, 
  LogOut, 
  Sparkles,
  Smartphone,
  ExternalLink,
  CheckCircle2
} from 'lucide-react';

export default function Header({
  onRefresh,
  onSimulate,
  theme = 'light',
  onToggleTheme,
  onToggleSidebar,
  onOpenMobileSidebar,
  notifications = [],
  unreadCount = 0,
  refreshing = false
}) {
  const [showNotifications, setShowNotifications] = useState(false);
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const notifRef = useRef(null);
  const profileRef = useRef(null);

  // Close dropdowns on outside click
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (notifRef.current && !notifRef.current.contains(e.target)) {
        setShowNotifications(false);
      }
      if (profileRef.current && !profileRef.current.contains(e.target)) {
        setShowProfileMenu(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <header className="top-header">
      {/* Left: Mobile Menu Trigger + Omni-search */}
      <div className="header-left">
        {/* Mobile Hamburger */}
        <button
          type="button"
          className="sidebar-toggle-btn"
          onClick={onOpenMobileSidebar}
          style={{ display: 'none' }}
          id="mobile-menu-btn"
        >
          <Menu size={18} />
        </button>

        {/* Desktop Sidebar Toggle */}
        <button
          type="button"
          className="sidebar-toggle-btn"
          onClick={onToggleSidebar}
          title="Toggle Sidebar"
        >
          <Menu size={18} />
        </button>

        {/* Global Search Bar */}
        <div className="header-search-bar">
          <Search size={16} className="header-search-icon" />
          <input
            type="text"
            className="header-search-input"
            placeholder="Search destinations, vendors, trips, users..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          <span className="header-search-shortcut">⌘K</span>
        </div>

        {/* Firebase Connected Live Badge */}
        <div className="data-flow-badge" title="Firebase Project: webdashboard-d040c (Firestore Live)">
          <span className="pulse-dot" />
          <Radio size={12} />
          <span>Firebase: webdashboard-d040c</span>
        </div>
      </div>

      {/* Right: Actions Group, Separator, Simulator, Separator, Profile */}
      <div className="header-right">
        {/* Quick Utility Actions */}
        <div className="header-actions-group">
          {/* Sync / Refresh Button */}
          <button
            type="button"
            className="header-icon-btn"
            onClick={onRefresh}
            title="Sync Central Database"
            disabled={refreshing}
          >
            <RefreshCw size={16} style={{ animation: refreshing ? 'spin 1s linear infinite' : 'none' }} />
          </button>

          {/* Multi-app Notifications Bell */}
          <div style={{ position: 'relative' }} ref={notifRef}>
            <button
              type="button"
              className="header-icon-btn"
              onClick={() => setShowNotifications(!showNotifications)}
              title="Notification Center"
            >
              <Bell size={16} />
              {unreadCount > 0 && <span className="icon-badge-dot" />}
            </button>

            {/* Notifications Dropdown */}
            {showNotifications && (
              <div className="dropdown-card" style={{ width: '340px' }}>
                <div style={{ padding: '14px 18px', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                    Notifications ({notifications.length})
                  </span>
                  <span style={{ fontSize: '0.7rem', color: '#10b981', fontWeight: 600 }}>
                    Firestore Realtime
                  </span>
                </div>

                <div style={{ maxHeight: '320px', overflowY: 'auto' }}>
                  {notifications.length === 0 ? (
                    <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                      No recent notifications
                    </div>
                  ) : (
                    notifications.slice(0, 6).map((n) => (
                      <div 
                        key={n.id} 
                        style={{
                          padding: '12px 16px',
                          borderBottom: '1px solid var(--border-color)',
                          display: 'flex',
                          flexDirection: 'column',
                          gap: '3px',
                          backgroundColor: n.status === 'unread' ? 'var(--primary-light)' : 'transparent'
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <span style={{ fontSize: '0.7rem', fontWeight: 700, color: n.recipient_type === 'tourist' ? '#2563eb' : '#8b5cf6', textTransform: 'uppercase' }}>
                            {n.recipient_type === 'tourist' ? '📱 Tourist App' : '🛡️ Leader App'}
                          </span>
                          <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>
                            {new Date(n.timestamp || Date.now()).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </span>
                        </div>
                        <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                          {n.title}
                        </span>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', lineHeight: 1.3 }}>
                          {n.message}
                        </span>
                      </div>
                    ))
                  )}
                </div>

                <div style={{ padding: '10px 16px', backgroundColor: 'var(--bg-surface-secondary)', borderTop: '1px solid var(--border-color)', textAlign: 'center' }}>
                  <span style={{ fontSize: '0.75rem', color: 'var(--primary)', fontWeight: 600, cursor: 'pointer' }}>
                    View All in Trip Approvals Desk →
                  </span>
                </div>
              </div>
            )}
          </div>

          {/* Dark / Light Theme Toggle */}
          <button
            type="button"
            className="header-icon-btn"
            onClick={onToggleTheme}
            title={theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
          >
            {theme === 'dark' ? <Sun size={16} color="#f59e0b" /> : <Moon size={16} />}
          </button>
        </div>

        {/* Divider */}
        <div className="header-divider" />

        {/* Simulator Button */}
        <button
          type="button"
          className="btn btn-primary header-action-btn"
          onClick={onSimulate}
          title="Simulate Cross-App Workflow"
        >
          <Sparkles size={14} />
          <span>Simulate Flow</span>
        </button>

        {/* Divider */}
        <div className="header-divider" />

        {/* User Profile Pill & Menu */}
        <div style={{ position: 'relative' }} ref={profileRef}>
          <button
            type="button"
            className="profile-pill-btn"
            onClick={() => setShowProfileMenu(!showProfileMenu)}
          >
            <div className="user-avatar-sm">
              AS
            </div>
            <div className="profile-details">
              <span className="profile-name">Akash Sharma</span>
              <span className="profile-role">Director</span>
            </div>
            <ChevronDown size={14} color="var(--text-muted)" />
          </button>

          {/* Profile Dropdown */}
          {showProfileMenu && (
            <div className="dropdown-card" style={{ width: '220px' }}>
              <div style={{ padding: '14px 16px', borderBottom: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-primary)' }}>Akash Sharma</div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>admin@travelhub.ops</div>
                <div style={{ display: 'inline-flex', alignItems: 'center', gap: '5px', marginTop: '6px', fontSize: '0.7rem', color: '#10b981', fontWeight: 600 }}>
                  <span className="pulse-dot" style={{ width: '6px', height: '6px' }} />
                  <span>Online & Active</span>
                </div>
              </div>

              <div style={{ padding: '6px' }}>
                <button
                  type="button"
                  className="nav-item"
                  style={{ color: 'var(--text-primary)', fontSize: '0.8rem', padding: '8px 12px' }}
                >
                  <User size={15} />
                  <span>Admin Profile</span>
                </button>
                <button
                  type="button"
                  className="nav-item"
                  style={{ color: 'var(--text-primary)', fontSize: '0.8rem', padding: '8px 12px' }}
                >
                  <Shield size={15} />
                  <span>Security & Audit</span>
                </button>
                <div style={{ height: '1px', backgroundColor: 'var(--border-color)', margin: '4px 0' }} />
                <button
                  type="button"
                  className="nav-item"
                  style={{ color: 'var(--danger)', fontSize: '0.8rem', padding: '8px 12px' }}
                  onClick={() => alert('Logged out demo session.')}
                >
                  <LogOut size={15} />
                  <span>Sign Out</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
