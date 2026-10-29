package com.jamiecarl.droneside.data.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveMathTest {

    private fun pilot(id: Int, vararg laps: Pair<Int, Double>) =
        LivePilot(id = id, name = "P$id", channel = "R$id", color = null, laps = laps.toMap())

    private fun race(
        vararg pilots: LivePilot,
        state: LiveRaceState = LiveRaceState.RUNNING,
        target: Int = 3,
        timeTrial: Boolean = false,
        window: Int = 0
    ) = LiveRace(
        id = 1, label = "Current Race", title = "Round 1 — Race 1", state = state, startMs = 1L,
        isTimeTrial = timeTrial, heatType = if (timeTrial) "Time Trial" else "Race", targetLaps = target,
        consecWindow = window, pilots = pilots.toList()
    )

    @Test
    fun `orders a running heat by laps done, then by who got there first`() {
        val r = race(
            pilot(1, 0 to 5.0, 1 to 20.0),               // lap 1 at 25.0
            pilot(2, 0 to 4.0, 1 to 20.0, 2 to 21.0),    // lap 2 at 45.0
            pilot(3),                                     // no holeshot yet
            pilot(4, 0 to 5.5, 1 to 19.0)                 // lap 1 at 24.5
        )
        val s = LiveMath.standings(r)
        assertEquals(listOf(2, 4, 1, 3), s.map { it.pilot.id })
        assertEquals(listOf(1, 2, 3, null), s.map { it.position })
        assertTrue(s[0].isLeader)
    }

    @Test
    fun `gaps compare at the last lap both pilots completed`() {
        val s = LiveMath.standings(race(
            pilot(1, 0 to 5.0, 1 to 20.0, 2 to 20.0),   // 25, 45
            pilot(2, 0 to 5.2, 1 to 20.1),              // 25.3
            pilot(3, 0 to 5.0, 1 to 21.0)               // 26.0
        ))
        assertEquals(listOf(1, 2, 3), s.map { it.pilot.id })
        assertNull(s[0].gapToLeader)
        assertEquals(0.3, s[1].gapToLeader!!, 1e-9)       // leader at lap 1 was 25.0
        assertNull(s[1].gapToAhead)                        // the pilot ahead of 2nd is the leader
        assertEquals(1.0, s[2].gapToLeader!!, 1e-9)
        assertEquals(0.7, s[2].gapToAhead!!, 1e-9)
    }

    @Test
    fun `laps past the target don't count`() {
        val s = LiveMath.standings(race(pilot(1, 0 to 5.0, 1 to 20.0, 2 to 20.0, 3 to 20.0, 4 to 30.0), target = 3))
        assertEquals(3, s[0].lapsDone)
        assertEquals(65.0, s[0].total!!, 1e-9)
    }

    @Test
    fun `a finished heat keeps the server's order`() {
        val s = LiveMath.standings(race(
            pilot(1, 0 to 5.0, 1 to 30.0),
            pilot(2, 0 to 5.0, 1 to 20.0),
            state = LiveRaceState.ENDED
        ))
        assertEquals(listOf(1, 2), s.map { it.pilot.id })
        assertEquals(listOf(2, 1), s.map { it.position })
    }

    @Test
    fun `time trials rank by best consecutive laps`() {
        val s = LiveMath.standings(race(
            pilot(1, 0 to 3.0, 1 to 20.0, 2 to 20.0, 3 to 25.0),
            pilot(2, 0 to 3.0, 1 to 19.0, 2 to 19.5),
            pilot(3, 0 to 3.0, 1 to 18.0),
            timeTrial = true, window = 2, target = 0
        ))
        assertEquals(listOf(2, 1, 3), s.map { it.pilot.id })
        assertEquals(38.5, s[0].bestConsec!!, 1e-9)
        assertNull(s[2].bestConsec)
        assertNull(s[0].gapToLeader)
    }

    @Test
    fun `best consecutive needs enough laps`() {
        assertEquals(39.0, LiveMath.bestConsec(listOf(20.0, 19.0, 20.0, 25.0), 2)!!, 1e-9)
        assertNull(LiveMath.bestConsec(listOf(20.0), 2))
        assertNull(LiveMath.bestConsec(listOf(20.0), 0))
    }

    @Test
    fun `formats like the site`() {
        assertEquals("+0.84", LiveMath.formatGap(0.84))
        assertEquals("+1:02.50", LiveMath.formatGap(62.5))
        assertEquals("01:42.6", LiveMath.formatClock(102.61))
        assertEquals("00:00.0", LiveMath.formatClock(-3.0))
    }
}
