#!/usr/bin/env bash
# ==============================================================================
# AutoLounge — Android Auto One-Click Installer (macOS / Linux)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" 2>/dev/null && pwd || pwd)"
TOOLS_DIR="${SCRIPT_DIR}/tools"

GREEN="\033[1;32m"
YELLOW="\033[1;33m"
RED="\033[1;31m"
CYAN="\033[1;36m"
BOLD="\033[1m"
RESET="\033[0m"

echo -e "${CYAN}=====================================================================${RESET}"
echo -e "${BOLD}   🏎️  AutoLounge — Android Auto One-Click Installer (macOS / Linux)${RESET}"
echo -e "${CYAN}=====================================================================${RESET}"
echo -e "This tool installs AutoLounge stamped with the Google Play Store installer ID"
echo -e "(\"com.android.vending\") ${GREEN}WITHOUT ROOTING${RESET} your smartphone."
echo -e "This unlocks full-screen YouTube, Live TV and web apps on your car display.\n"

# 1. OS DETECTION
OS_TYPE="$(uname -s)"
case "${OS_TYPE}" in
    Darwin*) PLATFORM="darwin" ;;
    Linux*)  PLATFORM="linux" ;;
    *)       PLATFORM="unknown" ;;
esac

# 2. ADB CHECK
ADB_BIN="adb"
if command -v adb >/dev/null 2>&1; then
    echo -e "${GREEN}[✓] ADB (Android Debug Bridge) detected on your system.${RESET}"
elif [[ -x "${TOOLS_DIR}/platform-tools/adb" ]]; then
    ADB_BIN="${TOOLS_DIR}/platform-tools/adb"
    echo -e "${GREEN}[✓] ADB found in local tools folder: ${ADB_BIN}${RESET}"
else
    echo -e "${YELLOW}[!] 'adb' command was not found on your system.${RESET}"
    echo -e "    Google Platform Tools (~18 MB) is required to proceed with installation.\n"
    
    read -rp "Would you like to automatically download Google Platform Tools? [Y/n]: " choice < /dev/tty || choice="Y"
    choice="${choice:-Y}"
    if [[ ! "$choice" =~ ^[YyEe]$ ]]; then
        echo -e "${RED}[X] Cannot proceed without ADB. Aborting.${RESET}"
        exit 1
    fi

    mkdir -p "${TOOLS_DIR}"
    ZIP_PATH="/tmp/platform-tools-${PLATFORM}.zip"

    if [[ "${PLATFORM}" == "darwin" ]]; then
        DOWNLOAD_URL="https://dl.google.com/android/repository/platform-tools-latest-darwin.zip"
    else
        DOWNLOAD_URL="https://dl.google.com/android/repository/platform-tools-latest-linux.zip"
    fi

    echo -e "${CYAN}[*] Downloading from official Google servers: ${DOWNLOAD_URL}${RESET}"
    curl -fsSL -o "${ZIP_PATH}" "${DOWNLOAD_URL}"

    echo -e "${CYAN}[*] Extracting files...${RESET}"
    unzip -q -o "${ZIP_PATH}" -d "${TOOLS_DIR}"
    rm -f "${ZIP_PATH}"
    chmod +x "${TOOLS_DIR}/platform-tools/adb"
    ADB_BIN="${TOOLS_DIR}/platform-tools/adb"
    echo -e "${GREEN}[✓] ADB successfully installed.${RESET}"
fi

# 3. LOCATE OR DOWNLOAD APK
echo -e "\n${CYAN}[*] Searching for AutoLounge APK file...${RESET}"
TARGET_APK=""

shopt -s nullglob
apk_files=("${SCRIPT_DIR}"/AutoLounge*.apk "${SCRIPT_DIR}"/app/build/outputs/apk/debug/*.apk "${SCRIPT_DIR}"/*.apk)
shopt -u nullglob

if [[ ${#apk_files[@]} -gt 0 ]]; then
    TARGET_APK="${apk_files[0]}"
    echo -e "${GREEN}[✓] APK file found: ${TARGET_APK}${RESET}"
else
    echo -e "${YELLOW}[!] No AutoLounge APK file found in directory.${RESET}"
    read -rp "Download the latest APK from GitHub Releases? [Y/n]: " choice_apk < /dev/tty || choice_apk="Y"
    choice_apk="${choice_apk:-Y}"
    if [[ ! "$choice_apk" =~ ^[YyEe]$ ]]; then
        echo -e "${RED}[X] Please place AutoLounge.apk in this directory and run again.${RESET}"
        exit 1
    fi

    TARGET_APK="${SCRIPT_DIR}/AutoLounge.apk"
    echo -e "${CYAN}[*] Downloading latest APK from GitHub...${RESET}"
    API_URL="https://api.github.com/repos/salihoz0/AutoLounge/releases/latest"
    DOWNLOAD_URL="$(curl -fsSL "${API_URL}" | grep "browser_download_url.*\.apk" | head -n1 | cut -d '"' -f 4 || true)"
    
    if [[ -n "${DOWNLOAD_URL}" ]]; then
        curl -fsSL -L -o "${TARGET_APK}" "${DOWNLOAD_URL}"
        echo -e "${GREEN}[✓] APK successfully downloaded: ${TARGET_APK}${RESET}"
    else
        echo -e "${RED}[X] No released APK found on GitHub. Please copy an APK file into this directory.${RESET}"
        exit 1
    fi
fi

# 4. DEVICE CONNECTION CHECK
echo -e "\n${CYAN}=====================================================================${RESET}"
echo -e "${BOLD} 📲 Checking Device Connection${RESET}"
echo -e "${CYAN}=====================================================================${RESET}"
echo -e " Please verify the following:"
echo -e "  1. Your phone is connected to your computer via USB."
echo -e "  2. 'USB Debugging' is ${YELLOW}REQUIRED${RESET} and enabled in Developer Options."
echo -e "  3. If 'Allow USB debugging?' pops up on your phone screen, tap ${GREEN}ALLOW${RESET}."
echo -e "  4. (Xiaomi / Redmi / POCO only) 'Install via USB' must be ${GREEN}ENABLED${RESET}."
echo -e "${CYAN}=====================================================================${RESET}\n"

"${ADB_BIN}" start-server >/dev/null 2>&1 || true

while true; do
    DEVICE_LINE="$("${ADB_BIN}" devices | grep -v "List of devices" | grep -v "^$" | grep -v "^emulator-" | head -n1 || true)"
    
    if [[ -z "${DEVICE_LINE}" ]]; then
        echo -e "${YELLOW}[!] No connected phone detected.${RESET}"
        echo -e "    Connect your phone via USB and enable USB Debugging."
    else
        DEVICE_ID="$(echo "${DEVICE_LINE}" | awk '{print $1}')"
        DEVICE_STATE="$(echo "${DEVICE_LINE}" | awk '{print $2}')"
        
        if [[ "${DEVICE_STATE}" == "device" ]]; then
            echo -e "${GREEN}[✓] Phone connected and authorized: ${DEVICE_ID}${RESET}"
            export ANDROID_SERIAL="${DEVICE_ID}"
            break
        elif [[ "${DEVICE_STATE}" == "unauthorized" ]]; then
            echo -e "${YELLOW}[!] Phone detected, but UNAUTHORIZED.${RESET}"
            echo -e "    Please unlock your phone screen and tap 'Always allow from this computer'."
        fi
    fi
    
    read -rp "Press Enter to retry (or CTRL+C to quit): " _ < /dev/tty || true
done

# 5. INSTALL WITH GOOGLE PLAY VERIFICATION
echo -e "\n${CYAN}=====================================================================${RESET}"
echo -e "${BOLD} 🚀 Installing AutoLounge with Google Play Store signature...${RESET}"
echo -e "${CYAN}=====================================================================${RESET}"
echo -e "Command: ${BOLD}${ADB_BIN} install -i \"com.android.vending\" -r \"${TARGET_APK}\"${RESET}\n"

if "${ADB_BIN}" install -i "com.android.vending" -r "${TARGET_APK}"; then
    echo -e "\n${GREEN}=====================================================================${RESET}"
    echo -e "${GREEN}${BOLD} 🎉 SUCCESS! INSTALLATION COMPLETE!${RESET}"
    echo -e "${GREEN}=====================================================================${RESET}"
    # Samsung Dual Messenger auto-clones Play-signed installs into a DUAL_APP user (second icon with clone badge)
    for u in $("${ADB_BIN}" shell pm list users | grep DUAL_APP | sed -E 's/.*UserInfo\{([0-9]+):.*/\1/'); do
        "${ADB_BIN}" shell pm uninstall --user "${u}" com.autolounge.park >/dev/null 2>&1 || true
    done
    echo -e " ✓ AutoLounge is now installed with Google Play Store verification."
    echo -e " ✓ Plug your phone into your vehicle to launch AutoLounge on your car screen.\n"
    
    echo -e "${CYAN}=====================================================================${RESET}"
    echo -e "${BOLD} ⚙️  Opening Android Auto Settings on your phone...${RESET}"
    echo -e "${CYAN}=====================================================================${RESET}"
    "${ADB_BIN}" shell am start -n com.google.android.projection.gearhead/.companion.settings.DefaultSettingsActivity >/dev/null 2>&1 || true
    echo -e " Please check your phone screen and perform these 3 quick steps:"
    echo -e "  1. Scroll to the very bottom and tap ${BOLD}'Version'${RESET} 10 times."
    echo -e "  2. Tap the ${BOLD}3 dots (⋮)${RESET} in the top-right corner -> ${BOLD}Developer settings${RESET}."
    echo -e "  3. Check the box for ${GREEN}${BOLD}'Unknown sources'${RESET}.\n"
    echo -e " Need help finding it on your specific phone model? Ask an AI assistant:"
    echo -e "  • ChatGPT: ${CYAN}https://chatgpt.com/?q=Android+Auto+Developer+Settings+Unknown+sources+how+to+enable+on+model:+${RESET}"
    echo -e "  • Claude:  ${CYAN}https://claude.ai/new?q=Android+Auto+Developer+Settings+Unknown+sources+how+to+enable+on+model:+${RESET}"
    echo -e "  • Gemini:  ${CYAN}https://gemini.google.com/app?q=Android+Auto+Developer+Settings+Unknown+sources+how+to+enable+on+model:+${RESET}\n"
    echo -e "${GREEN}All set! AutoLounge is ready for your vehicle.${RESET}\n"
else
    echo -e "\n${RED}=====================================================================${RESET}"
    echo -e "${RED}${BOLD} [X] INSTALLATION FAILED!${RESET}"
    echo -e "${RED}=====================================================================${RESET}"
    echo -e " Please check common causes:\n"
    echo -e " 1. ${YELLOW}XIAOMI / REDMI / POCO DEVICES (IMPORTANT):${RESET}"
    echo -e "    In Developer Options, you MUST enable:"
    echo -e "    -> 'Install via USB'"
    echo -e "    -> 'USB Debugging (Security settings)'"
    echo -e "    (Otherwise, MIUI/HyperOS blocks ADB installations).\n"
    echo -e " 2. ${YELLOW}PHONE SCREEN LOCKED OR PROMPT PENDING:${RESET}"
    echo -e "    Unlock your phone and accept any installation prompts.\n"
    echo -e " 3. ${YELLOW}EXISTING VERSION CONFLICT:${RESET}"
    echo -e "    If an older version is installed, uninstall it from your phone and retry."
    exit 1
fi
