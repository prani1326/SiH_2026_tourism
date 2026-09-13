import React, { useState, useEffect } from 'react';
import Sidebar from './components/Sidebar';
import { SECTIONS } from './constants/navigation';
import Header from './components/Header';
import DetailModal from './components/DetailModal';
import AppSimulatorModal from './components/AppSimulatorModal';

import OverviewPage from './pages/OverviewPage';
import TravelerPage from './pages/TravelerPage';
import VendorPage from './pages/VendorPage';
import LeaderPage from './pages/LeaderPage';
import ContentPage from './pages/ContentPage';
import BookingPage from './pages/BookingPage';
import TripPage from './pages/TripPage';
import TripApprovalPage from './pages/TripApprovalPage';
import TicketPage from './pages/TicketPage';
import SafetyPage from './pages/SafetyPage';
import AnalyticsPage from './pages/AnalyticsPage';
import ReportsPage from './pages/ReportsPage';
import SettingsPage from './pages/SettingsPage';

import {
  fetchOverview,
  fetchTravelers,
  fetchVendors,
  fetchLeaders,
  fetchDestinations,
  fetchBookings,
  fetchTrips,
  fetchTripApprovals,
  fetchTickets,
  fetchSafetyEvents,
  fetchAnalytics,
  fetchNotifications
} from './api/client';

export default function App() {
  // Read initial section from URL hash or default to 'overview'
  const getInitialSection = () => {
    try {
      const hash = window.location.hash.replace('#', '').trim().toLowerCase();
      if (hash && SECTIONS.some(s => s.id === hash)) {
        return hash;
      }
    } catch (e) {}
    return 'overview';
  };

  const [activeSection, setActiveSection] = useState(getInitialSection);
  const [selectedDetail, setSelectedDetail] = useState(null);
  const [showSimulator, setShowSimulator] = useState(false);

  // Layout & Theme states
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem('travelhub_theme') || 'light';
  });
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);

  // Sync active section with URL hash
  const handleSelectSection = (sectionId) => {
    setActiveSection(sectionId);
    try {
      window.location.hash = sectionId;
    } catch (e) {}
    setMobileSidebarOpen(false);
  };

  // Listen for hash changes
  useEffect(() => {
    const onHashChange = () => {
      const hash = window.location.hash.replace('#', '').trim().toLowerCase();
      if (hash && SECTIONS.some(s => s.id === hash)) {
        setActiveSection(hash);
      }
    };
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);

  // Sync theme with data-theme attribute on document root
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('travelhub_theme', theme);
  }, [theme]);

  const handleToggleTheme = () => {
    setTheme(prev => (prev === 'light' ? 'dark' : 'light'));
  };

  // Data states
  const [overviewData, setOverviewData] = useState(null);
  const [travelers, setTravelers] = useState([]);
  const [vendors, setVendors] = useState([]);
  const [leaders, setLeaders] = useState([]);
  const [destinations, setDestinations] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [trips, setTrips] = useState([]);
  const [tripApprovals, setTripApprovals] = useState([]);
  const [tickets, setTickets] = useState([]);
  const [safetyEvents, setSafetyEvents] = useState([]);
  const [analyticsData, setAnalyticsData] = useState(null);
  const [notifications, setNotifications] = useState([]);

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const loadAllData = async (isBackground = false) => {
    try {
      if (!isBackground) setLoading(true);
      else setRefreshing(true);

      // Using Promise.allSettled so NO SINGLE failure blocks other pages
      const results = await Promise.allSettled([
        fetchOverview(),
        fetchTravelers(),
        fetchVendors(),
        fetchLeaders(),
        fetchDestinations(),
        fetchBookings(),
        fetchTrips(),
        fetchTripApprovals(),
        fetchTickets(),
        fetchSafetyEvents(),
        fetchAnalytics(),
        fetchNotifications()
      ]);

      const getValue = (idx, fallback) => 
        results[idx]?.status === 'fulfilled' ? results[idx].value : fallback;

      setOverviewData(getValue(0, null));
      setTravelers(getValue(1, []));
      setVendors(getValue(2, []));
      setLeaders(getValue(3, []));
      setDestinations(getValue(4, []));
      setBookings(getValue(5, []));
      setTrips(getValue(6, []));
      setTripApprovals(getValue(7, []));
      setTickets(getValue(8, []));
      setSafetyEvents(getValue(9, []));
      setAnalyticsData(getValue(10, null));
      setNotifications(getValue(11, []));
    } catch (err) {
      console.error('Data sync error:', err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadAllData();
  }, []);

  const activeObj = SECTIONS.find((s) => s.id === activeSection);
  const title = activeObj ? activeObj.label : 'Central Console';

  // Badge counts for sidebar navigation
  const pendingApprovalsCount = Array.isArray(tripApprovals)
    ? tripApprovals.filter(t => (t?.approval_status || t?.status) === 'Pending Approval').length
    : 2;
  const activeTicketsCount = Array.isArray(tickets)
    ? tickets.filter(t => (t?.status || 'Open').toLowerCase() === 'open').length
    : 1;
  const activeSafetyCount = Array.isArray(safetyEvents)
    ? safetyEvents.filter(e => (e?.resolution_status || 'Active').toLowerCase() === 'active').length
    : 1;

  const badgeCounts = {
    approvals: pendingApprovalsCount,
    tickets: activeTicketsCount,
    safety: activeSafetyCount
  };

  const unreadNotifsCount = Array.isArray(notifications)
    ? notifications.filter(n => n?.status === 'unread').length
    : 0;

  return (
    <div className={`dashboard-container ${sidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
      {/* 13-Section Enterprise Navigation Sidebar */}
      <Sidebar 
        activeSection={activeSection} 
        setActiveSection={handleSelectSection} 
        collapsed={sidebarCollapsed}
        setCollapsed={setSidebarCollapsed}
        mobileOpen={mobileSidebarOpen}
        setMobileOpen={setMobileSidebarOpen}
        badgeCounts={badgeCounts}
      />

      {/* Main Content Layout */}
      <div className="main-layout">
        {/* Global Top Header */}
        <Header
          title={title}
          onRefresh={() => loadAllData(true)}
          refreshing={refreshing}
          onSimulate={() => setShowSimulator(true)}
          theme={theme}
          onToggleTheme={handleToggleTheme}
          onToggleSidebar={() => setSidebarCollapsed(!sidebarCollapsed)}
          onOpenMobileSidebar={() => setMobileSidebarOpen(true)}
          notifications={notifications}
          unreadCount={unreadNotifsCount}
        />

        {/* Dynamic Page Router Body — Instant Non-Blocking Navigation */}
        <main className="content-body">
          {activeSection === 'overview' && (
            <OverviewPage 
              overviewData={overviewData} 
              onViewDetail={setSelectedDetail} 
              onNavigate={handleSelectSection}
            />
          )}

          {activeSection === 'traveler' && (
            <TravelerPage 
              travelers={travelers} 
              onViewDetail={setSelectedDetail} 
            />
          )}

          {activeSection === 'vendor' && (
            <VendorPage 
              vendors={vendors} 
              onViewDetail={setSelectedDetail} 
            />
          )}

          {activeSection === 'leader' && (
            <LeaderPage 
              leaders={leaders} 
              onViewDetail={setSelectedDetail} 
            />
          )}

          {activeSection === 'content' && (
            <ContentPage 
              destinations={destinations} 
              onViewDetail={setSelectedDetail} 
            />
          )}

          {activeSection === 'booking' && (
            <BookingPage 
              bookings={bookings} 
              onViewDetail={setSelectedDetail} 
            />
          )}

          {activeSection === 'trip' && (
            <TripPage 
              trips={trips} 
              onViewDetail={setSelectedDetail} 
            />
          )}

          {activeSection === 'approvals' && (
            <TripApprovalPage 
              trips={tripApprovals} 
              leaders={leaders}
              onRefresh={() => loadAllData(true)} 
              onViewDetail={setSelectedDetail} 
            />
          )}

          {activeSection === 'ticket' && (
            <TicketPage 
              tickets={tickets} 
              onViewDetail={setSelectedDetail} 
            />
          )}

          {activeSection === 'safety' && (
            <SafetyPage 
              safetyEvents={safetyEvents} 
              onViewDetail={setSelectedDetail} 
            />
          )}

          {activeSection === 'analytics' && (
            <AnalyticsPage 
              analyticsData={analyticsData} 
            />
          )}

          {activeSection === 'reports' && (
            <ReportsPage />
          )}

          {activeSection === 'settings' && (
            <SettingsPage 
              theme={theme}
              onToggleTheme={handleToggleTheme}
            />
          )}
        </main>
      </div>

      {/* Global Slide-over Detail Modal */}
      {selectedDetail && (
        <DetailModal
          title={`Detail Inspector — ${activeObj ? activeObj.label : 'Record'}`}
          data={selectedDetail}
          onClose={() => setSelectedDetail(null)}
        />
      )}

      {/* Multi-App Flow Simulator Modal */}
      {showSimulator && (
        <AppSimulatorModal
          onClose={() => setShowSimulator(false)}
          onSuccess={() => loadAllData(true)}
        />
      )}
    </div>
  );
}
