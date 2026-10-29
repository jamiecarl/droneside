#!/usr/bin/env bash
# Copies the race-day recordings made by the debug app (Settings › Developer › Record live data) off a phone
# connected over USB. Each recording is a folder like cmrc-20261106-180212/ holding stream.log and
# snapshots/<epoch ms>.html.gz, the same layout tools/capture-live.sh writes.
#
# Usage: tools/pull-captures.sh [out-dir]    (default: captures/phone)
set -euo pipefail

PKG=com.jamiecarl.droneside.debug
OUT="${1:-captures/phone}"
ADB="${ADB:-$(command -v adb || echo "${ANDROID_HOME:-$HOME/android-sdk}/platform-tools/adb")}"

[[ -x "$ADB" ]] || { echo "adb not found; set ADB=/path/to/adb" >&2; exit 1; }
"$ADB" get-state >/dev/null 2>&1 || { echo "No phone connected (check USB debugging)" >&2; exit 1; }

# The recordings live in the app's private storage; run-as reads them because the debug build is debuggable.
if ! "$ADB" shell run-as "$PKG" ls files/captures >/dev/null 2>&1; then
    echo "No recordings on the phone yet (they're made while race alerts are on)." >&2
    exit 1
fi

mkdir -p "$OUT"
"$ADB" exec-out run-as "$PKG" sh -c 'cd files/captures && tar -cf - .' | tar -xf - -C "$OUT"

for d in "$OUT"/*/; do
    [[ -f "$d/stream.log" ]] || continue
    frames=$(grep -c ' event: ' "$d/stream.log" || true)
    laps=$(grep -c ' event: lap' "$d/stream.log" || true)
    snaps=$(find "$d/snapshots" -name '*.html.gz' 2>/dev/null | wc -l)
    echo "$(basename "$d"): $frames frames ($laps laps), $snaps snapshots"
done
echo "Saved to $OUT"
