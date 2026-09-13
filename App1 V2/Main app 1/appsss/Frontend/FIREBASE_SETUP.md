# 🚀 Tourist App — Firebase Cloud Backend Setup Guide

This application operates **100% autonomously** using Google Firebase cloud services. There is **no local Python/FastAPI server**, **no SQLite/Room database**, and **no local IP address** required.

---

## 🏗️ Architecture

```
Android App (Jetpack Compose UI)
       │
       ├──► Firebase Authentication   (Email/Password, Google Sign-In, Session Lifecycle)
       │
       ├──► Cloud Firestore           (Single source of truth: Users, Destinations, Trips, Bookings, SOS Alerts)
       │
       └──► Firebase Storage          (Avatars, Destination media, Trip attachments)
```

---

## 📋 Prerequisites & Configuration

### 1. Firebase Project
- **Project ID**: `trip-planner-version-1`
- **Registered Package**: `com.touristapp`
- **Config File**: [`app/google-services.json`](file:///c:/Users/97018/OneDrive/Desktop/version%202/App1%20V2/Main%20app%201/appsss/frontend%20torist/app/google-services.json) (Already configured in project!)

### 2. Enabled Services in Firebase Console
Ensure the following services are enabled in [Firebase Console](https://console.firebase.google.com/project/trip-planner-version-1):
1. **Authentication**: Enable **Email/Password** provider under Sign-in method.
2. **Cloud Firestore**: Database created in `(default)` location.
3. **Firebase Storage**: Default bucket enabled (`trip-planner-version-1.firebasestorage.app`).

### 3. Deploy Security Rules
To deploy the security rules via Firebase CLI:
```bash
firebase deploy --only firestore:rules,storage
```
The rules files are located at:
- [`firestore.rules`](file:///c:/Users/97018/OneDrive/Desktop/version%202/App1%20V2/Main%20app%201/appsss/frontend%20torist/firestore.rules)
- [`storage.rules`](file:///c:/Users/97018/OneDrive/Desktop/version%202/App1%20V2/Main%20app%201/appsss/frontend%20torist/storage.rules)
- [`firestore.indexes.json`](file:///c:/Users/97018/OneDrive/Desktop/version%202/App1%20V2/Main%20app%201/appsss/frontend%20torist/firestore.indexes.json)

---

## 🛠️ Building & Running on Any Laptop

1. Open the project root folder `frontend torist` in Android Studio or VS Code.
2. Ensure Android SDK is pointed to your local machine in `local.properties`:
   ```properties
   sdk.dir=C\:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk   # On Windows
   # or
   sdk.dir=/Users/<YourUsername>/Library/Android/sdk                # On macOS
   ```
3. Build the debug APK via command line:
   ```bash
   ./gradlew assembleDebug
   ```
4. Run on your Android Emulator or physical device connected via USB/Wi-Fi.

---

## 📦 Firestore Collections Schema

| Collection | Path | Description | Access Rules |
|---|---|---|---|
| **Users** | `/users/{uid}` | User profiles, contact, preferences | Owner read/write; public read of basic info |
| **Destinations** | `/destinations/{id}` | Verified tourist destinations (Agra, Jaipur, Goa, etc.) | Public read; Admin write |
| **Trips** | `/trips/{tripId}` | User custom & AI-generated itineraries | Owner read/write only |
| **Bookings** | `/bookings/{bookingId}` | User bookings & reservation vouchers | Owner read/write only |
| **SOS Alerts** | `/sos_alerts/{alertId}` | Emergency dispatches with GPS coordinates | Authenticated create; Owner/Ops read |

---

## 🧪 Automatic Data Seeding
When running the app on a fresh/empty Firestore database, the `DestinationRepository` automatically bootstraps the top Indian tourist destinations (Agra, Jaipur, Goa, Kerala, Varanasi, etc.) so your screens immediately have beautiful, interactive content.
