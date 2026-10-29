package com.jamiecarl.droneside.data.network

import com.jamiecarl.droneside.data.model.Channel
import com.jamiecarl.droneside.data.model.Club
import com.jamiecarl.droneside.data.model.Event
import com.jamiecarl.droneside.data.model.EventDetailResponse
import com.jamiecarl.droneside.data.model.Pilot
import com.jamiecarl.droneside.data.model.RaceDetail
import com.jamiecarl.droneside.data.model.RaceSummary
import com.jamiecarl.droneside.data.model.RawRaceData
import com.jamiecarl.droneside.data.model.Round
import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path

interface FpvTracksideApi {

    @GET("events")
    suspend fun getEvents(): List<Event>

    @GET("channels")
    suspend fun getChannels(): List<Channel>

    @GET("clubs")
    suspend fun getClubs(): List<Club>

    @GET("clubs/id/{clubId}")
    suspend fun getClub(@Path("clubId") clubId: String): Club

    @GET("clubs/id/{clubId}/full")
    suspend fun getClubEventsRaw(@Path("clubId") clubId: String): JsonElement

    @GET("events/id/{eventId}")
    suspend fun getEventDetail(@Path("eventId") eventId: String): EventDetailResponse

    @GET("rounds/eventId/{eventId}")
    suspend fun getRounds(@Path("eventId") eventId: String): List<Round>

    @GET("pilots/eventId/{eventId}")
    suspend fun getPilots(@Path("eventId") eventId: String): List<Pilot>

    @GET("raceSummaries/eventId/{eventId}")
    suspend fun getRaceSummaries(@Path("eventId") eventId: String): List<RaceSummary>

    @GET("races/eventId/{eventId}/roundId/{roundId}")
    suspend fun getRacesForRound(
        @Path("eventId") eventId: String,
        @Path("roundId") roundId: String
    ): List<RaceDetail>

    @GET("races/eventId/{eventId}/pilotId/{pilotId}")
    suspend fun getRacesForPilot(
        @Path("eventId") eventId: String,
        @Path("pilotId") pilotId: String
    ): List<RaceDetail>

    @GET("races/eventId/{eventId}/raceId/{raceId}/raw/1")
    suspend fun getRawRace(
        @Path("eventId") eventId: String,
        @Path("raceId") raceId: String
    ): RawRaceData
}
