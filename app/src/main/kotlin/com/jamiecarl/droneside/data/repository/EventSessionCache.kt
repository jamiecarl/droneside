package com.jamiecarl.droneside.data.repository

import com.jamiecarl.droneside.data.model.Channel
import com.jamiecarl.droneside.data.model.Pilot

/**
 * Small in-memory cache keyed by event id, so that navigating between the rounds/races/pilot
 * screens within one event doesn't re-fetch the pilot roster and channel colors on every hop —
 * mirroring how the original app threaded these down as navigation props.
 */
object EventSessionCache {
    private val pilotsByEvent = mutableMapOf<String, List<Pilot>>()
    private val channelsByEvent = mutableMapOf<String, List<Channel>>()

    fun putPilots(eventId: String, pilots: List<Pilot>) {
        pilotsByEvent[eventId] = pilots
    }

    fun getPilots(eventId: String): List<Pilot>? = pilotsByEvent[eventId]

    fun putChannels(eventId: String, channels: List<Channel>) {
        channelsByEvent[eventId] = channels
    }

    fun getChannels(eventId: String): List<Channel>? = channelsByEvent[eventId]
}
