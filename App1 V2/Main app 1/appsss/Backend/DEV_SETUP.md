# Local Development Setup Guide

This guide explains how to properly connect the Android Emulator to the FastAPI backend.

## 1. Start the FastAPI Backend
When running Uvicorn, you **must** bind it to `0.0.0.0` (all network interfaces) so the Android Emulator can reach it. By default, Uvicorn binds to `127.0.0.1`, which the emulator considers external and will refuse connection.

```bash
cd "SIH APP1 Tourist User"
source venv_new/bin/activate
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

## 2. Android Emulator Configuration
The Android Emulator cannot use `localhost` or `127.0.0.1` to access your host machine's server. It must use the special loopback IP: `10.0.2.2`.

Ensure the `ApiConfig.kt` in the Android project is set to:
```kotlin
const val BASE_URL = "http://10.0.2.2:8000/api/v1/"
```

## 3. Physical Devices (Same Wi-Fi)
If you are running the app on a physical Android device connected to the same Wi-Fi network as your Mac:
1. Find your Mac's local IP address (e.g., `192.168.1.15`).
2. Update `ApiConfig.kt` to use that IP: `const val BASE_URL = "http://192.168.1.15:8000/api/v1/"`.
3. Rebuild the app and ensure the backend is running with `--host 0.0.0.0`.

## 4. Debugging Connectivity Issues
- Check the backend console for incoming requests.
- Verify the Android Logcat for network errors (ensure OkHttp logging interceptor is active in debug mode).
- Verify the FastAPI server returns a healthy response when visiting `http://localhost:8000/health` in your Mac's browser.
