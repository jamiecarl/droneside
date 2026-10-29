package com.jamiecarl.droneside.data.live

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.GZIPInputStream

class LiveRecorderTest {

    @get:Rule val tmp = TemporaryFolder()

    private var clock = 1_000L

    // Unconfined runs each write straight away, so the files can be checked without waiting.
    private fun recorder() = LiveRecorder(tmp.root, now = { clock }, scope = CoroutineScope(Dispatchers.Unconfined))

    private fun snapshot(raceId: Int, state: LiveRaceState) = LiveSnapshot(
        eventId = 11, eventName = "Meet", serverNowMs = 0, events = emptyList(), upcoming = emptyList(),
        races = listOf(
            LiveRace(
                id = raceId, label = "Current Race", title = "Race", state = state, startMs = 0,
                isTimeTrial = false, heatType = "Race", targetLaps = 3, consecWindow = 0, pilots = emptyList()
            )
        )
    )

    @Test
    fun `writes frames in the capture script's SSE layout`() {
        val r = recorder()
        r.note("connect event=11")
        clock = 1_005
        r.frame("lap", """{"pilot_id":1,"lap_number":2,"length_seconds":21.5}""")
        r.close()
        assertEquals(
            listOf(
                "1000 # connect event=11",
                "1005 event: lap",
                """1005 data: {"pilot_id":1,"lap_number":2,"length_seconds":21.5}""",
                "1005 "
            ),
            File(tmp.root, "stream.log").readLines()
        )
    }

    @Test
    fun `keeps a snapshot every 15 seconds, or straight away when the race changes`() {
        val r = recorder()
        r.snapshot("<html>1</html>", snapshot(1, LiveRaceState.PENDING))   // first: kept
        clock += 5_000
        r.snapshot("<html>2</html>", snapshot(1, LiveRaceState.PENDING))   // too soon, same race: skipped
        clock += 1_000
        r.snapshot("<html>3</html>", snapshot(1, LiveRaceState.RUNNING))   // race started: kept
        clock += 15_000
        r.snapshot("<html>4</html>", snapshot(1, LiveRaceState.RUNNING))   // 15 s on: kept

        val saved = File(tmp.root, "snapshots").listFiles()!!.sortedBy { it.name.substringBefore('.').toLong() }
        assertEquals(listOf("1000.html.gz", "7000.html.gz", "22000.html.gz"), saved.map { it.name })
        assertEquals("<html>3</html>", GZIPInputStream(saved[1].inputStream()).bufferedReader().readText())
    }
}
