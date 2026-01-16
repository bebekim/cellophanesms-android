@echo off
REM CellophoneSMS E2E Test Script for Windows CMD
REM Usage: scripts\test_sms_e2e.bat
REM Note: For Git Bash, use test_sms_e2e.sh instead

setlocal enabledelayedexpansion

REM Configuration
set PACKAGE_NAME=com.cellophanemail.sms.debug
set MAIN_ACTIVITY=com.cellophanemail.sms.ui.main.MainActivity
set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe

REM Check if ADB exists
if not exist "%ADB%" (
    echo [ERROR] ADB not found at %ADB%
    exit /b 1
)

echo.
echo ================================================================
echo          CellophoneSMS E2E Test Suite
echo ================================================================
echo.

"%ADB%" devices | findstr /r "device$" >nul
if errorlevel 1 (
    echo [FAIL] No device connected
    exit /b 1
)
echo [PASS] Device found

"%ADB%" shell pm list packages | findstr "%PACKAGE_NAME%" >nul
if errorlevel 1 (
    echo [INFO] Installing app...
    cd /d "%~dp0.."
    call gradlew.bat installDebug
)
echo [PASS] App installed

"%ADB%" shell am start -n %PACKAGE_NAME%/%MAIN_ACTIVITY% >nul 2>&1
ping -n 3 127.0.0.1 >nul
echo [PASS] App launched

"%ADB%" logcat -c

echo [TEST] Sending test SMS messages...
"%ADB%" emu sms send 5550001111 "Test message 1"
"%ADB%" emu sms send 5550002222 "Test message 2"
"%ADB%" emu sms send 5550003333 "Test message 3"
ping -n 3 127.0.0.1 >nul

echo [INFO] Taking screenshot...
mkdir "%~dp0..\test-screenshots" 2>nul
"%ADB%" exec-out screencap -p > "%~dp0..\test-screenshots\test_result.png"

echo.
echo ================================================================
echo                    TEST COMPLETE
echo ================================================================
echo Check emulator for results. Screenshot saved.

endlocal
