-- Central Database SQL Schema (ANSI SQL / PostgreSQL Compatible)

CREATE TABLE IF NOT EXISTS travelers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    traveler_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    profile_info TEXT,
    trips_count INTEGER DEFAULT 0,
    itinerary TEXT,
    booking_requests_count INTEGER DEFAULT 0,
    emergency_info TEXT,
    trip_status VARCHAR(50) DEFAULT 'Idle',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vendors (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    vendor_id VARCHAR(50) UNIQUE NOT NULL,
    business_name VARCHAR(150) NOT NULL,
    owner_contact VARCHAR(100) NOT NULL,
    kyc_info TEXT,
    verification_status VARCHAR(50) DEFAULT 'Pending',
    listings TEXT,
    booking_requests_count INTEGER DEFAULT 0,
    accepted_bookings_count INTEGER DEFAULT 0,
    rejected_bookings_count INTEGER DEFAULT 0,
    vendor_activity TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS leaders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    leader_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(100) NOT NULL,
    assigned_work TEXT,
    approvals_count INTEGER DEFAULT 0,
    tickets_handled_count INTEGER DEFAULT 0,
    incidents_created_count INTEGER DEFAULT 0,
    activity_history TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS destinations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    destination_name VARCHAR(100) UNIQUE NOT NULL,
    places_pois TEXT,
    hotels TEXT,
    activities TEXT,
    restaurants TEXT,
    emergency_help_info TEXT,
    content_status VARCHAR(50) DEFAULT 'Published',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS trips (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    trip_id VARCHAR(50) UNIQUE NOT NULL,
    traveler_name VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    start_date VARCHAR(20) NOT NULL,
    end_date VARCHAR(20) NOT NULL,
    number_of_travelers INTEGER DEFAULT 1,
    budget DECIMAL(10,2) DEFAULT 0.00,
    itinerary TEXT,
    status VARCHAR(50) DEFAULT 'Planned',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bookings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    booking_id VARCHAR(50) UNIQUE NOT NULL,
    traveler_name VARCHAR(100) NOT NULL,
    vendor_name VARCHAR(100) NOT NULL,
    trip_name VARCHAR(100) NOT NULL,
    service VARCHAR(100) NOT NULL,
    booking_date VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) DEFAULT 0.00,
    status VARCHAR(50) DEFAULT 'Pending', -- Pending, Accepted, Rejected, Cancelled, Completed
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tickets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticket_id VARCHAR(50) UNIQUE NOT NULL,
    reporter VARCHAR(100) NOT NULL,
    issue TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    priority VARCHAR(50) DEFAULT 'Medium', -- Low, Medium, High, Critical
    assigned_leader VARCHAR(100),
    status VARCHAR(50) DEFAULT 'Open', -- Open, In Progress, Resolved, Closed
    resolution TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS safety_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id VARCHAR(50) UNIQUE NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- SOS Event, Lost-phone Event, Emergency Request
    user_name VARCHAR(100) NOT NULL,
    contact_number VARCHAR(20) NOT NULL,
    location VARCHAR(150) NOT NULL,
    active_incidents TEXT,
    resolution_status VARCHAR(50) DEFAULT 'Active', -- Active, Dispatched, Resolved
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app_activities (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    app_source VARCHAR(50) NOT NULL, -- Traveler App, Vendor App, Leader/Ops App
    action VARCHAR(100) NOT NULL,
    details TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

