package com.jamiecarl.droneside.data.repository

import com.jamiecarl.droneside.data.model.Channel
import com.jamiecarl.droneside.data.model.Club
import com.jamiecarl.droneside.data.model.Event
import com.jamiecarl.droneside.data.model.Pilot
import com.jamiecarl.droneside.data.model.RaceDetail
import com.jamiecarl.droneside.data.model.RaceSummary
import com.jamiecarl.droneside.data.model.RawRaceData
import com.jamiecarl.droneside.data.model.Round
import com.jamiecarl.droneside.data.network.FpvTracksideApi
import com.jamiecarl.droneside.data.network.NetworkModule
import com.jamiecarl.droneside.util.sortResultsByPbTime
import com.jamiecarl.droneside.util.sortResultsByPosition
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class FpvTracksideRepository(private val api: FpvTracksideApi = NetworkModule.api) {

    suspend fun getEvents(): List<Event> =
        api.getEvents().sortedByDescending { it.start }

    suspend fun getChannels(): List<Channel> = api.getChannels()

    suspend fun getClubs(): List<Club> =
        api.getClubs().sortedBy { it.name.lowercase() }

    suspend fun getClub(clubId: String): Club = api.getClub(clubId)

    /**
     * The `/full` endpoint sometimes returns a bare array and sometimes an object with an
     * `events`/`Events` property — the original app handled both shapes defensively.
     */
    suspend fun getClubEvents(clubId: String): List<Event> {
        val element = api.getClubEventsRaw(clubId)
        val parentClub = if (element.isJsonObject) element.asJsonObject else null
        val array: JsonArray = when {
            element.isJsonArray -> element.asJsonArray
            parentClub != null ->
                (parentClub.firstArrayOrNull("events") ?: parentClub.firstArrayOrNull("Events")) ?: JsonArray()
            else -> JsonArray()
        }
        // Events nested under a club carry only the club's id as `Club`, so rebuild it from the parent.
        val clubJson = JsonObject().apply {
            addProperty("ID", clubId)
            parentClub?.get("Name")?.takeIf { it.isJsonPrimitive }?.let { add("Name", it) }
            parentClub?.get("LogoUrl")?.takeIf { it.isJsonPrimitive }?.let { add("LogoUrl", it) }
        }
        val gson = com.google.gson.Gson()
        val events = array.mapNotNull { item ->
            val obj = item.takeIf { it.isJsonObject }?.asJsonObject?.deepCopy() ?: return@mapNotNull null
            if (obj.get("Club")?.isJsonObject != true) obj.add("Club", clubJson)
            gson.fromJson(obj, Event::class.java)
        }
        return events.sortedByDescending { it.start }
    }

    private fun JsonObject.firstArrayOrNull(key: String): JsonArray? =
        if (has(key) && get(key).isJsonArray) getAsJsonArray(key) else null

    /** Base channel list, with per-event color overrides merged in where available. */
    suspend fun getChannelsForEvent(eventId: String): List<Channel> {
        val channels = api.getChannels()
        val detail = runCatching { api.getEventDetail(eventId) }.getOrNull()
        val colorsByChannelId = detail?.channels?.associate { it.channel to it.color }.orEmpty()
        channels.forEach { channel ->
            colorsByChannelId[channel.id]?.let { channel.color = it }
        }
        return channels
    }

    suspend fun getRounds(eventId: String): List<Round> = api.getRounds(eventId)

    suspend fun getPilots(eventId: String): List<Pilot> =
        api.getPilots(eventId).sortedBy { it.name.lowercase() }

    suspend fun getRaceSummaries(eventId: String): List<RaceSummary> = api.getRaceSummaries(eventId)

    /** Races for a round, with result summaries sorted/enriched with pilot name and channel color. */
    suspend fun getRacesForRound(
        eventId: String,
        roundId: String,
        eventType: String,
        pilots: List<Pilot>,
        channels: List<Channel>
    ): List<RaceDetail> {
        val races = api.getRacesForRound(eventId, roundId).sortedBy { it.raceNumber }
        races.forEach { race -> enrichResultSummaries(race, eventType, pilots, channels) }
        return races
    }

    suspend fun getRacesForPilot(eventId: String, pilotId: String): List<RaceDetail> =
        api.getRacesForPilot(eventId, pilotId)

    suspend fun getRawRace(eventId: String, raceId: String): RawRaceData = api.getRawRace(eventId, raceId)

    private fun enrichResultSummaries(
        race: RaceDetail,
        eventType: String,
        pilots: List<Pilot>,
        channels: List<Channel>
    ) {
        val sorted = if (eventType == "TimeTrial") {
            sortResultsByPbTime(race.resultSummaries)
        } else {
            sortResultsByPosition(race.resultSummaries)
        }
        sorted.forEach { result ->
            result.pilotName = pilots.find { it.id == result.pilot }?.name ?: "Unknown Pilot"
            val raceChannel = race.pilotChannels.find { it.pilot == result.pilot }
            result.resolvedChannel = raceChannel?.let { pc -> channels.find { it.id == pc.channel } }
        }
        race.resultSummaries = sorted
    }
}
