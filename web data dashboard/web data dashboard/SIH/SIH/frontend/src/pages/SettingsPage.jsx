import React, { useState } from 'react';
import { 
  Settings as SettingsIcon, 
  Bell, 
  Shield, 
  Layers, 
  Palette, 
  Users, 
  CheckCircle2, 
  Radio, 
  Save, 
  Key, 
  Lock, 
  Sun, 
  Moon, 
  UserPlus, 
  Trash2,
  ExternalLink,
  Sparkles
} from 'lucide-react';
import PageHeader from '../components/PageHeader';
import StatusBadge from '../components/StatusBadge';

export default function SettingsPage({ theme = 'light', onToggleTheme }) {
  const [activeTab, setActiveTab] = useState('general');
  const [saveToast, setSaveToast] = useState(false);

  // General Settings State
  const [generalSettings, setGeneralSettings] = useState({
    platformName: 'TravelHub',
    tagline: 'Central Data & Operations Console',
    supportEmail: 'ops@travelhub.tourism.gov.in',
    emergencyHotline: '+91 1800 11 1363 (Toll Free)',
    timezone: 'Asia/Kolkata (IST +05:30)',
    currency: 'INR (₹)'
  });

  // Notifications State
  const [notifSettings, setNotifSettings] = useState({
    touristPush: true,
    leaderDispatchPush: true,
    sosCriticalSms: true,
    audioAlerts: true,
    weeklyReportEmail: true
  });

  // Admin users list
  const [admins, setAdmins] = useState([
    { id: 'ADM-1', name: 'Akash Sharma', email: 'akash@travelhub.ops', role: 'Super Director', permissions: 'Full Root Access', status: 'Active' },
    { id: 'ADM-2', name: 'Vikram Mehta', email: 'vikram@travelhub.ops', role: 'Operations Lead', permissions: 'Approvals & Dispatch', status: 'Active' },
    { id: 'ADM-3', name: 'Priya Joshi', email: 'priya@travelhub.ops', role: 'Safety Dispatcher', permissions: 'SOS & Incident Radar', status: 'Active' }
  ]);

  const [showAddAdminModal, setShowAddAdminModal] = useState(false);
  const [newAdmin, setNewAdmin] = useState({ name: '', email: '', role: 'Operations Lead' });

  const handleSave = () => {
    setSaveToast(true);
    setTimeout(() => setSaveToast(false), 4000);
  };

  const handleAddAdmin = (e) => {
    e.preventDefault();
    if (!newAdmin.name || !newAdmin.email) return;
    setAdmins([
      ...admins,
      {
        id: `ADM-${Date.now().toString().slice(-3)}`,
        name: newAdmin.name,
        email: newAdmin.email,
        role: newAdmin.role,
        permissions: 'Standard Admin Access',
        status: 'Active'
      }
    ]);
    setShowAddAdminModal(false);
    setNewAdmin({ name: '', email: '', role: 'Operations Lead' });
  };

  const tabs = [
    { id: 'general', label: 'General', icon: SettingsIcon },
    { id: 'notifications', label: 'Notifications', icon: Bell },
    { id: 'security', label: 'Security & Access', icon: Shield },
    { id: 'integrations', label: 'Integrations & Firebase', icon: Layers },
    { id: 'appearance', label: 'Appearance', icon: Palette },
    { id: 'admins', label: 'Admin Management', icon: Users }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '22px' }}>
      {/* Save Success Toast */}
      {saveToast && (
        <div style={{
          backgroundColor: '#065f46',
          color: '#ffffff',
          padding: '12px 18px',
          borderRadius: '8px',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          fontWeight: 600,
          boxShadow: '0 4px 14px rgba(0,0,0,0.15)'
        }}>
          <CheckCircle2 size={18} />
          <span>System configuration preferences saved successfully to Central Cloud!</span>
        </div>
      )}

      {/* Top Page Header */}
      <PageHeader
        title="Settings & System Console"
        description="Configure enterprise metadata, real-time push gateways, Firebase telemetry, and admin credentials."
        breadcrumbs={[
          { label: 'TravelHub' },
          { label: 'Settings', active: true }
        ]}
        primaryAction={{
          label: 'Save Changes',
          icon: Save,
          onClick: handleSave
        }}
      />

      {/* Settings Container with Sidebar Nav */}
      <div style={{ display: 'grid', gridTemplateColumns: '240px 1fr', gap: '22px' }}>
        
        {/* Settings Tab Navigation */}
        <div className="card" style={{ padding: '12px', display: 'flex', flexDirection: 'column', gap: '4px', height: 'fit-content' }}>
          {tabs.map(t => {
            const Icon = t.icon;
            const isActive = activeTab === t.id;
            return (
              <button
                key={t.id}
                type="button"
                onClick={() => setActiveTab(t.id)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  padding: '10px 14px',
                  borderRadius: '6px',
                  border: 'none',
                  backgroundColor: isActive ? 'var(--primary-light)' : 'transparent',
                  color: isActive ? 'var(--primary)' : 'var(--text-secondary)',
                  fontWeight: isActive ? 700 : 500,
                  fontSize: '0.85rem',
                  cursor: 'pointer',
                  textAlign: 'left',
                  transition: 'all 0.15s ease'
                }}
              >
                <Icon size={16} />
                <span>{t.label}</span>
              </button>
            );
          })}
        </div>

        {/* Tab Content Panel */}
        <div className="card" style={{ padding: '24px' }}>
          
          {/* 1. GENERAL TAB */}
          {activeTab === 'general' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                  Platform & Organization Identity
                </h3>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                  Basic metadata presented across internal command interfaces and mobile app payloads.
                </p>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div>
                  <label className="form-label">Platform Name</label>
                  <input
                    type="text"
                    className="form-control"
                    value={generalSettings.platformName}
                    onChange={e => setGeneralSettings({ ...generalSettings, platformName: e.target.value })}
                  />
                </div>
                <div>
                  <label className="form-label">Console Subtitle</label>
                  <input
                    type="text"
                    className="form-control"
                    value={generalSettings.tagline}
                    onChange={e => setGeneralSettings({ ...generalSettings, tagline: e.target.value })}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div>
                  <label className="form-label">Operations Support Email</label>
                  <input
                    type="email"
                    className="form-control"
                    value={generalSettings.supportEmail}
                    onChange={e => setGeneralSettings({ ...generalSettings, supportEmail: e.target.value })}
                  />
                </div>
                <div>
                  <label className="form-label">Central Emergency Hotline</label>
                  <input
                    type="text"
                    className="form-control"
                    value={generalSettings.emergencyHotline}
                    onChange={e => setGeneralSettings({ ...generalSettings, emergencyHotline: e.target.value })}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div>
                  <label className="form-label">Primary Timezone</label>
                  <input
                    type="text"
                    className="form-control"
                    value={generalSettings.timezone}
                    disabled
                  />
                </div>
                <div>
                  <label className="form-label">Default Currency</label>
                  <input
                    type="text"
                    className="form-control"
                    value={generalSettings.currency}
                    disabled
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '12px' }}>
                <button type="button" className="btn btn-primary" onClick={handleSave}>
                  <Save size={14} /> Save General Settings
                </button>
              </div>
            </div>
          )}

          {/* 2. NOTIFICATIONS TAB */}
          {activeTab === 'notifications' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                  Live Broadcast & Notification Channels
                </h3>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                  Manage dispatch protocols for Tourist App, Leader App, and emergency escalation alerts.
                </p>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                {[
                  { key: 'touristPush', title: 'Tourist Mobile App Push Notifications', desc: 'Notify travelers instantly upon trip approval, leader dispatch, and itinerary updates via Firestore' },
                  { key: 'leaderDispatchPush', title: 'Tour Leader Mission Dispatch Orders', desc: 'Push verified assignments, trek briefing dossiers, and traveler contact details to Tour Leader App' },
                  { key: 'sosCriticalSms', title: 'Emergency SOS Dual SMS Gateway', desc: 'Trigger high-priority carrier SMS alerts to state disaster response force (SDRF) upon critical SOS' },
                  { key: 'audioAlerts', title: 'Dashboard Audio Alarm for Live Incidents', desc: 'Play audible acoustic chime when a new distress signal or incident ticket is registered' },
                  { key: 'weeklyReportEmail', title: 'Automated Weekly Executive Briefing', desc: 'Email compiled KPI audit deck every Monday morning to executive administration' }
                ].map(item => (
                  <div 
                    key={item.key}
                    style={{ 
                      display: 'flex', 
                      justifyContent: 'space-between', 
                      alignItems: 'center',
                      padding: '14px 16px',
                      backgroundColor: 'var(--bg-surface-secondary)',
                      borderRadius: '8px',
                      border: '1px solid var(--border-color)'
                    }}
                  >
                    <div>
                      <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.88rem' }}>{item.title}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '2px' }}>{item.desc}</div>
                    </div>
                    <label style={{ position: 'relative', display: 'inline-block', width: '42px', height: '22px', cursor: 'pointer' }}>
                      <input
                        type="checkbox"
                        checked={notifSettings[item.key]}
                        onChange={e => setNotifSettings({ ...notifSettings, [item.key]: e.target.checked })}
                        style={{ opacity: 0, width: 0, height: 0 }}
                      />
                      <span style={{
                        position: 'absolute',
                        cursor: 'pointer',
                        top: 0, left: 0, right: 0, bottom: 0,
                        backgroundColor: notifSettings[item.key] ? 'var(--primary)' : '#cbd5e1',
                        borderRadius: '22px',
                        transition: '0.3s'
                      }}>
                        <span style={{
                          position: 'absolute',
                          content: '""',
                          height: '16px',
                          width: '16px',
                          left: notifSettings[item.key] ? '22px' : '3px',
                          bottom: '3px',
                          backgroundColor: '#ffffff',
                          borderRadius: '50%',
                          transition: '0.3s'
                        }} />
                      </span>
                    </label>
                  </div>
                ))}
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '12px' }}>
                <button type="button" className="btn btn-primary" onClick={handleSave}>
                  <Save size={14} /> Update Notification Protocols
                </button>
              </div>
            </div>
          )}

          {/* 3. SECURITY TAB */}
          {activeTab === 'security' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                  Authentication, Sessions & RBAC
                </h3>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                  Enterprise zero-trust perimeter configuration and admin credentials policy.
                </p>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div style={{ padding: '16px', backgroundColor: 'var(--bg-surface-secondary)', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.88rem' }}>Multi-Factor Authentication (MFA)</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Enforce TOTP authenticator (Google Authenticator / YubiKey) for all console admins.</div>
                    </div>
                    <span style={{ backgroundColor: 'rgba(16, 185, 129, 0.1)', color: '#10b981', padding: '3px 10px', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 700 }}>
                      ENFORCED
                    </span>
                  </div>
                </div>

                <div style={{ padding: '16px', backgroundColor: 'var(--bg-surface-secondary)', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.88rem' }}>Admin Session Inactivity Timeout</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Automatically invalidate inactive administrator tokens after designated period.</div>
                    </div>
                    <select className="form-control" style={{ width: '140px', fontSize: '0.8rem' }}>
                      <option>15 Minutes</option>
                      <option>30 Minutes</option>
                      <option>1 Hour</option>
                      <option>4 Hours</option>
                    </select>
                  </div>
                </div>

                <div style={{ padding: '16px', backgroundColor: 'var(--bg-surface-secondary)', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.88rem' }}>Firestore Cloud Security Audit</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Rules evaluated: Read/Write active on collections <code>users, vendors, trips, notifications</code></div>
                    </div>
                    <span style={{ backgroundColor: 'rgba(59, 130, 246, 0.1)', color: '#3b82f6', padding: '3px 10px', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 700 }}>
                      SECURE
                    </span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* 4. INTEGRATIONS TAB */}
          {activeTab === 'integrations' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                  Connected Cloud Backends & APIs
                </h3>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                  Live heartbeat status of databases, payment gateways, and geospatial servers.
                </p>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div style={{ padding: '16px', backgroundColor: 'var(--bg-surface-secondary)', borderRadius: '8px', border: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <div style={{ width: '36px', height: '36px', borderRadius: '8px', backgroundColor: '#ffca28', color: '#000', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800 }}>
                      🔥
                    </div>
                    <div>
                      <div style={{ fontWeight: 700, color: 'var(--text-primary)', fontSize: '0.9rem' }}>Google Cloud Firestore</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Project: <code>webdashboard-d040c</code> | Real-time WebSockets Sync</div>
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#10b981', fontWeight: 700, fontSize: '0.8rem' }}>
                    <span className="pulse-dot" style={{ width: '8px', height: '8px' }} />
                    <span>OPERATIONAL (0ms lag)</span>
                  </div>
                </div>

                <div style={{ padding: '16px', backgroundColor: 'var(--bg-surface-secondary)', borderRadius: '8px', border: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <div style={{ width: '36px', height: '36px', borderRadius: '8px', backgroundColor: 'rgba(37, 99, 235, 0.15)', color: '#2563eb', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800 }}>
                      🗺️
                    </div>
                    <div>
                      <div style={{ fontWeight: 700, color: 'var(--text-primary)', fontSize: '0.9rem' }}>Geospatial Telemetry & GIS Mapping</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>High-altitude topographic circuits & live guide tracking</div>
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#10b981', fontWeight: 700, fontSize: '0.8rem' }}>
                    <span className="pulse-dot" style={{ width: '8px', height: '8px' }} />
                    <span>CONNECTED</span>
                  </div>
                </div>

                <div style={{ padding: '16px', backgroundColor: 'var(--bg-surface-secondary)', borderRadius: '8px', border: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <div style={{ width: '36px', height: '36px', borderRadius: '8px', backgroundColor: 'rgba(16, 185, 129, 0.15)', color: '#10b981', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800 }}>
                      💳
                    </div>
                    <div>
                      <div style={{ fontWeight: 700, color: 'var(--text-primary)', fontSize: '0.9rem' }}>National Tourism Payment Gateway (UPI / NetBanking)</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Instant settlement escrow for verified trip bookings</div>
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#10b981', fontWeight: 700, fontSize: '0.8rem' }}>
                    <span className="pulse-dot" style={{ width: '8px', height: '8px' }} />
                    <span>ACTIVE</span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* 5. APPEARANCE TAB */}
          {activeTab === 'appearance' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                  Console Theme & Interface Experience
                </h3>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                  Tailor high-contrast operational palettes and night vision mode for control center monitoring.
                </p>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div 
                  onClick={() => onToggleTheme && theme !== 'light' && onToggleTheme()}
                  style={{
                    padding: '20px',
                    borderRadius: '8px',
                    border: theme === 'light' ? '2px solid var(--primary)' : '1px solid var(--border-color)',
                    backgroundColor: '#ffffff',
                    color: '#0f172a',
                    cursor: 'pointer',
                    boxShadow: theme === 'light' ? '0 0 0 3px rgba(37, 99, 235, 0.2)' : 'none'
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Sun size={18} color="#f59e0b" />
                      <strong style={{ fontSize: '0.95rem' }}>Light Mode</strong>
                    </div>
                    {theme === 'light' && <CheckCircle2 size={18} color="#2563eb" />}
                  </div>
                  <p style={{ fontSize: '0.78rem', color: '#64748b', marginTop: '10px' }}>
                    Crisp white surfaces, soft blue canvas, navy typography. Ideal for daylight office auditing.
                  </p>
                </div>

                <div 
                  onClick={() => onToggleTheme && theme !== 'dark' && onToggleTheme()}
                  style={{
                    padding: '20px',
                    borderRadius: '8px',
                    border: theme === 'dark' ? '2px solid var(--primary)' : '1px solid var(--border-color)',
                    backgroundColor: '#0b1329',
                    color: '#f8fafc',
                    cursor: 'pointer',
                    boxShadow: theme === 'dark' ? '0 0 0 3px rgba(37, 99, 235, 0.2)' : 'none'
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Moon size={18} color="#38bdf8" />
                      <strong style={{ fontSize: '0.95rem' }}>Dark Mode (Command Deck)</strong>
                    </div>
                    {theme === 'dark' && <CheckCircle2 size={18} color="#38bdf8" />}
                  </div>
                  <p style={{ fontSize: '0.78rem', color: '#94a3b8', marginTop: '10px' }}>
                    Deep obsidian & midnight navy surfaces, low glare, optimized for 24/7 mission operations.
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* 6. ADMIN MANAGEMENT TAB */}
          {activeTab === 'admins' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                    Central Command Administrators
                  </h3>
                  <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                    Authorized personnel with administrative clearance to review, approve, and dispatch operations.
                  </p>
                </div>
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={() => setShowAddAdminModal(true)}
                >
                  <UserPlus size={14} /> Add Administrator
                </button>
              </div>

              <div style={{ overflowX: 'auto' }}>
                <table className="data-table" style={{ width: '100%' }}>
                  <thead>
                    <tr>
                      <th>Administrator</th>
                      <th>Console Role</th>
                      <th>Permissions</th>
                      <th>Status</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {admins.map(adm => (
                      <tr key={adm.id}>
                        <td>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                            <div style={{ width: '32px', height: '32px', borderRadius: '50%', backgroundColor: 'var(--primary)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: '0.75rem' }}>
                              {adm.name.slice(0, 2).toUpperCase()}
                            </div>
                            <div>
                              <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{adm.name}</div>
                              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{adm.email}</div>
                            </div>
                          </div>
                        </td>
                        <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>{adm.role}</td>
                        <td style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{adm.permissions}</td>
                        <td>
                          <StatusBadge status={adm.status} />
                        </td>
                        <td>
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            style={{ padding: '3px 8px', fontSize: '0.72rem', color: 'var(--danger)' }}
                            onClick={() => {
                              if (admins.length > 1) setAdmins(admins.filter(a => a.id !== adm.id));
                            }}
                          >
                            <Trash2 size={12} />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

        </div>
      </div>

      {/* Add Admin Modal */}
      {showAddAdminModal && (
        <div className="modal-overlay" onClick={() => setShowAddAdminModal(false)}>
          <div className="modal-card" style={{ maxWidth: '480px' }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">Authorize Administrator</h3>
              <button 
                type="button" 
                className="btn btn-secondary btn-sm" 
                onClick={() => setShowAddAdminModal(false)}
                style={{ padding: '4px 8px' }}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleAddAdmin}>
              <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div>
                  <label className="form-label">Full Name *</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. Neha Verma"
                    value={newAdmin.name}
                    onChange={e => setNewAdmin({ ...newAdmin, name: e.target.value })}
                    required
                  />
                </div>

                <div>
                  <label className="form-label">Official Email Address *</label>
                  <input
                    type="email"
                    className="form-control"
                    placeholder="e.g. neha@travelhub.ops"
                    value={newAdmin.email}
                    onChange={e => setNewAdmin({ ...newAdmin, email: e.target.value })}
                    required
                  />
                </div>

                <div>
                  <label className="form-label">Command Role</label>
                  <select
                    className="form-control"
                    value={newAdmin.role}
                    onChange={e => setNewAdmin({ ...newAdmin, role: e.target.value })}
                  >
                    <option value="Operations Lead">Operations Lead</option>
                    <option value="Safety Dispatcher">Safety Dispatcher</option>
                    <option value="Finance Auditor">Finance Auditor</option>
                    <option value="Content Curator">Content Curator</option>
                  </select>
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowAddAdminModal(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                >
                  Provision Admin Key
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
