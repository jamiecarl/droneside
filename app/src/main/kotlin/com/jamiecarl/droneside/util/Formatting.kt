package com.jamiecarl.droneside.util

import com.jamiecarl.droneside.data.model.ResultSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Mirrors the original app's lenient "parse or pass through" lap/race time formatting. */
fun formatRaceTime(time: String?): String {
    if (time == null) return "-"
    val num = time.toDoubleOrNull() ?: return time
    return String.format(Locale.US, "%.2f", num)
}

// The API sends zone-less timestamps, sometimes with fractional seconds; parse() ignores the trailing
// fraction. They're read as device-local time, matching how the original app's `new Date()` read them.
private val apiDateFormats = listOf(
    "yyyy-MM-dd HH:mm:ss",
    "yyyy-MM-dd'T'HH:mm:ss"
)

fun parseApiDate(value: String?): Date? {
    if (value.isNullOrBlank()) return null
    for (pattern in apiDateFormats) {
        try {
            val format = SimpleDateFormat(pattern, Locale.US)
            return format.parse(value)
        } catch (_: Exception) {
            // try the next pattern
        }
    }
    return null
}

fun formatLongDate(value: String?): String {
    val date = parseApiDate(value) ?: return "-"
    val format = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    return format.format(date)
}

fun formatClockTime(value: String?): String {
    val date = parseApiDate(value) ?: return "Not available"
    val format = SimpleDateFormat("H:mm:ss", Locale.getDefault())
    return format.format(date)
}

fun sortResultsByPosition(results: List<ResultSummary>): List<ResultSummary> {
    return results.sortedWith(compareBy(nullsLast()) { it.position?.toIntOrNull() })
}

fun sortResultsByPbTime(results: List<ResultSummary>): List<ResultSummary> {
    val sorted = results.sortedBy { it.pbLapTime?.toDoubleOrNull() ?: 999.0 }
    sorted.forEachIndexed { index, result -> result.position = (index + 1).toString() }
    return sorted
}

enum class RaceStatus { COMPLETE, RUNNING, NOT_STARTED }

fun hasRaceStarted(start: String?): Boolean {
    val startDate = parseApiDate(start) ?: return false
    return startDate.time > 0
}

fun isRaceFinished(start: String?, end: String?): Boolean {
    val startDate = parseApiDate(start) ?: return false
    val endDate = parseApiDate(end) ?: return false
    return endDate.after(startDate)
}

fun raceStatus(start: String?, end: String?): RaceStatus = when {
    isRaceFinished(start, end) -> RaceStatus.COMPLETE
    hasRaceStarted(start) -> RaceStatus.RUNNING
    else -> RaceStatus.NOT_STARTED
}

/** "m:ss" duration between two timestamps, or null if either is missing/invalid. */
fun raceDuration(start: String?, end: String?): String? {
    val startDate = parseApiDate(start) ?: return null
    val endDate = parseApiDate(end) ?: return null
    if (!endDate.after(startDate)) return null
    val totalSeconds = (endDate.time - startDate.time) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
