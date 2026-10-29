# Live race data — dev plan

Status: v1 is built (Live tab, HTML snapshot plus SSE stream). It's checked against captured pages and
simulated streams, but not yet against a real race. The first real test is CMRC Micro Series Meet 9 on
2026-11-06. Last updated 2026-10-29.

## 1. What the live source actually is

### Whalefreezer/drone-dashboard (GitHub)

- Repo: https://github.com/Whalefreezer/drone-dashboard (last commit b30d3b3, 2026-06-28).
- Go backend embedding PocketBase, with a Deno/React/Vite frontend. It ingests from a venue's local FPVTrackside
  HTTP API.
- Its **websocket** is the *pits ↔ cloud control link* (`backend/control/protocol.go`). The cloud sends HTTP `fetch`
  requests down the socket and the venue laptop proxies them to FPVTrackside. Viewers never see this socket.

### AUFPV dronedash, a multi-club platform

AUFPV (aufpv.org.au) hosts club websites, and each one has a **dronedash** live-timing page. This is AUFPV's own
server-rendered port, not the Whalefreezer React app. Every site runs the same code against **one shared
database**:

- Club sites come from the "Club site" links on `https://aufpv.org.au/clubs`. Examples:
  - `cmrc.aufpv.org.au` (also served as `cmrc.asn.au`)
  - `fpvrebels.com`
  - `adelaidefpvracing.com.au`
  - `fpvr.aufpv.org.au`
  - `outerheaven.`, `pdr.`, `eastside.`, `mmrc.`, `rox.` and `wcmrc.aufpv.org.au`
  - `aufpv.org.au` itself for the nationals
- Event, race and pilot IDs are **AUFPV-wide integers**, e.g. CMRC event 11, Rebels 698, Nationals 699. Each site
  lists only its own club's events.
- Club websites embed it as `/dronedash/embed`, an iframe of `/dronedash?embed=1`.

### Endpoints (same on every club host)

| Endpoint | Type | Notes |
|---|---|---|
| `GET /dronedash/event/{eventId}/stream` | `text/event-stream` | Public, no auth, CORS `*`. Sends `: connected event=N` then events. |
| `GET /dronedash?embed=1` | HTML | Snapshot of the club's **current** event: current, last and previous race cards, next races, leaderboard and event menu. About 220 KB. |
| `GET /dronedash?event={id}&embed=1` | HTML | The same, for a specific event. |
| `GET /dronedash/races?event={id}&embed=1` | HTML | Every race. About 1.8 MB; not polled. |
| `GET /dronedash/join.json?event={id}` | JSON | Channels with colours, classes and heat-join state. |
| `GET /dronedash/race/{raceId}/commentary.json` | JSON | Commentary status. |

### SSE events

These shapes come from the site's `dronedash-home.js`; they haven't been seen on the wire yet.

| `event:` | `data:` | Meaning |
|---|---|---|
| `ping` | `{}` | Keepalive about every 20 s. |
| `lap` | `{"pilot_id":int,"lap_number":int,"length_seconds":float}` | A timed lap; lap 0 is the holeshot. Can be re-sent after a reconnect. |
| `race_state` | `{"state":"created"\|"running"\|"ended"\|"current", …}` | Race lifecycle edge. Re-fetch the snapshot. |
| `sector` | ? | Split-gate crossing. Re-fetch the snapshot. |

The stream only carries deltas. Names, channels, race order and start times come only from the HTML snapshot. The
site itself re-fetches the snapshot on every `race_state`, every 5 s while racing and every 10 s otherwise, on
reconnect, and when the tab comes back to the foreground.

### Snapshot markup hooks

These are the hooks the site's own JS relies on, and the ones `AufpvParser` reads:

- `[data-dash-root]` with `data-event-id` and `data-server-now`.
- `.races-container > .race-box[data-race-id]` with `data-race-state`, `data-start-ms` (server clock),
  `data-heat-type`, `data-target-laps`, `data-is-tt` and `data-consec-window`.
  - Heading: `.race-header h3` ("Current Race", "Last Race", "Previous Race").
  - Title: `.race-number`, e.g. "Round 2 — Time Trial 1".
  - Optional `.race-bracket-info[title]` of the form "FPVTrackside · Round 31 · Race 1".
- Pilot rows: `.laps-view-desktop .gt-row[data-pilot-id]`.
  - `[data-col=name][data-fav-name]`
  - `[data-col=chan]` (channel text plus `.channel-square` colour)
  - `[data-col=hs] [data-secs]`
  - `.lap[data-lap-cell][data-secs]`
  - `[data-pb][data-value]`
  - `.dd-qual-chip`
- Next races: the `.race-box` titled "Next Races". It has a `<strong>` title per heat and a `.pilot-meta-chip`
  per pilot.
- Events: `.dd-event-menu a.dd-event-item` (`?event=N`), `.dd-event-name`, and an optional `.dd-event-live`.

### How it lines up with FPVTrackside

- **Clubs** match by name, e.g. "Canberra Multirotor Racing Club". Some FPVTrackside clubs use the short name,
  which equals the AUFPV subdomain ("CMRC" → `cmrc`).
- **Events** match by name for CMRC, e.g. "CMRC 2026 Racing Series Meet 6" is AUFPV 11 and FPVTrackside
  `c21e0861-…`. Other clubs don't always match: Rebels uses names like "Sept 2026" and "New Event (2 Jul)".
  So v1 links at the **club** level and lets the club's dashboard choose the current event. Event-level linking
  stays best-effort for later.
- **Pilots and channels** match by callsign and channel name. IDs never match.

## 2. Risks

1. **HTML scraping.** There's no JSON snapshot, so a markup change breaks parsing. The parser only reads the
   `data-*` hooks above, and the tests run it against real captured pages. Asking AUFPV for a JSON endpoint is
   still the best fix.
2. **Courtesy and load.** Use one stream per viewer and the same refresh cadence as the site, only while the Live
   tab is on screen. Getting AUFPV's OK before a public release is still to do.
3. **Clock skew.** `skew = deviceNow - data-server-now` at fetch time, like the site.
4. **State flaps.** A re-run heat can flap running → ended → running. The timer holds for 6 s, like the site.
5. **Unverified wire format.** The lap and race_state payloads haven't been captured live yet.

## 3. What's built (v1)

```
data/live/
  LiveModels.kt     LiveClub/LiveSnapshot/LiveRace/LivePilot/StreamEvent; FPVTrackside club matching
  AufpvParser.kt    Jsoup: club directory + dronedash snapshot
  AufpvClient.kt    OkHttp: clubs, snapshot, SSE stream (okhttp-sse); 45 s read timeout = dead-stream watchdog
  LiveMath.kt       running order, gaps, best-N-consecutive (ported from dronedash-home.js)
  LiveSession.kt    sync loop: laps applied idempotently, snapshot on edges + 5 s/10 s backstop, reconnect backoff,
                    recent laps replayed onto a fresh snapshot only for the race they belong to
data/storage/LiveClubStore.kt   which club the Live tab watches
ui/screens/live/                Live tab (LiveScreen + LiveViewModel), stops streaming when not visible
ui/components/LiveTimingLink.kt green "Live timing" link used on Club details and the Home race-day banner
tools/capture-live.sh           records stream + snapshots during a meeting
```

The Live tab has:
- A club picker. It defaults to the home club when that club is on AUFPV.
- An event switcher ("Follow current event" or a past one).
- A race clock card: status, timer, fastest lap and lap progress.
- Running order, with gaps for races and best-N for time trials.
- A lap feed of streamed laps, with PB and fastest-lap tags.
- "Up next" line-ups, and result cards for the last and previous races.

Tests in `app/src/test`: the parser against real pages from CMRC, Rebels and the Nationals, the race maths, and
the session on virtual time with a fake stream.

## 4. Next

| When | What |
|---|---|
| 2026-11-06 (Micro Meet 9) | Run `tools/capture-live.sh https://cmrc.aufpv.org.au` for the whole meeting, and use the app alongside it. Note anything that differs from the website. |
| After the meeting | Add captured frames and snapshots as fixtures; confirm the `lap` and `race_state` payloads; fix markup drift; write a replay test that feeds a real stream log through `LiveSession`. |
| Later | Leaderboard tab; split-sector chips; favourites and "your pilot is on deck" notifications; link races to FPVTrackside results through the `race-bracket-info` hint; ask AUFPV about a JSON snapshot and permission for third-party clients. |

## 5. Open questions

1. Will AUFPV add a JSON snapshot and officially allow third-party clients?
2. What extra fields does `race_state` carry, such as a race ID? This needs a live capture.
3. Does `.dd-event-live` mark the event that's racing now? The default page might already follow it. This also
   needs a live capture.
