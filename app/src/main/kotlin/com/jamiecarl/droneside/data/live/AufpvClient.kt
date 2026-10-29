package com.jamiecarl.droneside.data.live

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit

const val AUFPV_URL = "https://aufpv.org.au"

/** Talks to a club's AUFPV dronedash: an HTML snapshot plus a Server-Sent Events stream of laps. */
object AufpvClient {

    private const val USER_AGENT = "DroneSide-Android"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // The server pings every ~20 s, so a read timeout a bit past that doubles as the dead-stream watchdog.
    private val streamHttp = http.newBuilder()
        .readTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    @Volatile private var clubsCache: List<LiveClub>? = null

    /** AUFPV member clubs that run dronedash, plus AUFPV itself (the nationals live there). */
    suspend fun clubs(): List<LiveClub> = clubsCache ?: withContext(Dispatchers.IO) {
        val html = get("$AUFPV_URL/clubs")
        val clubs = listOf(LiveClub("AUFPV Nationals", AUFPV_URL, "$AUFPV_URL/static/images/aufpv/favicon-192.png")) +
            AufpvParser.parseClubs(html, "$AUFPV_URL/clubs")
        clubs.also { clubsCache = it }
    }

    /** With no [eventId] the site picks the club's current event. */
    suspend fun snapshot(baseUrl: String, eventId: Int?): LiveSnapshot = withContext(Dispatchers.IO) {
        val query = if (eventId != null) "event=$eventId&embed=1" else "embed=1"
        AufpvParser.parseSnapshot(get("$baseUrl/dronedash?$query"))
            ?: throw IOException("No dronedash on $baseUrl")
    }

    fun stream(baseUrl: String, eventId: Int): Flow<StreamEvent> = callbackFlow {
        val request = Request.Builder()
            .url("$baseUrl/dronedash/event/$eventId/stream")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/event-stream")
            .build()
        val source = EventSources.createFactory(streamHttp).newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                trySend(StreamEvent.Open)
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                parseEvent(type ?: "message", data)?.let { trySend(it) }
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(t ?: IOException("Stream failed (HTTP ${response?.code})"))
            }
        })
        awaitClose { source.cancel() }
    }.flowOn(Dispatchers.IO)

    internal fun parseEvent(type: String, data: String): StreamEvent? = when (type) {
        "ping" -> StreamEvent.Ping
        "sector" -> StreamEvent.Sector
        "lap" -> runCatching {
            val o = JsonParser.parseString(data).asJsonObject
            StreamEvent.Lap(o["pilot_id"].asInt, o["lap_number"].asInt, o["length_seconds"].asDouble)
        }.getOrNull()
        "race_state" -> runCatching {
            StreamEvent.RaceState(JsonParser.parseString(data).asJsonObject["state"]?.asString.orEmpty(), data)
        }.getOrNull()
        else -> StreamEvent.Other(type, data)
    }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
            return response.body?.string().orEmpty()
        }
    }
}
