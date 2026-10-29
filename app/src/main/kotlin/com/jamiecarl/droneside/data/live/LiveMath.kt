package com.jamiecarl.droneside.data.live

import java.util.Locale

data class Standing(
    val pilot: LivePilot,
    /** Null until the pilot has a holeshot. */
    val position: Int?,
    /** Racing laps completed (holeshot excluded; laps past the target don't count in a race). */
    val lapsDone: Int,
    val lastLap: Double?,
    val bestLap: Double?,
    val isLeader: Boolean = false,
    val gapToLeader: Double? = null,
    val gapToAhead: Double? = null,
    /** Best N consecutive laps this heat (time trials). */
    val bestConsec: Double? = null,
    /** Holeshot plus counted laps. */
    val total: Double? = null
)

/**
 * Ports dronedash-home.js so the app shows what the site shows: a race is ordered by most laps, then whoever
 * completed them soonest; gaps compare pilots at the last lap *both* have completed; time trials rank by best
 * N consecutive laps and get no gaps (their starts are staggered).
 */
object LiveMath {

    fun standings(race: LiveRace): List<Standing> =
        if (race.isTimeTrial || !race.heatType.equals("Race", ignoreCase = true)) timeTrial(race) else heat(race)

    private fun racingLaps(pilot: LivePilot, target: Int): List<Double> {
        val laps = mutableListOf<Double>()
        var n = 1
        while (true) {
            val lap = pilot.laps[n] ?: break
            if (target > 0 && n > target) break
            laps += lap
            n++
        }
        return laps
    }

    private fun heat(race: LiveRace): List<Standing> {
        class Run(val pilot: LivePilot, val laps: List<Double>, val cum: List<Double>) {
            val n get() = cum.size - 1
        }

        val runs = race.pilots.map { p ->
            val laps = racingLaps(p, race.targetLaps)
            val hs = p.laps[0]
            val cum = if (hs == null) emptyList() else laps.runningFold(hs) { acc, lap -> acc + lap }
            Run(p, laps, cum)
        }
        val live = runs.filter { it.n >= 0 }.sortedWith(compareBy<Run>({ -it.n }, { it.cum[it.n] }))
        val waiting = runs.filter { it.n < 0 }
        // While racing, re-rank as laps land (the site only re-renders order on a race-state edge). Otherwise
        // the server's order stands; it has the final say on DNFs and penalties.
        val ordered = if (race.state == LiveRaceState.RUNNING) live + waiting else runs

        return ordered.map { r ->
            val i = live.indexOf(r)
            val lead = live.firstOrNull()
            Standing(
                pilot = r.pilot,
                position = if (i >= 0) i + 1 else null,
                lapsDone = r.laps.size,
                lastLap = r.laps.lastOrNull(),
                bestLap = r.laps.minOrNull(),
                isLeader = i == 0 && live.size > 1,
                gapToLeader = if (i > 0 && lead != null) (r.cum[r.n] - lead.cum[r.n]).coerceAtLeast(0.0) else null,
                gapToAhead = if (i > 1) (r.cum[r.n] - live[i - 1].cum[r.n]).coerceAtLeast(0.0) else null,
                total = r.cum.lastOrNull()
            )
        }
    }

    private fun timeTrial(race: LiveRace): List<Standing> {
        val rows = race.pilots.map { p ->
            val laps = racingLaps(p, 0)
            Standing(
                pilot = p,
                position = null,
                lapsDone = laps.size,
                lastLap = laps.lastOrNull(),
                bestLap = laps.minOrNull(),
                bestConsec = bestConsec(laps, race.consecWindow),
                total = p.laps[0]?.let { hs -> hs + laps.sum() }
            )
        }
        val ordered = if (race.state == LiveRaceState.RUNNING) {
            rows.filter { it.bestConsec != null }.sortedBy { it.bestConsec } + rows.filter { it.bestConsec == null }
        } else rows
        return ordered.mapIndexed { i, s -> s.copy(position = i + 1) }
    }

    /** Lowest sum of [window] consecutive laps, or null if there aren't enough laps yet. */
    fun bestConsec(laps: List<Double>, window: Int): Double? {
        if (window < 1 || laps.size < window) return null
        return (0..laps.size - window).minOf { start -> laps.subList(start, start + window).sum() }
    }

    fun fastestLap(race: LiveRace): Double? =
        race.pilots.flatMap { p -> p.laps.filterKeys { it >= 1 }.values }.minOrNull()

    fun formatGap(seconds: Double): String {
        if (seconds < 60) return "+" + String.format(Locale.US, "%.2f", seconds)
        val m = (seconds / 60).toInt()
        return "+" + m + ":" + String.format(Locale.US, "%05.2f", seconds - m * 60)
    }

    fun formatLap(seconds: Double): String = String.format(Locale.US, "%.2f", seconds)

    fun formatClock(seconds: Double): String {
        val s = seconds.coerceAtLeast(0.0)
        val m = (s / 60).toInt()
        return String.format(Locale.US, "%02d:%04.1f", m, s - m * 60)
    }
}
