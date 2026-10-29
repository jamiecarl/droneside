package com.jamiecarl.droneside.alerts

import com.jamiecarl.droneside.data.live.LiveMath
import com.jamiecarl.droneside.data.live.LivePilot
import com.jamiecarl.droneside.data.live.LiveRace
import com.jamiecarl.droneside.data.live.LiveRaceState
import com.jamiecarl.droneside.data.live.LiveSnapshot
import com.jamiecarl.droneside.data.live.Standing
import com.jamiecarl.droneside.data.live.UpcomingPilot
import com.jamiecarl.droneside.data.live.UpcomingRace

sealed interface RaceAlert {
    /** Stable per heat, so each alert fires once. */
    val key: String
    val eventName: String

    /** The pilot is in the heat after the one on the line: time to get the quad ready. */
    data class UpNext(
        override val key: String,
        override val eventName: String,
        val race: UpcomingRace,
        val me: UpcomingPilot,
        /** The heat being flown (or set up) before it. */
        val before: String?
    ) : RaceAlert

    /** The pilot's heat is the current race and hasn't started: get to the line. */
    data class OnTheLine(
        override val key: String,
        override val eventName: String,
        val race: LiveRace,
        val me: LivePilot
    ) : RaceAlert

    /** A heat the pilot flew has finished. */
    data class Result(
        override val key: String,
        override val eventName: String,
        val race: LiveRace,
        val me: Standing,
        val fieldSize: Int
    ) : RaceAlert
}

fun isCallsign(name: String, callsign: String): Boolean =
    name.trim().equals(callsign.trim(), ignoreCase = true)

/**
 * Turns successive dashboard snapshots into alerts for one pilot. [sent] carries keys already alerted (persisted
 * across service restarts) so a heat never rings twice.
 */
class RaceAlertDetector(private val callsign: String, sent: Set<String> = emptySet()) {

    private val sent = sent.toMutableSet()
    /** Heats we've seen as the current race, so a result is only reported for a heat flown while watching. */
    private val seenCurrent = mutableSetOf<Int>()

    val sentKeys: Set<String> get() = sent

    fun update(snapshot: LiveSnapshot): List<RaceAlert> {
        val alerts = mutableListOf<RaceAlert>()
        val current = snapshot.current

        if (current != null && current.state == LiveRaceState.PENDING) {
            current.pilots.firstOrNull { isCallsign(it.name, callsign) }?.let { me ->
                alerts += RaceAlert.OnTheLine("line:${current.id}", snapshot.eventName, current, me)
            }
        }

        snapshot.upcoming.firstOrNull()?.let { next ->
            next.pilots.firstOrNull { isCallsign(it.name, callsign) }?.let { me ->
                alerts += RaceAlert.UpNext(
                    "next:${snapshot.eventId}:${next.title}", snapshot.eventName, next, me,
                    before = current?.title?.takeIf { it.isNotEmpty() }
                )
            }
        }

        snapshot.races.filter { it.state == LiveRaceState.ENDED && it.id in seenCurrent }.forEach { race ->
            val standings = LiveMath.standings(race)
            standings.firstOrNull { isCallsign(it.pilot.name, callsign) }
                ?.takeIf { it.pilot.laps.isNotEmpty() }
                ?.let { me -> alerts += RaceAlert.Result("done:${race.id}", snapshot.eventName, race, me, standings.size) }
        }

        current?.let { seenCurrent += it.id }
        return alerts.filter { sent.add(it.key) }
    }
}
