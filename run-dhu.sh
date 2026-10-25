#!/usr/bin/env bash
# ==============================================================================
# AutoLounge - Android Auto Desktop Head Unit (DHU) Runner
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/headunit.ini"

echo "=========================================================="
echo "🏎️  AutoLounge — Android Auto DHU Başlatıcı"
echo "=========================================================="

# 1. Android SDK Konumunu Belirle
SDK_DIR=""
if [[ -n "${ANDROID_HOME:-}" && -d "${ANDROID_HOME}" ]]; then
    SDK_DIR="${ANDROID_HOME}"
elif [[ -n "${ANDROID_SDK_ROOT:-}" && -d "${ANDROID_SDK_ROOT}" ]]; then
    SDK_DIR="${ANDROID_SDK_ROOT}"
elif [[ -f "${SCRIPT_DIR}/local.properties" ]]; then
    PROP_SDK="$(grep -E '^[[:space:]]*sdk\.dir' "${SCRIPT_DIR}/local.properties" | cut -d'=' -f2 | tr -d '\r' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
    if [[ -n "${PROP_SDK}" && -d "${PROP_SDK}" ]]; then
        SDK_DIR="${PROP_SDK}"
    fi
fi

if [[ -z "${SDK_DIR}" && -d "${HOME}/Android/Sdk" ]]; then
    SDK_DIR="${HOME}/Android/Sdk"
fi

# 2. ADB Komutunu Doğrula
ADB_BIN="adb"
if ! command -v adb >/dev/null 2>&1; then
    if [[ -n "${SDK_DIR}" && -x "${SDK_DIR}/platform-tools/adb" ]]; then
        ADB_BIN="${SDK_DIR}/platform-tools/adb"
    else
        echo "❌ [AutoLounge] HATA: 'adb' bulunamadı! PATH veya Android SDK platform-tools kontrol edin."
        exit 1
    fi
fi

# 3. DHU Binary Kontrolü
DHU_DIR=""
DHU_BIN=""
if [[ -n "${SDK_DIR}" && -x "${SDK_DIR}/extras/google/auto/desktop-head-unit" ]]; then
    DHU_DIR="${SDK_DIR}/extras/google/auto"
    DHU_BIN="${DHU_DIR}/desktop-head-unit"
elif command -v desktop-head-unit >/dev/null 2>&1; then
    DHU_BIN="$(command -v desktop-head-unit)"
    DHU_DIR="$(dirname "${DHU_BIN}")"
else
    echo "❌ [AutoLounge] HATA: Desktop Head Unit (DHU) bulunamadı!"
    echo "   Android Studio SDK Manager -> SDK Tools -> 'Android Auto Desktop Head Unit Emulator' bileşenini yükleyin."
    echo "   Beklenen dizin: ${SDK_DIR:-~/Android/Sdk}/extras/google/auto/desktop-head-unit"
    exit 1
fi

# 4. ADB Cihaz ve Port Yönlendirme (Port Forwarding & Reverse 5277)
echo "🔌 [AutoLounge] ADB bağlantısı kontrol ediliyor..."
"${ADB_BIN}" start-server >/dev/null 2>&1 || true

DEVICE_COUNT="$("${ADB_BIN}" devices | grep -v "List of" | grep -c "device" || true)"
if [[ "${DEVICE_COUNT}" -eq 0 ]]; then
    echo "⚠️  [AutoLounge] UYARI: Bağlı ve yetkilendirilmiş bir Android cihazı bulunamadı!"
    echo "   - Telefonunuzu USB ile bağlayıp USB Hata Ayıklama (USB Debugging) iznini verin."
    echo "   - Android Auto uygulamasında Geliştirici Ayarları'na girin."
    echo "   - 'Baş ünite sunucusunu başlat' (Start head unit server) seçeneğini aktif edin."
    echo "   ------------------------------------------------------"
else
    echo "✅ [AutoLounge] Android cihazı algılandı (${DEVICE_COUNT} cihaz bağlı)."
fi

echo "🔄 [AutoLounge] TCP 5277 port yönlendirmesi kuruluyor (Forward & Reverse)..."
# DHU (Client) -> Telefon (Head Unit Server) için port forward
"${ADB_BIN}" forward tcp:5277 tcp:5277 2>/dev/null || true
# İki yönlü uyumluluk için port reverse (cihazın DHU hostuna erişimi için)
"${ADB_BIN}" reverse tcp:5277 tcp:5277 2>/dev/null || true

# 5. Linux Kütüphane Yollarını Ayarla
export LD_LIBRARY_PATH="${DHU_DIR}/lib:${DHU_DIR}:${LD_LIBRARY_PATH:-}"

# 6. DHU'yu Başlat
echo "🚀 [AutoLounge] DHU Başlatılıyor..."
echo "   Yapılandırma: ${CONFIG_FILE}"

# Eğer kullanıcı özel -c parametresi vermediyse varsayılan CONFIG_FILE kullan
DHU_ARGS=()
HAS_CONFIG_ARG=0
for arg in "$@"; do
    if [[ "${arg}" == "-c" ]]; then
        HAS_CONFIG_ARG=1
        break
    fi
done

if [[ "${HAS_CONFIG_ARG}" -eq 0 && -f "${CONFIG_FILE}" ]]; then
    DHU_ARGS+=("-c" "${CONFIG_FILE}")
fi

if [ -t 0 ]; then
    exec "${DHU_BIN}" "${DHU_ARGS[@]}" "$@"
else
    echo "ℹ️  [AutoLounge] stdin aktif bir TTY değil; DHU'nun kapanmaması için stdin açık tutuluyor..."
    tail -f /dev/null | exec "${DHU_BIN}" "${DHU_ARGS[@]}" "$@"
fi
