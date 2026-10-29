# Live race data — dev plan

Status: draft, researched 2026-10-29. Nothing implemented yet.

## 1. What the live source actually is

The assumption going in was "AUFPV websockets stream, built on Drone Dashboard". Research says otherwise in two
important ways.

### Whalefreezer/drone-dashboard (GitHub)

- Repo: https://github.com/Whalefreezer/drone-dashboard (last commit b30d3b3, 2026-06-28).
- Go backend embedding PocketBase, with a Deno/React/Vite frontend. It ingests from a venue's local FPVTrackside
  HTTP API on a schedule, then stores the data in PocketBase collections.
- The **websocket** in that project is the *pits ↔ cloud control link* (`backend/control/protocol.go`: `hello`,
  `fetch`, `response`, `ping`/`pong` envelopes). The cloud sends HTTP `fetch` requests down the socket and the venue
  laptop proxies them to FPVTrackside. Viewers never touch this socket; they get updates via PocketBase realtime.

### What cmrc.asn.au/dronedash actually runs

`https://cmrc.asn.au/dronedash/embed` is a wrapper page holding an iframe of `/dronedash?embed=1`. That page is
**not** the Whalefreezer React app. It's an AUFPV-hosted, server-rendered port with its own routes, integer IDs and
JS (`/static/js/dronedash-*.js`). It shares the theme key `aufpv_theme` with the rest of the CMRC site. I can't tell
from outside whether its ingest uses Whalefreezer's pits link.

The live transport here is **Server-Sent Events, not websockets**:

| Endpoint | Type | Notes |
|---|---|---|
| `GET /dronedash/event/{eventId}/stream` | `text/event-stream` | Public, no auth, `Access-Control-Allow-Origin: *`. Confirmed live: `: connected event=11` then `event: ping`. |
| `GET /dronedash?event={id}&embed=1` | HTML | The full snapshot: current, last and previous race cards, leaderboard and upcoming list. ~220 KB. |
| `GET /dronedash/races?event={id}&embed=1` | HTML | Every race in the event. ~1.8 MB, too heavy to poll. |
| `GET /dronedash/join.json?event={id}` | JSON | Channel list with colours, classes, heat-join state. Useful for channel colours. |
| `GET /dronedash/race/{raceId}/commentary.json` | JSON | `{"race_id":…,"status":"none"}` when unused. |
| `GET /dronedash/pilot/{pilotId}?event={id}` | HTML | Pilot page. |

SSE events (from `dronedash-home.js`):

| `event:` | `data:` | Meaning |
|---|---|---|
| `ping` | `{}` | Keepalive about every 20 s. The site treats 30 s of silence as a dead stream. |
| `lap` | `{"pilot_id":int,"lap_number":int,"length_seconds":float}` | A timed lap. `lap_number` 0 is the holeshot. **Can be re-sent after a reconnect**, so apply it idempotently. |
| `sector` | (ignored by the site) | A split-gate crossing. The site just re-fetches the snapshot. |
| `race_state` | `{"state":"created"\|"running"\|"ended"\|"current", …}` | A race lifecycle edge. The site re-fetches the snapshot. Other fields are probably present but unconfirmed. |

The stream carries only **deltas keyed by AUFPV-internal integer IDs**: pilot 113, race 3593, event 11. Names,
channels, race order and start time come only from the HTML snapshot. The site itself doesn't trust the stream
alone. It soft-refreshes the snapshot on every `race_state`, every 5 s while a race runs, every 10 s between races,
on reconnect, and when the tab returns to the foreground.

### Snapshot markup the client depends on

Taken from the live page. These hooks are stable enough that the site's own JS relies on them:

- Root: `[data-dash-root]` with `data-event-id` and `data-server-now` (ms, used for clock skew).
- Race card: `.race-box` (current race adds `.current-race`) with `data-race-id`, `data-race-state`
  (`pending`/`running`/`ended`), `data-start-ms` (server-clock epoch ms, `0` before start), `data-heat-type`
  (`Race`/`Time Trial`), `data-target-laps`, `data-is-tt`, `data-consec-window`. Header text reads
  "Round 2 — Time Trial 1".
- Pilot row: `.laps-view-desktop .gt-row[data-pilot-id]` containing:
  - `[data-col="name"]`, with the name in `data-fav-name`
  - `[data-col="chan"]`, with the channel text plus `.channel-square` background colour
  - `[data-col="hs"] [data-secs]`
  - `[data-col="laps"] .lap[data-lap-cell][data-secs]`
  - `[data-total]`
  - `[data-pb][data-value]`
- Event list: `.dd-event-item` links (`?event={id}`) with a `.dd-event-name` child.
- Leaderboard: `[data-leaderboard] .gt-row[data-pilot-id]`.

### Does it line up with our FPVTrackside data?

Yes, by name. AUFPV event names match FPVTrackside exactly, e.g. "CMRC 2026 Racing Series Meet 6" is AUFPV `11` and
FPVTrackside `c21e0861-…`, and "CMRC 2026 Racing Series Meet 9" is `b1f7691c-…`. Pilot callsigns, channel names
(L6, R1, F2…) and colours match FPVTrackside's too. **IDs never match**: AUFPV uses ints, FPVTrackside uses GUIDs. Two
AUFPV events share the name "…Meet 8 (MultiGP Global Qualifier)", so matching needs name plus date.

Nothing was racing on 2026-10-29 (Thursday), so no `lap` or `race_state` frames were captured. The payload shapes
above come from the site's JS, not from the wire. The next CMRC meeting in FPVTrackside is **Micro Series Meet 9 on
2026-11-06**. Capture a real stream then (see Phase 0).

## 2. Constraints and risks

1. **No JSON snapshot.** We'd be scraping HTML for state. A markup change breaks us silently. Mitigations: scrape
   only the `data-*` hooks the site's own JS depends on, version-check with a parse-health metric, and fall back to
   FPVTrackside data on failure. The best fix is to ask AUFPV for a JSON snapshot endpoint, e.g.
   `/dronedash/event/{id}/state.json`. This is the single highest-value ask.
2. **Undocumented, third-party, CMRC-scoped.** The stream lives on `cmrc.asn.au`. Other clubs may run AUFPV on other
   hosts or not at all. Treat the live source as an optional per-club integration, not a global feature.
3. **Courtesy and load.** Get AUFPV/CMRC's OK before shipping. Poll the snapshot only while a Live screen is
   visible, never in the background. Respect the site's own cadence: one stream per viewer and snapshot refreshes no
   faster than 5 s.
4. **Clock skew.** `data-start-ms` is server time. Compute `skew = deviceNow - data-server-now` once per snapshot,
   exactly as the site does.
5. **Flapping state.** A re-run heat can flick `running → ended → running`. Hold the running timer for 6 s like the
   site's `TIMER_HOLD_MS`.

## 3. Architecture in the app

```
data/live/
  LiveSource.kt            // per-club config: base URL (https://cmrc.asn.au), enabled flag
  DronedashClient.kt       // OkHttp: SSE stream + snapshot GET; no new deps (okhttp-sse is optional, see below)
  DronedashSnapshotParser  // HTML → LiveSnapshot (Jsoup)
  LiveEventMapper.kt       // AUFPV event ↔ FPVTrackside event (name + date), pilot ↔ pilot (callsign)
  LiveRaceRepository.kt    // merges snapshot + lap deltas into StateFlow<LiveState>; owns reconnect/watchdog
ui/screens/live/
  LiveScreen.kt + LiveViewModel.kt
```

- **Transport:** add `com.squareup.okhttp3:okhttp-sse:4.12.0`, matching our OkHttp version. It parses the
  `event:`/`data:` frames for us. Give the stream its own `OkHttpClient` with `readTimeout(0)` and no
  `HttpLoggingInterceptor` body logging, which would buffer the stream forever.
- **Parsing:** add `org.jsoup:jsoup:1.18.1` for the snapshot, and select only by the `data-*` hooks listed above.
  Use Gson for the SSE payloads and `join.json`. Gson is already a dependency.
- **State model:**
  ```kotlin
  data class LiveState(
      val connection: Connection,            // Connecting / Live / Stale / Offline
      val eventName: String,
      val current: LiveRace?, val last: LiveRace?, val next: LiveRace?,
      val skewMs: Long
  )
  data class LiveRace(val id: Int, val title: String, val state: RaceState, val startMs: Long,
                      val heatType: HeatType, val targetLaps: Int, val pilots: List<LivePilot>)
  data class LivePilot(val id: Int, val name: String, val channel: String, val color: Color,
                       val laps: Map<Int, Double>)   // key 0 = holeshot → idempotent on re-sent laps
  ```
  Positions, gaps, best lap and best-N-consecutive are derived in Kotlin, porting `computeGaps` and `bestConsec`.
  Unit-test them against the site's rules: most laps first, then earliest finish, compare at the last lap both
  pilots completed, ignore laps past the target.
- **Sync loop**, mirroring the site:
  1. Connect the stream, then fetch the snapshot.
  2. On a `lap` event, upsert `laps[lap_number]` straight away.
  3. On a `race_state` event, refetch the snapshot, debounced by 150 ms.
  4. On a `sector` event, refetch with a 400 ms debounce. This only matters once splits are shown.
  5. Backstop refetch every 5 s while a race is running and every 10 s otherwise.
  6. A watchdog reconnects after 30 s with no frames. On an error, reconnect after a 2 s fixed delay, backing off
     to 30 s after repeated failures.
  7. Run all of this only while `Lifecycle.State.STARTED`, using `repeatOnLifecycle`, so it stops when the screen
     is backgrounded.
- **Linking to existing screens:** `LiveEventMapper` gives us the FPVTrackside event ID, so "View full results"
  opens our existing `EventDetails` and `RaceDetails` screens. Pilot names map to FPVTrackside pilots by callsign
  so we can show photos.

## 4. UI (from the design canvas)

This follows the "Live" board in the design artifact (https://claude.ai/artifact/7MhYi3UgobuT8W3AzPNmot):

- **Live entry points:**
  - A pulsing "LIVE" chip on the Dashboard's home-club card and on `EventCard` when the event is live.
  - A Live tab or banner on Event details.
- **Live screen:**
  - Header with the race title, a running timer (100 ms tick with skew correction) and a connection dot.
  - Running-order rows: position, channel colour bar, callsign, laps pill row with HS and best lap in cyan, total,
    and a gap or "Leader" label.
  - For time trials, show "Best N" and PB instead of gaps.
  - "Last race" results card and "Next up" line-up below.
- Animate row reordering with `LazyColumn` plus `Modifier.animateItem()`, and flash a row on each new lap.
- Show a "Reconnecting…" pill when stale. Never blank the screen; keep showing the last good state.

## 5. Phases

| # | Deliverable | Done when |
|---|---|---|
| 0 | **Capture & confirm** (no app code). Record a full stream plus snapshots during Micro Meet 9 (2026-11-06), using a small script in `tools/`. Contact AUFPV/CMRC for permission and to ask about a JSON snapshot. | We have real `lap`/`race_state`/`sector` frames saved as test fixtures, and an answer from AUFPV. |
| 1 | **Data layer**: `DronedashClient` (SSE + snapshot), Jsoup parser, `LiveRaceRepository` with the sync loop, and the gap and consecutive maths. | Unit tests pass against the captured fixtures, including a replay test that feeds the recorded stream and snapshots and checks the final state. |
| 2 | **Event mapping**: AUFPV event list ↔ FPVTrackside events by name+date; per-club `LiveSource` config (CMRC hard-coded first). | Every 2026 CMRC event maps to exactly one AUFPV ID, and the test covers the duplicate-name case. |
| 3 | **Live screen**: route `live/{fpvEventId}`, ViewModel, rows, timer, connection states, entry points. | A fixture-replay "demo mode" (debug only) drives the screen end to end. |
| 4 | **Field test** at a real meeting, then fix-ups for markup drift, reconnects on mobile data, and battery use. | The app tracks a full meeting without a manual refresh, and it matches the website lap for lap. |
| 5 | **Polish / later**: split-sector chips, favourites, optional notification for "your pilot is on deck", and swapping the HTML scrape for a JSON endpoint if AUFPV adds one. | — |

## 6. Open questions

1. Will AUFPV add a JSON snapshot endpoint and officially permit third-party clients? Their answer decides whether
   Phase 1 scrapes HTML at all.
2. Is AUFPV running for any club besides CMRC, and on which hosts? That decides whether `LiveSource` needs a club
   list or remote config.
3. Is there a way to find the *active* AUFPV event, other than the default `/dronedash` page? The default showed
   event 11 (Meet 6, June) on a non-race day, so it may just be the last pinned event.
4. What extra fields does `race_state` carry, such as a race ID? That needs Phase 0 capture.
