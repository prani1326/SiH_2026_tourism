# 🌍 International & Domestic Tourist Platform — Enterprise Backend

> **Production-Ready Customer-Facing Tourist Mobile App Backend**  
> Built for the Smart India Hackathon (SIH) tourism initiative. Covers the complete tourist journey:  
> **Discover → Explore → Plan → Book → Pay → Travel → Navigate → Stay Safe → Get Support → Complete Trip**

---

## 🚀 Key Architectural Highlights

- **Framework**: High-performance async **FastAPI** with Python 3.14, **SQLAlchemy 2.0 ORM**, and **Pydantic v2**.
- **Interactive Documentation**:
  - Swagger UI: [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs)
  - ReDoc: [http://127.0.0.1:8000/redoc](http://127.0.0.1:8000/redoc)
- **Hardened Enterprise Security Architecture**:
  - **Token Blacklist & Revocation**: Real-time JWT revocation on `/logout` and immediate global session invalidation when Lost Phone Mode freezes an account.
  - **OWASP Security Headers**: Automatic injection of `HSTS`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `X-XSS-Protection: 1; mode=block`, `Content-Security-Policy`, and `Permissions-Policy`.
  - **Sliding-Window Rate Limiter**: IP & route-specific throttling on sensitive endpoints (e.g. 20 req/min on `/auth/login`, 10 req/min on `/auth/otp/send` and `/lost-phone/recover`) returning `429 Too Many Requests`.
  - **Unique Request ID Tracing**: Injects `X-Request-ID` and latency `X-Process-Time-Ms` on all responses.
  - **Firebase & Security**: ID token validation for Google Login/Phone Auth, and PBKDF2 HMAC SHA-256 password hashing.
- **Top 10 Indian Destinations Seed Data (Section 43)**:
  - **Agra** (Taj Mahal, Agra Fort, Mehtab Bagh, Petha markets, scam warnings, etiquette)
  - **Jaipur** (Amber Fort, Hawa Mahal, City Palace, Nahargarh, royal bazaars)
  - **Goa** (Calangute, Palolem, Dudhsagar, Old Goa churches, beach shacks)
  - **Kerala** (Alleppey backwaters, Munnar tea hills, Fort Kochi, Ayurvedic spas)
  - **Varanasi** (Dashashwamedh Ghat Ganga Aarti, Kashi Vishwanath, boat scam precautions)
  - **Rishikesh–Haridwar** (Ganga Aarti, white water rafting, yoga ashrams)
  - **Leh–Ladakh** (Pangong Tso, Nubra Valley, Khardung La, altitude acclimation)
  - **Himachal Pradesh** (Manali, Shimla, Dharamshala, Solang snow safety)
  - **Delhi** (Red Fort, Qutub Minar, Humayun's Tomb, Chandni Chowk food walk)
  - **Andaman & Nicobar** (Radhanagar Beach, Cellular Jail, Havelock scuba)

---

## 📱 Mobile App UI & Bottom Navigation Mapping

The backend is tailored for the 5-tab customer mobile app:

| Tab | API Route | Description |
|---|---|---|
| **1. Home** | `GET /api/v1/destinations?popular_only=true` | Hero search, Near Me, trending experiences, weather, safety alerts, active trip card |
| **2. Explore** | `GET /api/v1/explore/categories` | Categories (Heritage, Beaches, Wildlife, etc.) with List & Map View toggles |
| **3. Trips** | `GET /api/v1/trips` & `POST /api/v1/trips/ai-plan` | AI Trip Planner, day-wise timeline, drag-and-drop reorder, Trip OS auto re-planner |
| **4. Bookings** | `GET /api/v1/bookings` | Request-to-Book lifecycle, vouchers, QR codes, payment status, refunds |
| **5. Profile** | `GET /api/v1/users/me` | Preferences, dietary setup, emergency contacts, GDPR data export / deletion |
| **🚨 Floating SOS** | `POST /api/v1/safety/sos` | One-tap emergency dispatch, live GPS capture, nearest police & hospitals |

---

## 🛠️ Specialized Engine Services

1. **AI Trip Planner Engine** (`/api/v1/trips/ai-plan`):
   - Generates morning, afternoon, evening, and night itineraries tailored to budget, pace, family needs, and dietary rules (Vegetarian, Jain, Halal).
2. **Trip OS & Auto Re-Planning Engine** (`/api/v1/trip-os/replan`):
   - Detects or simulates disruptions (Rain, Flight Delays, Closures, Heavy Crowds) and automatically recommends constraint-preserving schedule adjustments.
3. **True Trip Cost & Budget Drift Alerts** (`/api/v1/budget/{trip_id}/true-cost`):
   - Itemized expense calculation (Flights, Hotel, Transport, Food, Visa, Insurance, eSIM, Taxes, Tips).
   - Flags budget drift (e.g., *"Trending 18% above budget"*) with cost-saving alternatives.
4. **Digital Trip Card & Offline Mode** (`/api/v1/trip-card/{trip_id}/card`):
   - Generates consolidated tourist passport, QR codes, emergency contacts, embassy info, and offline JSON cache packages.
5. **Lost Phone Mode Recovery Portal** (`/api/v1/lost-phone/recover`):
   - Allows a stranded tourist to recover all travel credentials, hotel vouchers, and emergency help from ANY browser with email + backup PIN.
6. **Lost Person / Separated Group Mode** (`/api/v1/separated-mode/checkin`):
   - Configures group geofences and designated rendezvous points (e.g. *"Gate 2"*), with scheduled check-ins and missed check-in alerts.
7. **Food & Cultural Engine** (`/api/v1/food-culture/menu-scan`):
   - Computes Diet Confidence scores and analyzes restaurant menus for Veg, Jain, Halal, and allergens.
8. **Local Transport Brain** (`/api/v1/transport-brain/compare`):
   - Compares Airport-to-Hotel transit (Cheapest vs Fastest vs Safest vs Metro vs Taxi).
9. **Preparation Engine** (`/api/v1/preparation/{destination}`):
   - Weather packing list, temple/cultural etiquette checklist, medical travel kit, international plug adapters.

---

## ⚡ Quick Start & Run Locally

### 1. Install Dependencies
```bash
pip install -r requirements.txt
```

### 2. Initialize Database & Seed Content
```bash
python -m app.db.init_db
```

### 3. Run FastAPI Development Server
```bash
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### 4. Run Automated Test Suite (31/31 Tests Passing)
```bash
pytest tests/test_api.py -v
```
All 31 integration tests pass with 100% success rate across all domains and security safeguards.

---

## 🔑 Default Demo Credentials

- **Email**: `traveler@touristapp.com`
- **Password**: `Tourist@123`
- **Emergency Recovery PIN**: `1234` (for Lost Phone Mode)
- **Active Demo Trip**: Pre-seeded active 4-day trip to Agra with Taj Mahal sunrise activity and Oberoi Amarvilas confirmed hotel booking.
