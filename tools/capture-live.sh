#!/usr/bin/env bash
# Records an AUFPV dronedash meeting so the live parser and session logic can be tested against real
# traffic: every SSE frame (timestamped) plus a snapshot of the dashboard HTML every 15 seconds.
#
# Usage: tools/capture-live.sh [club-url] [event-id] [out-dir]
#   club-url  defaults to https://cmrc.aufpv.org.au
#   event-id  defaults to whichever event the club's dashboard is showing
#   out-dir   defaults to captures/<club>-<event>-<date>
# Stop with Ctrl-C.
set -euo pipefail

BASE="${1:-https://cmrc.aufpv.org.au}"
BASE="${BASE%/}"
EVENT="${2:-}"
UA="DroneSide-capture"

if [[ -z "$EVENT" ]]; then
    EVENT=$(curl -fsSL -A "$UA" "$BASE/dronedash?embed=1" | grep -oE 'data-event-id="[0-9]+"' | head -1 | grep -oE '[0-9]+')
fi
[[ -n "$EVENT" ]] || { echo "No dronedash event found on $BASE" >&2; exit 1; }

HOST="${BASE#*://}"
OUT="${3:-captures/${HOST%%.*}-$EVENT-$(date +%Y%m%d-%H%M)}"
mkdir -p "$OUT/snapshots"
echo "Capturing $BASE event $EVENT into $OUT"

snapshots() {
    while true; do
        curl -fsSL -A "$UA" "$BASE/dronedash?event=$EVENT&embed=1" -o "$OUT/snapshots/$(date +%s%3N).html" || true
        sleep 15
    done
}
snapshots &
SNAP_PID=$!
trap 'kill $SNAP_PID 2>/dev/null; echo; echo "Saved to $OUT"' EXIT

# Each line of stream.log is "<epoch ms> <raw SSE line>"; blank lines (frame ends) are kept.
while true; do
    echo "$(date +%s%3N) # connect" >> "$OUT/stream.log"
    curl -sN -A "$UA" -H 'Accept: text/event-stream' "$BASE/dronedash/event/$EVENT/stream" |
        while IFS= read -r line; do
            echo "$(date +%s%3N) $line" >> "$OUT/stream.log"
            [[ "$line" == event:* && "$line" != "event: ping" ]] && echo "$(date +%T) $line"
        done || true
    echo "$(date +%s%3N) # disconnected" >> "$OUT/stream.log"
    sleep 2
done
