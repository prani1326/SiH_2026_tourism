@echo off
echo ===================================================
echo Opening Tourist Traveler Android App in Android Studio...
echo Project Path: %~dp0Main app 1\appsss\Frontend
echo ===================================================

if exist "C:\Program Files\Android\Android Studio\bin\studio64.exe" (
    start "" "C:\Program Files\Android\Android Studio\bin\studio64.exe" "%~dp0Main app 1\appsss\Frontend"
    echo Project launched in Android Studio!
) else (
    echo Android Studio executable not found at default location.
    echo Please open Android Studio manually, click "File" -^> "Open", and choose:
    echo %~dp0Main app 1\appsss\Frontend
)
pause
