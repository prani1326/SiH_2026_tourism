@echo off
setlocal enabledelayedexpansion
title Tourist App - Connect Both (Backend + Android)
echo ================================================================
echo    CONNECTING BOTH: FASTAPI BACKEND ^& ANDROID FRONTEND
echo ================================================================
echo.

set "ADB_PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
set "BACKEND_DIR=%~dp0Main app 1\appsss\Backend"
if not exist "%BACKEND_DIR%" (
    set "BACKEND_DIR=%~dp0Main app 1\appsss\SIH APP1 Tourist User"
)
set "PYTHON_EXE=%BACKEND_DIR%\venv_win\Scripts\python.exe"

if not exist "%PYTHON_EXE%" (
    set "PYTHON_EXE=%BACKEND_DIR%\.venv\Scripts\python.exe"
)
if not exist "%PYTHON_EXE%" (
    set "PYTHON_EXE=python"
)

echo [1/3] Checking ADB connection and configuring port forwarding...
if exist "%ADB_PATH%" (
    "%ADB_PATH%" start-server >nul 2>&1
    set "DEVICE_FOUND=0"
    for /f "tokens=1" %%d in ('"%ADB_PATH%" devices ^| findstr /v "List" ^| findstr /r "device$"') do (
        "%ADB_PATH%" -s %%d reverse tcp:8000 tcp:8000 >nul 2>&1
        echo       [OK] ADB reverse tcp:8000 configured on device: %%d
        set "DEVICE_FOUND=1"
    )
    if "!DEVICE_FOUND!"=="0" (
        echo       [INFO] No ADB device actively attached. Standard routes will be used.
    )
) else (
    echo       [INFO] ADB not detected at %ADB_PATH%. Standard emulator route http://10.0.2.2:8000 will be used.
)

echo.
echo [2/3] Detecting network configuration...
set "WIFI_IP="
for /f "usebackq tokens=*" %%i in (`powershell -NoProfile -Command "(Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias '*Wi-Fi*' -ErrorAction SilentlyContinue | Select-Object -First 1).IPAddress"`) do set "WIFI_IP=%%i"
if "%WIFI_IP%"=="" (
    for /f "usebackq tokens=*" %%i in (`powershell -NoProfile -Command "(Get-NetIPAddress -AddressFamily IPv4 | Where-Object IPAddress -notmatch '^(127\.|169\.254\.)' | Select-Object -First 1).IPAddress"`) do set "WIFI_IP=%%i"
)
if "%WIFI_IP%"=="" set "WIFI_IP=172.20.10.8"

echo       - Android Emulator:  http://10.0.2.2:8000 OR http://127.0.0.1:8000
echo       - Wi-Fi Device:      http://%WIFI_IP%:8000
echo       - PC Browser:        http://127.0.0.1:8000/docs
echo.

echo [3/3] Checking port 8000 status...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8000 ^| findstr LISTENING 2^>nul') do (
    echo       [INFO] Port 8000 occupied by existing process (PID %%a).
    echo       Stopping stale process to ensure clean startup...
    taskkill /F /PID %%a >nul 2>&1
)

echo Starting FastAPI Backend on 0.0.0.0:8000 with Live Reload...
cd /d "%BACKEND_DIR%"
"%PYTHON_EXE%" -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload

pause
