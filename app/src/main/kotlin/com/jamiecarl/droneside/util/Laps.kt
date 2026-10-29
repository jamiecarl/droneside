package com.jamiecarl.droneside.util

import com.jamiecarl.droneside.data.model.Lap
import com.jamiecarl.droneside.data.model.RawRaceData

/** A pilot's valid laps in a race, matched via their lap-ending detections. */
fun lapsForPilot(raw: RawRaceData?, pilotId: String): List<Lap> {
    if (raw == null) return emptyList()
    val detections = raw.detections ?: return emptyList()
    val laps = raw.laps ?: return emptyList()
    val detectionIds = detections
        .filter { it.pilot == pilotId && it.valid != false && it.lapNumber > 0 }
        .map { it.id }
        .toSet()
    return laps.filter { it.detection in detectionIds }.sortedBy { it.lapNumber }
}
