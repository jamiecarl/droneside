package com.jamiecarl.droneside.data.live

/**
 * Live timing from AUFPV's "dronedash". Every AUFPV club site (cmrc.aufpv.org.au, fpvrebels.com, …) runs the
 * same dashboard against one shared database, so event/race/pilot ids are AUFPV-wide integers. None of them
 * match FPVTrackside's GUIDs.
 */
data class LiveClub(val name: String, val baseUrl: String, val logoUrl: String?) {
    /** "cmrc" for https://cmrc.aufpv.org.au, "fpvrebels" for https://fpvrebels.com. */
    val slug: String get() = baseUrl.substringAfter("://").removePrefix("www.").substringBefore('.')
}

private fun normalise(name: String) = name.lowercase().filter { it.isLetterOrDigit() }

/**
 * Finds the AUFPV club for an FPVTrackside club. Names usually match exactly ("Canberra Multirotor Racing
 * Club"); some FPVTrackside clubs use the short name, which is the AUFPV site's subdomain ("CMRC").
 */
fun List<LiveClub>.matchTracksideClub(tracksideName: String?): LiveClub? {
    val wanted = normalise(tracksideName ?: return null)
    if (wanted.isEmpty()) return null
    return firstOrNull { normalise(it.name) == wanted } ?: firstOrNull { it.slug == wanted }
}

data class LiveEventRef(val id: Int, val name: String, val live: Boolean)

enum class LiveRaceState { PENDING, RUNNING, ENDED;
    companion object {
        fun parse(value: String?): LiveRaceState = when (value?.lowercase()) {
            "running" -> RUNNING
            "ended" -> ENDED
            else -> PENDING
        }
    }
}

data class LivePilot(
    val id: Int,
    val name: String,
    val channel: String,
    /** "#RRGGBB" from the channel square, or null. */
    val color: String?,
    /** Lap number to seconds; 0 is the holeshot. Keyed so a lap re-sent after a reconnect can't double up. */
    val laps: Map<Int, Double> = emptyMap(),
    /** Best-N-consecutive for the whole meeting (time trials only). */
    val personalBest: Double? = null,
    val qualifying: String? = null
)

data class LiveRace(
    val id: Int,
    /** The card's heading on the site: "Current Race", "Last Race", "Previous Race". */
    val label: String,
    /** e.g. "Round 2 — Time Trial 1". */
    val title: String,
    val state: LiveRaceState,
    /** Race start in the server's clock (epoch ms), 0 before the start. */
    val startMs: Long,
    val isTimeTrial: Boolean,
    val heatType: String,
    val targetLaps: Int,
    val consecWindow: Int,
    val pilots: List<LivePilot>,
    /** "Round 31 · Race 1" from the site's FPVTrackside hint, when present. */
    val tracksideRef: String? = null
) {
    val isCurrent: Boolean get() = label.startsWith("Current", ignoreCase = true)
}

data class UpcomingPilot(val id: Int, val name: String, val channel: String, val color: String?)

data class UpcomingRace(val title: String, val pilots: List<UpcomingPilot>)

data class LiveSnapshot(
    val eventId: Int,
    val eventName: String,
    /** The server's clock when it rendered the page; used to cancel out this device's clock skew. */
    val serverNowMs: Long,
    val events: List<LiveEventRef>,
    val races: List<LiveRace>,
    val upcoming: List<UpcomingRace>
) {
    val current: LiveRace? get() = races.firstOrNull { it.isCurrent }
    val finished: List<LiveRace> get() = races.filter { !it.isCurrent }
}

/** One frame off the `/dronedash/event/{id}/stream` Server-Sent Events feed. */
sealed interface StreamEvent {
    data object Open : StreamEvent
    data object Ping : StreamEvent
    data class Lap(val pilotId: Int, val lapNumber: Int, val seconds: Double) : StreamEvent
    data class RaceState(val state: String, val raw: String) : StreamEvent
    data object Sector : StreamEvent
    data class Other(val type: String, val data: String) : StreamEvent
}
