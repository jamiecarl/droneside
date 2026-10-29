package com.jamiecarl.droneside.data.live

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveSessionTest {

    private fun snapshot(raceId: Int, vararg pilots: LivePilot, state: LiveRaceState = LiveRaceState.RUNNING) = LiveSnapshot(
        eventId = 11, eventName = "Meet", serverNowMs = 0, events = emptyList(), upcoming = emptyList(),
        races = listOf(
            LiveRace(
                id = raceId, label = "Current Race", title = "Race $raceId", state = state, startMs = 1_000,
                isTimeTrial = false, heatType = "Race", targetLaps = 3, consecWindow = 0, pilots = pilots.toList()
            )
        )
    )

    private fun pilot(id: Int, vararg laps: Pair<Int, Double>) =
        LivePilot(id = id, name = "P$id", channel = "R$id", color = "#FF0000", laps = laps.toMap())

    @Test
    fun `merges streamed laps with snapshots the way the site does`() = runTest {
        var served = snapshot(10, pilot(1, 0 to 5.0), pilot(2, 0 to 5.5))
        var fetches = 0
        val stream = MutableSharedFlow<StreamEvent>()
        val session = LiveSession(
            baseUrl = "https://example.test",
            pinnedEventId = null,
            fetchSnapshot = { _, _ -> fetches++; served },
            openStream = { _, _ -> stream },
            now = { testScheduler.currentTime }
        )
        backgroundScope.launch { session.run() }

        advanceTimeBy(300); runCurrent()
        assertEquals(1, fetches)
        assertEquals(10, session.state.value.snapshot?.current?.id)

        stream.emit(StreamEvent.Open)
        advanceTimeBy(300); runCurrent()
        assertEquals(LiveConnection.LIVE, session.state.value.connection)
        assertEquals(2, fetches)

        // A lap lands straight on the card, and again idempotently if the stream re-sends it.
        stream.emit(StreamEvent.Lap(1, 1, 20.0))
        stream.emit(StreamEvent.Lap(1, 1, 20.0))
        runCurrent()
        assertEquals(20.0, session.state.value.snapshot!!.current!!.pilots.first { it.id == 1 }.laps[1]!!, 1e-9)
        assertEquals(2, session.state.value.feed.size)

        // A snapshot rendered just before that lap must not wipe it.
        stream.emit(StreamEvent.RaceState("running", """{"state":"running"}"""))
        advanceTimeBy(300); runCurrent()
        assertEquals(3, fetches)
        assertEquals(20.0, session.state.value.snapshot!!.current!!.pilots.first { it.id == 1 }.laps[1]!!, 1e-9)

        // Next heat with the same pilot: the old lap stays with the old race.
        served = snapshot(11, pilot(1), pilot(3))
        stream.emit(StreamEvent.RaceState("created", "{}"))
        advanceTimeBy(300); runCurrent()
        assertEquals(11, session.state.value.snapshot?.current?.id)
        assertNull(session.state.value.snapshot!!.current!!.pilots.first { it.id == 1 }.laps[1])

        // A lap for someone not on the card means our card is stale: re-fetch.
        stream.emit(StreamEvent.Lap(99, 1, 20.0))
        advanceTimeBy(300); runCurrent()
        assertEquals(5, fetches)
    }

    @Test
    fun `re-syncs on a timer even when the stream is quiet`() = runTest {
        var fetches = 0
        val session = LiveSession(
            baseUrl = "https://example.test",
            pinnedEventId = 11,
            fetchSnapshot = { _, _ -> fetches++; snapshot(10, pilot(1)) },
            openStream = { _, _ -> MutableSharedFlow() },
            now = { testScheduler.currentTime }
        )
        backgroundScope.launch { session.run() }
        advanceTimeBy(300); runCurrent()
        assertEquals(1, fetches)
        // Running race: every 5 s (checked on a 2.5 s tick).
        advanceTimeBy(10_500); runCurrent()
        assertTrue("expected 2–3 backstop fetches, got ${fetches - 1}", fetches - 1 in 2..3)
    }

    @Test
    fun `stream frames decode`() {
        assertEquals(StreamEvent.Lap(133, 2, 21.25), AufpvClient.parseEvent("lap", """{"pilot_id":133,"lap_number":2,"length_seconds":21.25}"""))
        assertEquals("ended", (AufpvClient.parseEvent("race_state", """{"state":"ended","race_id":5}""") as StreamEvent.RaceState).state)
        assertEquals(StreamEvent.Ping, AufpvClient.parseEvent("ping", "{}"))
        assertNull(AufpvClient.parseEvent("lap", "not json"))
    }
}
