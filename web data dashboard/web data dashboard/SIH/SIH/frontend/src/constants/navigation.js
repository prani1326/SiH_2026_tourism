import { 
  LayoutDashboard, 
  Users, 
  Store, 
  UserCheck, 
  MapPin, 
  BookOpenCheck, 
  Compass, 
  CheckCircle2, 
  LifeBuoy, 
  ShieldAlert, 
  BarChart3, 
  FileText, 
  Settings 
} from 'lucide-react';

export const SECTIONS = [
  // MAIN
  { id: 'overview', label: 'Overview', category: 'MAIN', icon: LayoutDashboard },
  { id: 'traveler', label: 'Traveler Data', category: 'MAIN', icon: Users },
  { id: 'vendor', label: 'Vendor Data', category: 'MAIN', icon: Store },
  { id: 'leader', label: 'Leader / Ops', category: 'MAIN', icon: UserCheck },
  { id: 'content', label: 'Content Data', category: 'MAIN', icon: MapPin },
  
  // OPERATIONS
  { id: 'booking', label: 'Booking Data', category: 'OPERATIONS', icon: BookOpenCheck },
  { id: 'trip', label: 'Trip Data', category: 'OPERATIONS', icon: Compass },
  { id: 'approvals', label: 'Trip Approvals', category: 'OPERATIONS', icon: CheckCircle2, badgeKey: 'approvals' },
  { id: 'ticket', label: 'Tickets / Incidents', category: 'OPERATIONS', icon: LifeBuoy, badgeKey: 'tickets' },
  { id: 'safety', label: 'Safety / Emergency', category: 'OPERATIONS', icon: ShieldAlert, badgeKey: 'safety' },
  
  // INTELLIGENCE & SYSTEM
  { id: 'analytics', label: 'Analytics', category: 'INTELLIGENCE & SYSTEM', icon: BarChart3 },
  { id: 'reports', label: 'Reports', category: 'INTELLIGENCE & SYSTEM', icon: FileText },
  { id: 'settings', label: 'Settings', category: 'INTELLIGENCE & SYSTEM', icon: Settings }
];
