#!/usr/bin/env sh
# No SDK, network, test-framework replacement, or mocked JSON dependency required.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
KOTLINC=${KOTLINC:-kotlinc}
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT HUP INT TERM
CORE="$ROOT/app/src/main/java/io/github/wa_otomia/darkroom/core"
PRINTER="$ROOT/app/src/main/java/io/github/wa_otomia/darkroom/data/printer"
"$KOTLINC" -jvm-target 17 \
  "$CORE/Protocol.kt" "$CORE/Crypto.kt" "$CORE/PrintJobRestore.kt" "$CORE/ProPrinterStatus.kt" \
  "$PRINTER/BytePipe.kt" "$PRINTER/StreamBytePipe.kt" "$PRINTER/PrinterFrameReader.kt" \
  "$ROOT/tools/pro-printer-checks/CoreChecks.kt" "$ROOT/tools/pro-printer-checks/OfficialVectors.kt" \
  -include-runtime -d "$OUT/checks.jar"
java -jar "$OUT/checks.jar"
