package com.jamiecarl.droneside.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder

fun encodeArg(value: String): String = URLEncoder.encode(value, "UTF-8")
fun decodeArg(value: String?): String = URLDecoder.decode(value ?: "", "UTF-8")

object Routes {
    const val DASHBOARD = "dashboard"
    const val EXPLORE = "explore"
    const val CLUBS = "clubs"
    const val LIVE = "live"

    const val CLUB_DETAILS = "club/{clubId}"
    const val EVENT_DETAILS = "event/{eventId}?eventName={eventName}&clubName={clubName}"
    const val ROUND_DETAILS =
        "round/{eventId}/{roundId}?eventName={eventName}&roundNumber={roundNumber}&eventType={eventType}"
    const val RACE_DETAILS =
        "race/{eventId}/{roundId}/{raceId}?eventName={eventName}&roundNumber={roundNumber}" +
            "&raceNumber={raceNumber}&eventType={eventType}"
    const val PILOT_DETAILS = "pilot/{eventId}/{pilotId}?eventName={eventName}"

    fun clubDetails(clubId: String) = "club/${encodeArg(clubId)}"

    fun eventDetails(eventId: String, eventName: String, clubName: String) =
        "event/${encodeArg(eventId)}?eventName=${encodeArg(eventName)}&clubName=${encodeArg(clubName)}"

    fun roundDetails(eventId: String, roundId: String, eventName: String, roundNumber: String, eventType: String) =
        "round/${encodeArg(eventId)}/${encodeArg(roundId)}?eventName=${encodeArg(eventName)}" +
            "&roundNumber=${encodeArg(roundNumber)}&eventType=${encodeArg(eventType)}"

    fun raceDetails(
        eventId: String,
        roundId: String,
        raceId: String,
        eventName: String,
        roundNumber: String,
        raceNumber: Int,
        eventType: String
    ) = "race/${encodeArg(eventId)}/${encodeArg(roundId)}/${encodeArg(raceId)}" +
        "?eventName=${encodeArg(eventName)}&roundNumber=${encodeArg(roundNumber)}" +
        "&raceNumber=$raceNumber&eventType=${encodeArg(eventType)}"

    fun pilotDetails(eventId: String, pilotId: String, eventName: String) =
        "pilot/${encodeArg(eventId)}/${encodeArg(pilotId)}?eventName=${encodeArg(eventName)}"
}
