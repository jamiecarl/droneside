package com.jamiecarl.droneside.data.live

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LiveConnection { CONNECTING, LIVE, RECONNECTING }

data class LapFeedItem(
    val id: Long,
    val pilotName: String,
    val color: String?,
    val lapNumber: Int,
    val seconds: Double,
    /** Race clock when the lap landed, in seconds; null if the race has no start time yet. */
    val raceClock: Double?,
    val isRaceBest: Boolean,
    val isPersonalBest: Boolean
)

data class LiveSessionState(
    val snapshot: LiveSnapshot? = null,
    val connection: LiveConnection = LiveConnection.CONNECTING,
    val error: String? = null,
    /** How far this device's clock is ahead of the server's. */
    val clockSkewMs: Long = 0,
    val feed: List<LapFeedItem> = emptyList(),
    /** Race id to the device time we last saw it running; drives the timer's hold through state flaps. */
    val lastRunningAt: Map<Int, Long> = emptyMap()
)

/**
 * Keeps one club's dronedash in sync the way the site itself does: laps from the SSE stream are applied
 * straight away, and the HTML snapshot is re-fetched on every race-state edge, on reconnect, and on a
 * backstop timer (5 s while a race runs, 10 s otherwise) in case an edge was missed.
 *
 * [run] does the work until its coroutine is cancelled, so the caller ties it to the screen being visible.
 */
class LiveSession(
    private val baseUrl: String,
    /** Null follows whichever event the club's dashboard is showing (a new meeting is picked up on its own). */
    private val pinnedEventId: Int?,
    private val fetchSnapshot: suspend (String, Int?) -> LiveSnapshot = AufpvClient::snapshot,
    private val openStream: (String, Int) -> Flow<StreamEvent> = AufpvClient::stream,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val _state = MutableStateFlow(LiveSessionState())
    val state: StateFlow<LiveSessionState> = _state.asStateFlow()

    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)
    private val recentLaps = ArrayDeque<AppliedLap>()
    private var lastRefreshAt = 0L
    private var feedIds = 0L

    private class EventChanged : Exception()

    fun requestRefresh() {
        refreshRequests.trySend(Unit)
    }

    suspend fun run(): Unit = coroutineScope {
        launch { refreshLoop() }
        launch { backstopLoop() }
        requestRefresh()
        streamLoop()
    }

    private suspend fun refreshLoop() {
        for (request in refreshRequests) {
            delay(REFRESH_COALESCE_MS)
            try {
                applySnapshot(fetchSnapshot(baseUrl, pinnedEventId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_state.value.snapshot == null) {
                    _state.update { it.copy(error = e.message ?: "Couldn't reach the live dashboard") }
                }
            }
            lastRefreshAt = now()
        }
    }

    private suspend fun backstopLoop() {
        while (true) {
            delay(BACKSTOP_TICK_MS)
            val running = _state.value.snapshot?.current?.state == LiveRaceState.RUNNING
            val due = if (running) RESYNC_RUNNING_MS else RESYNC_IDLE_MS
            // Fire on the last tick before it would be overdue, so the cadence is `due`, not `due` + a tick.
            if (now() - lastRefreshAt + BACKSTOP_TICK_MS > due) requestRefresh()
        }
    }

    private suspend fun streamLoop() {
        var failures = 0
        while (true) {
            val eventId = _state.value.snapshot?.eventId ?: _state.first { it.snapshot != null }.snapshot!!.eventId
            try {
                openStream(baseUrl, eventId).collect { event ->
                    failures = 0
                    handle(event)
                    if (_state.value.snapshot?.eventId != eventId) throw EventChanged()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e !is EventChanged) failures++
            }
            _state.update { it.copy(connection = LiveConnection.RECONNECTING) }
            delay((RECONNECT_BASE_MS shl failures.coerceAtMost(4)).coerceAtMost(RECONNECT_MAX_MS))
        }
    }

    private fun handle(event: StreamEvent) {
        if (_state.value.connection != LiveConnection.LIVE) _state.update { it.copy(connection = LiveConnection.LIVE) }
        when (event) {
            StreamEvent.Open -> requestRefresh()
            is StreamEvent.Lap -> onLap(event)
            is StreamEvent.RaceState, StreamEvent.Sector -> requestRefresh()
            StreamEvent.Ping, is StreamEvent.Other -> Unit
        }
    }

    private fun onLap(lap: StreamEvent.Lap) {
        val snapshot = _state.value.snapshot ?: return
        val result = LiveReducer.applyLap(snapshot, lap)
        if (result == null) {
            // A lap for someone not on our current card: the card is stale (the next heat has started).
            requestRefresh()
            return
        }
        val (updated, race) = result
        recentLaps.addLast(AppliedLap(race.id, lap, now()))
        while (recentLaps.size > MAX_RECENT_LAPS) recentLaps.removeFirst()

        val skew = _state.value.clockSkewMs
        val item = LiveReducer.feedItem(++feedIds, updated.current ?: race, lap) { start ->
            if (start > 0) (now() - skew - start) / 1000.0 else null
        }
        _state.update { s ->
            s.copy(snapshot = updated, feed = (listOfNotNull(item) + s.feed).take(FEED_SIZE))
        }
    }

    private fun applySnapshot(fresh: LiveSnapshot) {
        val cutoff = now() - LAP_REPLAY_WINDOW_MS
        while (recentLaps.isNotEmpty() && recentLaps.first().receivedAt < cutoff) recentLaps.removeFirst()
        val merged = LiveReducer.replayLaps(fresh, recentLaps)
        val at = now()
        _state.update { s ->
            val eventChanged = s.snapshot != null && s.snapshot.eventId != merged.eventId
            s.copy(
                snapshot = merged,
                error = null,
                clockSkewMs = if (merged.serverNowMs > 0) at - merged.serverNowMs else s.clockSkewMs,
                feed = if (eventChanged) emptyList() else s.feed,
                lastRunningAt = s.lastRunningAt + merged.races
                    .filter { it.state == LiveRaceState.RUNNING }
                    .associate { it.id to at }
            )
        }
    }

    companion object {
        const val REFRESH_COALESCE_MS = 250L
        const val BACKSTOP_TICK_MS = 2_500L
        const val RESYNC_RUNNING_MS = 5_000L
        const val RESYNC_IDLE_MS = 10_000L
        const val RECONNECT_BASE_MS = 2_000L
        const val RECONNECT_MAX_MS = 30_000L
        /** A re-run heat can flap running → ended → running; keep the clock going through the blip. */
        const val TIMER_HOLD_MS = 6_000L
        const val LAP_REPLAY_WINDOW_MS = 120_000L
        const val MAX_RECENT_LAPS = 200
        const val FEED_SIZE = 8
    }
}

data class AppliedLap(val raceId: Int, val lap: StreamEvent.Lap, val receivedAt: Long)

/** The pure parts of [LiveSession], split out for tests. */
object LiveReducer {

    /** Puts a streamed lap on the current race card. Null if that pilot isn't in the current race. */
    fun applyLap(snapshot: LiveSnapshot, lap: StreamEvent.Lap): Pair<LiveSnapshot, LiveRace>? {
        val current = snapshot.current ?: return null
        if (current.pilots.none { it.id == lap.pilotId }) return null
        val race = current.copy(pilots = current.pilots.map { p ->
            if (p.id == lap.pilotId) p.copy(laps = p.laps + (lap.lapNumber to lap.seconds)) else p
        })
        return snapshot.copy(races = snapshot.races.map { if (it.id == race.id) race else it }) to race
    }

    /**
     * A snapshot fetched just as laps arrive can miss them; re-apply recent streamed laps, but only onto the
     * race they were streamed for, so a lap from the last heat never lands on the next one.
     */
    fun replayLaps(snapshot: LiveSnapshot, laps: Collection<AppliedLap>): LiveSnapshot {
        val currentId = snapshot.current?.id ?: return snapshot
        return laps.filter { it.raceId == currentId }
            .fold(snapshot) { snap, applied -> applyLap(snap, applied.lap)?.first ?: snap }
    }

    fun feedItem(id: Long, race: LiveRace, lap: StreamEvent.Lap, clockAt: (Long) -> Double?): LapFeedItem? {
        val pilot = race.pilots.firstOrNull { it.id == lap.pilotId } ?: return null
        val racing = lap.lapNumber >= 1
        val pilotBest = pilot.laps.filterKeys { it >= 1 }.values.minOrNull()
        val raceBest = LiveMath.fastestLap(race)
        return LapFeedItem(
            id = id,
            pilotName = pilot.name,
            color = pilot.color,
            lapNumber = lap.lapNumber,
            seconds = lap.seconds,
            raceClock = clockAt(race.startMs),
            isRaceBest = racing && raceBest != null && lap.seconds <= raceBest,
            isPersonalBest = racing && pilotBest != null && lap.seconds <= pilotBest && pilot.laps.count { it.key >= 1 } > 1
        )
    }
}
