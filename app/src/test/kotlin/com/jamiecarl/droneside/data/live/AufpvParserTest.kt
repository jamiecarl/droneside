package com.jamiecarl.droneside.data.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fixtures are real pages captured from AUFPV sites on 2026-10-29. */
class AufpvParserTest {

    private fun fixture(name: String) =
        requireNotNull(javaClass.getResource("/aufpv/$name")) { "missing fixture $name" }.readText()

    @Test
    fun `club directory keeps only clubs with a dronedash site`() {
        val clubs = AufpvParser.parseClubs(fixture("clubs.html"), "https://aufpv.org.au/clubs")
        val cmrc = clubs.single { it.name == "Canberra Multirotor Racing Club" }
        assertEquals("https://cmrc.aufpv.org.au", cmrc.baseUrl)
        assertEquals("cmrc", cmrc.slug)
        assertTrue(cmrc.logoUrl!!.startsWith("https://aufpv.org.au/static/"))
        assertTrue(clubs.any { it.baseUrl == "https://fpvrebels.com" })
        assertTrue(clubs.none { it.name == "ADF Drone Racing" })
    }

    @Test
    fun `matches FPVTrackside club names to AUFPV clubs`() {
        val clubs = AufpvParser.parseClubs(fixture("clubs.html"), "https://aufpv.org.au/clubs")
        assertEquals("https://cmrc.aufpv.org.au", clubs.matchTracksideClub("Canberra Multirotor Racing Club")?.baseUrl)
        assertEquals("https://cmrc.aufpv.org.au", clubs.matchTracksideClub("CMRC")?.baseUrl)
        assertEquals("https://fpvrebels.com", clubs.matchTracksideClub("FPV Rebels")?.baseUrl)
        assertNull(clubs.matchTracksideClub("Utah Drone Racing"))
        assertNull(clubs.matchTracksideClub(""))
    }

    @Test
    fun `parses a pending time trial with its line-up and upcoming heats`() {
        val snap = AufpvParser.parseSnapshot(fixture("cmrc-event11-tt-pending.html"))!!
        assertEquals(11, snap.eventId)
        assertEquals("CMRC 2026 Racing Series Meet 6", snap.eventName)
        assertEquals(1793247286000L, snap.serverNowMs)
        assertTrue(snap.events.any { it.id == 22 && it.name.startsWith("CMRC 2026 Racing Series Meet 5") })

        val current = snap.current!!
        assertEquals(3593, current.id)
        assertEquals("Round 2 — Time Trial 1", current.title)
        assertEquals(LiveRaceState.PENDING, current.state)
        assertEquals(0L, current.startMs)
        assertTrue(current.isTimeTrial)
        assertEquals(3, current.consecWindow)

        val wedge = current.pilots.first()
        assertEquals(133, wedge.id)
        assertEquals("Wedge FPV", wedge.name)
        assertEquals("F2", wedge.channel)
        assertEquals("#32F000", wedge.color)
        assertEquals(54.675, wedge.personalBest!!, 1e-9)
        assertEquals("Q18", wedge.qualifying)
        assertTrue(wedge.laps.isEmpty())

        val next = snap.upcoming.first()
        assertEquals("Round 2 — Time Trial 2", next.title)
        assertEquals(7, next.pilots.size)
        assertEquals(UpcomingPilot(117, "Has0", "R8", "#FFA800"), next.pilots.first())
    }

    @Test
    fun `parses finished races with holeshot, laps and the FPVTrackside hint`() {
        val snap = AufpvParser.parseSnapshot(fixture("nationals-event699-ended.html"))!!
        assertEquals(699, snap.eventId)
        assertNull(snap.current)
        assertEquals(listOf(3334, 3333), snap.races.map { it.id })

        val last = snap.races.first()
        assertEquals("Last Race", last.label)
        assertEquals(LiveRaceState.ENDED, last.state)
        assertEquals(1791096385000L, last.startMs)
        assertEquals(3, last.targetLaps)
        assertEquals("Round 31 · Race 1", last.tracksideRef)

        val flowerpig = snap.races.flatMap { it.pilots }.first { it.id == 2035 }
        assertEquals("flowerpig", flowerpig.name)
        // Sector chips sit inside each lap pill; data-secs keeps the lap time exact.
        assertEquals(5.095, flowerpig.laps[0]!!, 1e-9)
        assertEquals(20.92, flowerpig.laps[1]!!, 1e-9)
        assertEquals(3, flowerpig.laps.keys.count { it >= 1 })
    }

    @Test
    fun `parses another club's dashboard the same way`() {
        val snap = AufpvParser.parseSnapshot(fixture("rebels-event698.html"))!!
        assertEquals(698, snap.eventId)
        assertEquals("Sept 2026", snap.eventName)
        assertEquals(2838, snap.current?.id)
    }

    @Test
    fun `returns null for a page without a dashboard`() {
        assertNull(AufpvParser.parseSnapshot("<html><body>Not found</body></html>"))
    }
}
