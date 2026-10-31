#!/usr/bin/env bash
# Uygulamaya gömülü reklam engelleme listesini günceller (StevenBlack/hosts, MIT).
# Yeni sürüm çıkarmadan önce çalıştırın:
#   bash website/scripts/update-adblock-list.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT="$ROOT/app/src/main/assets/adblock"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

curl -sSL -o "$TMP/hosts" https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts
curl -sSL -o "$OUT/LICENSE-StevenBlack.txt" https://raw.githubusercontent.com/StevenBlack/hosts/master/license.txt

{
  echo "# Kaynak: StevenBlack/hosts (MIT) - https://github.com/StevenBlack/hosts"
  grep -m1 "^# Date" "$TMP/hosts"
  echo "# Guncellemek icin: website/scripts/update-adblock-list.sh"
  grep "^0.0.0.0 " "$TMP/hosts" | awk '{print $2}' | grep -v "^0.0.0.0$" | sort -u
} > "$OUT/domains.txt"

echo "Güncellendi: $(grep -vc '^#' "$OUT/domains.txt") alan adı → $OUT/domains.txt"
