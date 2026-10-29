package com.jamiecarl.droneside.data.model

import com.google.gson.annotations.SerializedName

const val API_BASE_URL = "https://fpvtrackside.com/api/public/"
const val SITE_BASE_URL = "https://fpvtrackside.com"
const val DEFAULT_CLUB_LOGO_URL = "https://fpvtrackside.com/assets/defaultclub-iJLq4-Em.png"

/** Resolves a possibly-relative image path returned by the API into an absolute URL. */
fun resolveImageUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    return if (path.startsWith("http")) path else "$SITE_BASE_URL$path"
}

data class Channel(
    @SerializedName("ID") val id: String,
    @SerializedName("Number") val number: Int,
    @SerializedName("Band") val band: String?,
    @SerializedName("ChannelPrefix") val channelPrefix: String?,
    @SerializedName("Frequency") val frequency: Int,
    @SerializedName("ShortBand") val shortBand: String?,
    @SerializedName("Color") var color: String? = null
) {
    val displayName: String get() = "${shortBand.orEmpty()}$number"
}

data class Club(
    @SerializedName("ID") val id: String,
    @SerializedName("Creation") val creation: String?,
    @SerializedName("Name") val name: String,
    @SerializedName("LogoUrl") val logoUrl: String?,
    @SerializedName("BannerUrl") val bannerUrl: String?,
    @SerializedName("Address") val address: String?,
    @SerializedName("Latitude") val latitude: Double?,
    @SerializedName("Longitude") val longitude: Double?,
    @SerializedName("Timezone") val timezone: String?
)

data class Event(
    @SerializedName("ID") val id: String,
    @SerializedName("Start") val start: String,
    @SerializedName("End") val end: String,
    @SerializedName("Name") val name: String,
    @SerializedName("PilotCount") val pilotCount: Int,
    @SerializedName("Club") val club: EventClub,
    @SerializedName("BannerUrl") val bannerUrl: String?
)

/** The nested club summary embedded in event responses — fewer fields than the full Club. */
data class EventClub(
    @SerializedName("ID") val id: String,
    @SerializedName("Name") val name: String?,
    @SerializedName("LogoUrl") val logoUrl: String?
)

data class Round(
    @SerializedName("ID") val id: String,
    @SerializedName("RoundNumber") val roundNumber: String,
    @SerializedName("Name") val name: String?,
    @SerializedName("Event") val event: String,
    @SerializedName("EventType") val eventType: String
)

data class Pilot(
    @SerializedName("ID") val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("FirstName") val firstName: String?,
    @SerializedName("LastName") val lastName: String?,
    @SerializedName("PhotoURL") val photoUrl: String?,
    @SerializedName("CatchPhrase") val catchPhrase: String?
)

data class PilotChannel(
    @SerializedName("ID") val id: String,
    @SerializedName("Race") val race: String,
    @SerializedName("Channel") val channel: String,
    @SerializedName("Pilot") val pilot: String
)

data class ResultSummary(
    @SerializedName("ID") val id: String,
    @SerializedName("Pilot") val pilot: String,
    @SerializedName("Race") val race: String,
    @SerializedName("HoleshotTime") val holeshotTime: String?,
    @SerializedName("PbLapTime") val pbLapTime: String?,
    @SerializedName("LapCount") val lapCount: String?,
    @SerializedName("RaceTime") val raceTime: String?,
    @SerializedName("Points") var points: String?,
    @SerializedName("Position") var position: String?
) {
    var pilotName: String = "Unknown Pilot"
    var resolvedChannel: Channel? = null
}

data class RaceDetail(
    @SerializedName("ID") val id: String,
    @SerializedName("Start") val start: String?,
    @SerializedName("End") val end: String?,
    @SerializedName("Event") val event: String,
    @SerializedName("Round") val round: String,
    @SerializedName("RaceNumber") val raceNumber: Int,
    @SerializedName("TargetLaps") val targetLaps: String?,
    @SerializedName("PilotChannels") val pilotChannels: List<PilotChannel>,
    @SerializedName("ResultSummaries") var resultSummaries: List<ResultSummary>
)

data class RawRaceData(
    @SerializedName("ID") val id: String,
    @SerializedName("RaceNumber") val raceNumber: Int,
    @SerializedName("Round") val round: String,
    @SerializedName("Event") val event: String,
    @SerializedName("Start") val start: String?,
    @SerializedName("End") val end: String?,
    @SerializedName("Detections") val detections: List<Detection>?,
    @SerializedName("Laps") val laps: List<Lap>?
)

data class Lap(
    @SerializedName("Detection") val detection: String,
    @SerializedName("LengthSeconds") val lengthSeconds: Double,
    @SerializedName("LapNumber") val lapNumber: Int,
    @SerializedName("ID") val id: String
)

data class Detection(
    @SerializedName("Channel") val channel: String?,
    @SerializedName("Pilot") val pilot: String,
    @SerializedName("LapNumber") val lapNumber: Int,
    @SerializedName("Valid") val valid: Boolean?,
    @SerializedName("ID") val id: String
)

/** Aggregate per-pilot lap summary from the raceSummaries endpoint — snake_case on the wire. */
data class RaceSummary(
    @SerializedName("pilot_id") val pilotId: String,
    @SerializedName("pb_lap_time") val pbLapTime: Double?
)

/** The nested per-event channel color override, keyed by channel id. */
data class EventChannelColor(
    @SerializedName("Channel") val channel: String,
    @SerializedName("Color") val color: String?
)

data class EventDetailResponse(
    @SerializedName("Channels") val channels: List<EventChannelColor>?
)
