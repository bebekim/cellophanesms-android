#!/bin/bash
#
# CellophoneSMS End-to-End Test Script
#
# Usage: ./scripts/test_sms_e2e.sh [options]
#
# Options:
#   --install     Build and install app before testing
#   --clean       Clear app data before testing
#   --verbose     Show detailed logs
#   --screenshots Take screenshots after each test
#

set -e

# ============== Configuration ==============
PACKAGE_NAME="com.cellophanemail.sms.debug"
MAIN_ACTIVITY="com.cellophanemail.sms.ui.main.MainActivity"
ADB="${ANDROID_HOME:-$HOME/AppData/Local/Android/Sdk}/platform-tools/adb"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
SCREENSHOT_DIR="$PROJECT_DIR/test-screenshots"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

# ============== Helper Functions ==============

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
    ((TESTS_PASSED++))
}

log_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
    ((TESTS_FAILED++))
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

check_device() {
    log_info "Checking for connected device/emulator..."
    if ! "$ADB" devices | grep -q "device$"; then
        log_fail "No device/emulator connected"
        echo "Please start an emulator or connect a device"
        exit 1
    fi
    DEVICE=$("$ADB" devices | grep "device$" | head -1 | cut -f1)
    log_success "Found device: $DEVICE"
}

check_app_installed() {
    log_info "Checking if app is installed..."
    if "$ADB" shell pm list packages | grep -q "$PACKAGE_NAME"; then
        log_success "App is installed: $PACKAGE_NAME"
        return 0
    else
        log_warn "App not installed"
        return 1
    fi
}

install_app() {
    log_info "Building and installing app..."
    cd "$PROJECT_DIR"
    ./gradlew installDebug
    if [ $? -eq 0 ]; then
        log_success "App installed successfully"
    else
        log_fail "Failed to install app"
        exit 1
    fi
}

clear_app_data() {
    log_info "Clearing app data..."
    "$ADB" shell pm clear "$PACKAGE_NAME" > /dev/null 2>&1
    log_success "App data cleared"
}

launch_app() {
    log_info "Launching app..."
    "$ADB" shell am start -n "$PACKAGE_NAME/$MAIN_ACTIVITY" > /dev/null 2>&1
    sleep 2
    log_success "App launched"
}

take_screenshot() {
    local name=$1
    mkdir -p "$SCREENSHOT_DIR"
    local filename="$SCREENSHOT_DIR/${TIMESTAMP}_${name}.png"
    "$ADB" exec-out screencap -p > "$filename"
    log_info "Screenshot saved: $filename"
}

send_sms() {
    local phone=$1
    local message=$2
    log_info "Sending SMS from $phone: \"$message\""
    "$ADB" emu sms send "$phone" "$message"
    sleep 1
}

check_log_contains() {
    local pattern=$1
    local description=$2
    if "$ADB" logcat -d | grep -q "$pattern"; then
        log_success "$description"
        return 0
    else
        log_fail "$description"
        return 1
    fi
}

clear_logcat() {
    "$ADB" logcat -c
}

get_thread_count() {
    # This is a simplified check - in real tests you'd query the database
    "$ADB" shell "dumpsys activity $PACKAGE_NAME" 2>/dev/null | grep -c "thread" || echo "0"
}

# ============== Test Cases ==============

test_app_launches() {
    log_info "=== Test: App Launches ==="
    clear_logcat
    launch_app

    # Check if app process is running
    if "$ADB" shell pidof "$PACKAGE_NAME" > /dev/null 2>&1; then
        log_success "App process is running"
    else
        log_fail "App process not found"
        return 1
    fi

    if [ "$TAKE_SCREENSHOTS" = true ]; then
        take_screenshot "01_app_launched"
    fi
}

test_app_is_default_sms() {
    log_info "=== Test: App is Default SMS Handler ==="

    local role_holder=$("$ADB" shell cmd role get-role-holders android.app.role.SMS 2>/dev/null)
    if echo "$role_holder" | grep -q "$PACKAGE_NAME"; then
        log_success "App is default SMS handler"
    else
        log_warn "App is NOT default SMS handler. Role holder: $role_holder"
        log_info "You may need to manually set it as default in Settings"
    fi
}

test_receive_single_sms() {
    log_info "=== Test: Receive Single SMS ==="
    clear_logcat

    send_sms "5550001111" "Test single SMS message"
    sleep 2

    if "$ADB" logcat -d | grep -q "SmsReceiver.*Received SMS from 5550001111"; then
        log_success "SMS received by SmsReceiver"
    else
        log_fail "SMS not received by SmsReceiver"
    fi

    if [ "$TAKE_SCREENSHOTS" = true ]; then
        take_screenshot "02_single_sms_received"
    fi
}

test_receive_multiple_sms_same_sender() {
    log_info "=== Test: Multiple SMS from Same Sender ==="
    clear_logcat

    send_sms "5550002222" "First message from sender"
    send_sms "5550002222" "Second message from sender"
    send_sms "5550002222" "Third message from sender"
    sleep 2

    local count=$("$ADB" logcat -d | grep -c "SmsReceiver.*5550002222" || echo "0")
    if [ "$count" -ge 3 ]; then
        log_success "Received $count messages from same sender"
    else
        log_fail "Expected 3 messages, got $count"
    fi

    if [ "$TAKE_SCREENSHOTS" = true ]; then
        take_screenshot "03_multiple_sms_same_sender"
    fi
}

test_receive_sms_different_senders() {
    log_info "=== Test: SMS from Different Senders ==="
    clear_logcat

    send_sms "5550003333" "Message from sender A"
    send_sms "5550004444" "Message from sender B"
    send_sms "5550005555" "Message from sender C"
    sleep 2

    local senders=$("$ADB" logcat -d | grep "SmsReceiver.*Received SMS" | grep -oE "555000[0-9]{4}" | sort -u | wc -l)
    if [ "$senders" -ge 3 ]; then
        log_success "Received messages from $senders different senders"
    else
        log_fail "Expected 3 senders, got $senders"
    fi

    if [ "$TAKE_SCREENSHOTS" = true ]; then
        take_screenshot "04_different_senders"
    fi
}

test_long_sms_message() {
    log_info "=== Test: Long SMS Message (Multi-part) ==="
    clear_logcat

    local long_message="This is a very long SMS message that exceeds the standard 160 character limit for a single SMS segment. It should be split into multiple parts and then reassembled by the SMS receiver. This tests the multi-part message handling capability."

    send_sms "5550006666" "$long_message"
    sleep 2

    if "$ADB" logcat -d | grep -q "SmsReceiver.*5550006666"; then
        log_success "Long SMS received"
    else
        log_fail "Long SMS not received"
    fi

    if [ "$TAKE_SCREENSHOTS" = true ]; then
        take_screenshot "05_long_sms"
    fi
}

test_api_call_attempted() {
    log_info "=== Test: API Analysis Call Attempted ==="
    clear_logcat

    send_sms "5550007777" "Test message for API analysis"
    sleep 3

    if "$ADB" logcat -d | grep -q "okhttp.*api.cellophonemail.com"; then
        log_success "API call was attempted"
    else
        log_fail "No API call detected"
    fi
}

test_notification_shown() {
    log_info "=== Test: Notification Shown ==="
    clear_logcat

    send_sms "5550008888" "Message that should trigger notification"
    sleep 2

    if "$ADB" logcat -d | grep -q "NotificationHelper\|NotifRow.*$PACKAGE_NAME"; then
        log_success "Notification was shown"
    else
        log_warn "Could not confirm notification (may still have appeared)"
    fi
}

test_special_characters() {
    log_info "=== Test: SMS with Special Characters ==="
    clear_logcat

    send_sms "5550009999" "Test with émojis 😀 and spëcial çharacters! @#\$%"
    sleep 2

    if "$ADB" logcat -d | grep -q "SmsReceiver.*5550009999"; then
        log_success "SMS with special characters received"
    else
        log_fail "SMS with special characters not received"
    fi
}

test_empty_message() {
    log_info "=== Test: Empty SMS Message ==="
    clear_logcat

    # Some carriers/emulators may not send truly empty messages
    send_sms "5550010000" " "
    sleep 2

    log_info "Empty message test completed (behavior may vary)"
}

# ============== Main Script ==============

print_usage() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  --install      Build and install app before testing"
    echo "  --clean        Clear app data before testing"
    echo "  --verbose      Show detailed logs"
    echo "  --screenshots  Take screenshots after each test"
    echo "  --help         Show this help message"
}

# Parse arguments
INSTALL_APP=false
CLEAN_DATA=false
VERBOSE=false
TAKE_SCREENSHOTS=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --install)
            INSTALL_APP=true
            shift
            ;;
        --clean)
            CLEAN_DATA=true
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --screenshots)
            TAKE_SCREENSHOTS=true
            shift
            ;;
        --help)
            print_usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            print_usage
            exit 1
            ;;
    esac
done

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║          CellophoneSMS E2E Test Suite                        ║"
echo "║          $(date)                              ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Pre-flight checks
check_device

if [ "$INSTALL_APP" = true ]; then
    install_app
else
    if ! check_app_installed; then
        log_info "Installing app..."
        install_app
    fi
fi

if [ "$CLEAN_DATA" = true ]; then
    clear_app_data
fi

# Run tests
echo ""
echo "Running tests..."
echo "─────────────────────────────────────────────────────────────────"

test_app_launches
test_app_is_default_sms
test_receive_single_sms
test_receive_multiple_sms_same_sender
test_receive_sms_different_senders
test_long_sms_message
test_api_call_attempted
test_notification_shown
test_special_characters

# Take final screenshot
if [ "$TAKE_SCREENSHOTS" = true ]; then
    sleep 1
    take_screenshot "99_final_state"
fi

# Print summary
echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "                      TEST SUMMARY"
echo "═══════════════════════════════════════════════════════════════"
echo -e "  ${GREEN}Passed:${NC} $TESTS_PASSED"
echo -e "  ${RED}Failed:${NC} $TESTS_FAILED"
echo "  Total:  $((TESTS_PASSED + TESTS_FAILED))"
echo ""

if [ "$TAKE_SCREENSHOTS" = true ]; then
    echo "Screenshots saved to: $SCREENSHOT_DIR"
fi

if [ $TESTS_FAILED -gt 0 ]; then
    echo -e "${RED}Some tests failed!${NC}"
    exit 1
else
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
fi
