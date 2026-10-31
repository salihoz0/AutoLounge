@echo off
chcp 65001 >nul
title AutoLounge — Android Auto One-Click Installer
color 0F

echo =====================================================================
echo    🏎️  AutoLounge — Android Auto One-Click Installer (Windows)
echo =====================================================================
echo.
echo This tool installs AutoLounge stamped with the Google Play Store installer ID
echo ("com.android.vending") WITHOUT ROOTING your smartphone.
echo This unlocks full-screen YouTube, Live TV and web apps on your car display.
echo.

:: 1. ADB CHECK
set "ADB_BIN=adb"
where adb >nul 2>&1
if %errorlevel% equ 0 (
    echo [✓] ADB (Android Debug Bridge) detected on your system.
    goto :find_apk
)

:: Check if already downloaded locally
if exist "%~dp0tools\platform-tools\adb.exe" (
    set "ADB_BIN=%~dp0tools\platform-tools\adb.exe"
    echo [✓] ADB found in local tools folder.
    goto :find_apk
)

:: ADB not found, prompt to download
echo [!] 'adb' command was not found on your system.
echo     Google Platform Tools (~18 MB) is required to proceed.
echo.
set /p "DL_CHOICE=Automatically download Google Platform Tools? [Y/n] (Default: Y): "
if /i "%DL_CHOICE%"=="N" (
    echo.
    echo [X] Cannot proceed without ADB. Installation aborted.
    pause
    exit /b 1
)

echo.
echo [*] Downloading Platform Tools from official Google servers...
if not exist "%~dp0tools" mkdir "%~dp0tools"
set "ZIP_PATH=%TEMP%\platform-tools-windows.zip"

powershell -NoProfile -Command ^
    "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; " ^
    "Invoke-WebRequest -Uri 'https://dl.google.com/android/repository/platform-tools-latest-windows.zip' -OutFile '%ZIP_PATH%'"

if not exist "%ZIP_PATH%" (
    echo [X] ERROR: Download failed. Please check your internet connection.
    pause
    exit /b 1
)

echo [*] Extracting files...
powershell -NoProfile -Command ^
    "Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%~dp0tools' -Force"

del /f /q "%ZIP_PATH%" >nul 2>&1

if exist "%~dp0tools\platform-tools\adb.exe" (
    set "ADB_BIN=%~dp0tools\platform-tools\adb.exe"
    echo [✓] ADB successfully installed.
) else (
    echo [X] ERROR: Could not extract ADB files.
    pause
    exit /b 1
)

:: 2. LOCATE OR DOWNLOAD APK
:find_apk
echo.
echo [*] Searching for AutoLounge APK file...
set "TARGET_APK="

for %%f in ("%~dp0AutoLounge*.apk") do (
    set "TARGET_APK=%%f"
    goto :apk_found
)
for %%f in ("%~dp0app\build\outputs\apk\debug\*.apk") do (
    set "TARGET_APK=%%f"
    goto :apk_found
)
for %%f in ("%~dp0*.apk") do (
    set "TARGET_APK=%%f"
    goto :apk_found
)

:apk_found
if defined TARGET_APK (
    echo [✓] APK file found: %TARGET_APK%
    goto :check_device
)

echo [!] No AutoLounge APK found in current directory.
set /p "DL_APK=Download latest release APK from GitHub? [Y/n] (Default: Y): "
if /i "%DL_APK%"=="N" (
    echo [X] Please place AutoLounge.apk in this directory and run again.
    pause
    exit /b 1
)

echo [*] Downloading latest APK from GitHub Releases...
set "TARGET_APK=%~dp0AutoLounge.apk"
powershell -NoProfile -Command ^
    "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; " ^
    "try { " ^
    "  $rel = Invoke-RestMethod -Uri 'https://api.github.com/repos/salihoz0/AutoLounge/releases/latest'; " ^
    "  $asset = $rel.assets | Where-Object { $_.name -like '*.apk' } | Select-Object -First 1; " ^
    "  if ($asset) { Invoke-WebRequest -Uri $asset.browser_download_url -OutFile '%TARGET_APK%' } " ^
    "} catch { exit 1 }"

if not exist "%TARGET_APK%" (
    echo [!] Could not fetch from GitHub API. Please place AutoLounge.apk in this directory.
    pause
    exit /b 1
)
echo [✓] APK successfully downloaded.

:: 3. DEVICE CONNECTION CHECK
:check_device
echo.
echo =====================================================================
echo  📲 Checking Device Connection
echo =====================================================================
echo  Please verify the following:
echo   1. Your phone is connected to your computer via USB.
echo   2. 'USB Debugging' is REQUIRED and enabled in Developer Options.
echo   3. If 'Allow USB debugging?' pops up on your phone, tap ALLOW.
echo   4. (Xiaomi/Redmi/POCO only) In Developer Options, enable 'Install via USB'.
echo =====================================================================
echo.

:wait_device
"%ADB_BIN%" start-server >nul 2>&1

:: Query device status
set "DEVICE_STATE="
for /f "tokens=1,2" %%A in ('"%ADB_BIN%" devices ^| findstr /v "List of devices emulator-"') do (
    if not "%%A"=="" (
        if "%%B"=="device" (
            set "DEVICE_STATE=device"
            echo [✓] Phone connected and authorized: %%A
            set "ANDROID_SERIAL=%%A"
            goto :install_app
        )
        if "%%B"=="unauthorized" (
            set "DEVICE_STATE=unauthorized"
            echo [!] Phone detected, but UNAUTHORIZED.
            echo     Please unlock your phone screen and tap 'Always allow from this computer'.
        )
    )
)

if "%DEVICE_STATE%"=="" (
    echo [!] No connected phone detected.
)

echo.
echo Press any key to retry (or CTRL+C to quit)...
pause >nul
goto :wait_device

:: 4. INSTALLATION STAGE
:install_app
echo.
echo =====================================================================
echo  🚀 Installing AutoLounge with Google Play Store signature...
echo =====================================================================
echo Method: system installer with Google Play Store as installer
echo.

set "REMOTE=/storage/emulated/0/Download/AutoLounge.apk"
set "BEFORE="
for /f "delims=" %%L in ('"%ADB_BIN%" shell dumpsys package com.autolounge.park ^| findstr /c:lastUpdateTime') do set "BEFORE=%%L"
"%ADB_BIN%" push "%TARGET_APK%" %REMOTE% >nul
set "MID="
:: The system installer only takes content:// URIs; MediaStore may need a moment to index the pushed file.
for /l %%i in (1,1,5) do if not defined MID call :find_media_id
if not defined MID (
    cmd /c exit 1
    goto :install_done
)
:: Same intent extras as KingInstaller: the system installer records Google Play as installer.
"%ADB_BIN%" shell am start -a android.intent.action.INSTALL_PACKAGE -d content://media/external/file/%MID% -t application/vnd.android.package-archive -f 0x00000001 --es android.intent.extra.INSTALLER_PACKAGE_NAME com.android.vending --es android.intent.extra.REFERRER_NAME android-app://com.android.vending --ei android.intent.extra.INSTALL_REASON 1 --ez android.intent.extra.NOT_UNKNOWN_SOURCE true >nul
if errorlevel 1 goto :install_done
echo  ^>^> Tap Install / Update on your phone screen. Waiting up to 3 minutes...
cmd /c exit 1
for /l %%i in (1,1,90) do if errorlevel 1 call :poll_install
if not errorlevel 1 ("%ADB_BIN%" shell rm -f %REMOTE% >nul 2>&1 & cmd /c exit 0)
goto :install_done

:find_media_id
"%ADB_BIN%" shell "content query --uri content://media/external/file --projection _id --where \"_data='%REMOTE%'\"" > "%TEMP%\al_mid.txt" 2>nul
for /f "tokens=2 delims==" %%M in ('type "%TEMP%\al_mid.txt"') do set "MID=%%M"
if not defined MID timeout /t 1 /nobreak >nul
exit /b 0

:poll_install
timeout /t 2 /nobreak >nul
set "NOW="
for /f "delims=" %%L in ('"%ADB_BIN%" shell dumpsys package com.autolounge.park ^| findstr /c:lastUpdateTime') do set "NOW=%%L"
if defined NOW if not "%NOW%"=="%BEFORE%" exit /b 0
exit /b 1

:install_done
if %errorlevel% equ 0 (
    echo.
    echo =====================================================================
    echo  🎉 SUCCESS! INSTALLATION COMPLETE!
    echo =====================================================================
    echo.
    :: Samsung Dual Messenger auto-clones Play-signed installs into a DUAL_APP user (second icon with clone badge)
    for /f "tokens=2 delims={:" %%U in ('"%ADB_BIN%" shell pm list users ^| findstr DUAL_APP') do "%ADB_BIN%" shell pm uninstall --user %%U com.autolounge.park >nul 2>&1
    echo  ✓ AutoLounge is now installed with Google Play Store verification.
    echo  ✓ Plug your phone into your vehicle to launch AutoLounge on your car screen.
    echo.
    echo =====================================================================
    echo  ⚙️  Opening Android Auto Settings on your phone...
    echo =====================================================================
    "%ADB_BIN%" shell am start -n com.google.android.projection.gearhead/.companion.settings.DefaultSettingsActivity >nul 2>&1
    echo  Please check your phone screen and perform these 3 quick steps:
    echo   1. Scroll to the very bottom and tap 'Version' 10 times.
    echo   2. Tap the 3 dots in the top-right corner -^> Developer settings.
    echo   3. Check the box for 'Unknown sources'.
    echo.
    echo  Need help? Ask popular AI assistants for your phone model:
    echo   - ChatGPT: https://chatgpt.com/?q=Android+Auto+Developer+Settings+Unknown+sources+how+to+enable+on+model:+
    echo   - Claude:  https://claude.ai/new?q=Android+Auto+Developer+Settings+Unknown+sources+how+to+enable+on+model:+
    echo   - Gemini:  https://gemini.google.com/app?q=Android+Auto+Developer+Settings+Unknown+sources+how+to+enable+on+model:+
    echo.
    echo All set! AutoLounge is ready for your vehicle.
    echo.
) else (
    echo.
    echo =====================================================================
    echo  [X] INSTALLATION FAILED!
    echo =====================================================================
    echo.
    echo  Please check common causes:
    echo.
    echo  1. XIAOMI / REDMI / POCO DEVICES (IMPORTANT):
    echo     In Developer Options, you MUST enable:
    echo     -^> 'Install via USB'
    echo     -^> 'USB Debugging (Security settings)'
    echo     (Otherwise, MIUI/HyperOS blocks ADB installations).
    echo.
    echo  2. PHONE SCREEN LOCKED OR PROMPT PENDING:
    echo     Unlock your phone and accept any installation prompts.
    echo.
    echo  3. EXISTING VERSION CONFLICT:
    echo     If an older version is installed, uninstall it from your phone and retry.
    echo =====================================================================
)

echo Press any key to exit...
pause >nul
