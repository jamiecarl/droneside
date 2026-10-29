package com.jamiecarl.droneside.alerts

import com.jamiecarl.droneside.data.live.AufpvParser
import com.jamiecarl.droneside.data.live.LivePilot
import com.jamiecarl.droneside.data.live.LiveRace
import com.jamiecarl.droneside.data.live.LiveRaceState
import com.jamiecarl.droneside.data.live.LiveSnapshot
import com.jamiecarl.droneside.data.live.UpcomingPilot
import com.jamiecarl.droneside.data.live.UpcomingRace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RaceAlertDetectorTest {

    private fun pilot(name: String, vararg laps: Pair<Int, Double>) =
        LivePilot(id = name.hashCode(), name = name, channel = "R1", color = "#FF0000", laps = laps.toMap())

    private fun race(id: Int, state: LiveRaceState, vararg pilots: LivePilot, label: String = "Current Race") = LiveRace(
        id = id, label = label, title = "Heat $id", state = state, startMs = 0, isTimeTrial = false,
        heatType = "Race", targetLaps = 3, consecWindow = 0, pilots = pilots.toList()
    )

    private fun snap(races: List<LiveRace>, upcoming: List<UpcomingRace> = emptyList()) = LiveSnapshot(
        eventId = 11, eventName = "Meet 9", serverNowMs = 0, events = emptyList(), races = races, upcoming = upcoming
    )

    private fun next(title: String, vararg names: String) =
        UpcomingRace(title, names.mapIndexed { i, n -> UpcomingPilot(i, n, "R$i", "#00FF00") })

    @Test
    fun `walks a pilot through up next, on the line and their result`() {
        val d = RaceAlertDetector("Wedge FPV")

        // Heat 1 on the line, we're in heat 2.
        val a1 = d.update(snap(listOf(race(1, LiveRaceState.PENDING, pilot("Other"))), listOf(next("Heat 2", "wedge fpv", "X"))))
        assertEquals(1, a1.size)
        val upNext = a1.single() as RaceAlert.UpNext
        assertEquals("Heat 2", upNext.race.title)
        assertEquals("Heat 1", upNext.before)

        // Same state again: nothing new.
        assertTrue(d.update(snap(listOf(race(1, LiveRaceState.RUNNING, pilot("Other"))), listOf(next("Heat 2", "Wedge FPV")))).isEmpty())

        // Our heat is set up.
        val a2 = d.update(snap(listOf(race(2, LiveRaceState.PENDING, pilot("Wedge FPV"), pilot("X")))))
        assertTrue(a2.single() is RaceAlert.OnTheLine)

        // Racing: no alert. Then it ends and becomes the Last Race.
        assertTrue(d.update(snap(listOf(race(2, LiveRaceState.RUNNING, pilot("Wedge FPV"))))).isEmpty())
        val ended = race(2, LiveRaceState.ENDED, pilot("Wedge FPV", 0 to 5.0, 1 to 20.0), pilot("X", 0 to 6.0), label = "Last Race")
        val a3 = d.update(snap(listOf(race(3, LiveRaceState.PENDING, pilot("Y")), ended)))
        val result = a3.single() as RaceAlert.Result
        assertEquals(1, result.me.position)
        assertEquals(2, result.fieldSize)
    }

    @Test
    fun `no result for a heat that had already finished before watching`() {
        val d = RaceAlertDetector("Wedge FPV")
        val old = race(7, LiveRaceState.ENDED, pilot("Wedge FPV", 0 to 5.0, 1 to 20.0), label = "Last Race")
        assertTrue(d.update(snap(listOf(race(8, LiveRaceState.PENDING, pilot("Y")), old))).isEmpty())
    }

    @Test
    fun `alerts already sent before a restart stay quiet`() {
        val d = RaceAlertDetector("Wedge FPV", sent = setOf("line:2"))
        assertTrue(d.update(snap(listOf(race(2, LiveRaceState.PENDING, pilot("Wedge FPV"))))).isEmpty())
    }

    @Test
    fun `finds a pilot in a real CMRC line-up`() {
        val html = javaClass.getResource("/aufpv/cmrc-event11-tt-pending.html")!!.readText()
        val snapshot = AufpvParser.parseSnapshot(html)!!
        // Wedge FPV is in the pending current race; Has0 opens the next one.
        assertTrue(RaceAlertDetector("Wedge FPV").update(snapshot).single() is RaceAlert.OnTheLine)
        val up = RaceAlertDetector("HAS0").update(snapshot).single() as RaceAlert.UpNext
        assertEquals("R8", up.me.channel)
        assertEquals("Round 2 — Time Trial 1", up.before)
    }
}
