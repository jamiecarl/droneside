package com.jamiecarl.droneside.data.storage

import android.content.Context
import android.content.SharedPreferences

/** How race alerts look and sound. Defaults are the loudest, can't-miss-it setup. */
data class AlertPrefs(
    val upNext: Boolean = true,
    val onTheLine: Boolean = true,
    val results: Boolean = true,
    /** Content URI of the sound; null means the default alarm sound, [SILENT] means no sound. */
    val soundUri: String? = null,
    /** Play through the alarm stream, so it sounds even when the phone is on silent. */
    val alarmVolume: Boolean = true,
    /** Keep ringing until the alert is acknowledged. */
    val untilDismissed: Boolean = true,
    val vibrate: Boolean = true,
    /** Heads-up pop-up over whatever's on screen. */
    val popUp: Boolean = true
) {
    val silent: Boolean get() = soundUri == SILENT

    companion object {
        const val SILENT = "silent"
    }
}

/** The pilot's callsign, race-alert options, and the race-alert watch (which club site, if alerts are on). */
class RaceAlertStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("droneside_prefs", Context.MODE_PRIVATE)

    var callsign: String?
        get() = prefs.getString(KEY_CALLSIGN, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_CALLSIGN, value?.trim()).apply()

    var alertPrefs: AlertPrefs
        get() = AlertPrefs(
            upNext = prefs.getBoolean(KEY_UP_NEXT, true),
            onTheLine = prefs.getBoolean(KEY_ON_LINE, true),
            results = prefs.getBoolean(KEY_RESULTS, true),
            soundUri = prefs.getString(KEY_SOUND, null),
            alarmVolume = prefs.getBoolean(KEY_ALARM_VOLUME, true),
            untilDismissed = prefs.getBoolean(KEY_UNTIL_DISMISSED, true),
            vibrate = prefs.getBoolean(KEY_VIBRATE, true),
            popUp = prefs.getBoolean(KEY_POP_UP, true)
        )
        set(value) = prefs.edit()
            .putBoolean(KEY_UP_NEXT, value.upNext)
            .putBoolean(KEY_ON_LINE, value.onTheLine)
            .putBoolean(KEY_RESULTS, value.results)
            .putString(KEY_SOUND, value.soundUri)
            .putBoolean(KEY_ALARM_VOLUME, value.alarmVolume)
            .putBoolean(KEY_UNTIL_DISMISSED, value.untilDismissed)
            .putBoolean(KEY_VIBRATE, value.vibrate)
            .putBoolean(KEY_POP_UP, value.popUp)
            .apply()

    /** Base URL of the AUFPV club being watched; null when alerts are off. */
    var watchedClubUrl: String?
        get() = prefs.getString(KEY_WATCHING, null)
        set(value) = prefs.edit().putString(KEY_WATCHING, value).apply()

    var watchedClubName: String?
        get() = prefs.getString(KEY_WATCHING_NAME, null)
        set(value) = prefs.edit().putString(KEY_WATCHING_NAME, value).apply()

    /** Alerts already sent, so a restarted service doesn't ring again for the same heat. */
    var sentAlerts: Set<String>
        get() = prefs.getStringSet(KEY_SENT, emptySet()).orEmpty()
        set(value) = prefs.edit().putStringSet(KEY_SENT, value.toList().takeLast(MAX_SENT).toSet()).apply()

    private companion object {
        const val KEY_CALLSIGN = "callsign"
        const val KEY_WATCHING = "alertClubUrl"
        const val KEY_WATCHING_NAME = "alertClubName"
        const val KEY_SENT = "sentAlerts"
        const val KEY_UP_NEXT = "alertUpNext"
        const val KEY_ON_LINE = "alertOnTheLine"
        const val KEY_RESULTS = "alertResults"
        const val KEY_SOUND = "alertSound"
        const val KEY_ALARM_VOLUME = "alertAlarmVolume"
        const val KEY_UNTIL_DISMISSED = "alertUntilDismissed"
        const val KEY_VIBRATE = "alertVibrate"
        const val KEY_POP_UP = "alertPopUp"
        const val MAX_SENT = 200
    }
}
