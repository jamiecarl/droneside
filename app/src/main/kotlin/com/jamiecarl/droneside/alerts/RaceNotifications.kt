package com.jamiecarl.droneside.alerts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jamiecarl.droneside.MainActivity
import com.jamiecarl.droneside.R
import com.jamiecarl.droneside.data.live.LiveMath
import com.jamiecarl.droneside.data.storage.AlertPrefs
import com.jamiecarl.droneside.data.storage.RaceAlertStore

object RaceNotifications {

    /**
     * Up next / on the line. Android fixes a channel's sound, vibration and importance once it exists, so the
     * channel id carries the alert style ([alertChannelId]); changing the style in Settings swaps channels.
     */
    private const val CHANNEL_ALERTS_PREFIX = "race_alerts"
    /** Heat results: a normal ping. */
    const val CHANNEL_RESULTS = "race_results"
    /** The quiet ongoing "watching" notification the background service has to show. */
    const val CHANNEL_WATCHING = "race_watching"

    const val WATCHING_ID = 1
    private const val TEST_ID = 2

    private const val ACCENT = 0xFF00D4FF.toInt()
    private val ALERT_VIBRATION = longArrayOf(0, 600, 250, 600, 250, 600, 800, 1200)

    fun defaultAlertSound(): Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    fun alertSound(prefs: AlertPrefs): Uri? = when {
        prefs.silent -> null
        prefs.soundUri != null -> Uri.parse(prefs.soundUri)
        else -> defaultAlertSound()
    }

    private fun alertChannelId(prefs: AlertPrefs): String {
        val style = listOf(prefs.soundUri, prefs.alarmVolume, prefs.vibrate, prefs.popUp).hashCode()
        return "${CHANNEL_ALERTS_PREFIX}_${Integer.toHexString(style)}"
    }

    /** Makes sure the channel for the current alert style exists, and removes channels for old styles. */
    fun ensureAlertChannel(context: Context, prefs: AlertPrefs): String {
        val manager = context.getSystemService(NotificationManager::class.java)
        val id = alertChannelId(prefs)
        if (manager.getNotificationChannel(id) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    id,
                    "Your race is up",
                    if (prefs.popUp) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alerts when you're in the next heat and when your heat is on the line. " +
                        "Change the sound and style in DroneSide's settings."
                    setSound(
                        alertSound(prefs),
                        AudioAttributes.Builder()
                            // The alarm stream still sounds when the phone is on silent.
                            .setUsage(if (prefs.alarmVolume) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    enableVibration(prefs.vibrate)
                    vibrationPattern = if (prefs.vibrate) ALERT_VIBRATION else null
                    enableLights(true)
                    lightColor = ACCENT
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
        }
        manager.notificationChannels
            .filter { it.id.startsWith(CHANNEL_ALERTS_PREFIX) && it.id != id }
            .forEach { manager.deleteNotificationChannel(it.id) }
        return id
    }

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        ensureAlertChannel(context, RaceAlertStore(context).alertPrefs)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_RESULTS, "Your heat results", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Your finishing position and best lap after each heat you fly"
                },
                NotificationChannel(CHANNEL_WATCHING, "Watching races", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shown while DroneSide watches live timing for your heats"
                    setShowBadge(false)
                }
            )
        )
    }

    fun openLiveIntent(context: Context, clubUrl: String?, requestCode: Int = 0): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_OPEN_LIVE, clubUrl ?: ""),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun serviceIntent(context: Context, action: String, requestCode: Int, id: Int = 0): PendingIntent =
        PendingIntent.getService(
            context,
            requestCode,
            Intent(context, RaceAlertService::class.java).setAction(action).putExtra(RaceAlertService.EXTRA_NOTIFICATION_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun watching(context: Context, clubName: String, clubUrl: String, status: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_WATCHING)
            .setSmallIcon(R.drawable.ic_stat_live)
            .setColor(ACCENT)
            .setContentTitle("Watching $clubName")
            .setContentText(status)
            .setStyle(NotificationCompat.BigTextStyle().bigText(status))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openLiveIntent(context, clubUrl))
            .addAction(0, "Stop watching", serviceIntent(context, RaceAlertService.ACTION_STOP, 1))
            .build()

    fun show(context: Context, alert: RaceAlert, clubUrl: String) {
        val prefs = RaceAlertStore(context).alertPrefs
        val wanted = when (alert) {
            is RaceAlert.UpNext -> prefs.upNext
            is RaceAlert.OnTheLine -> prefs.onTheLine
            is RaceAlert.Result -> prefs.results
        }
        if (!wanted) return
        val id = alert.key.hashCode()
        val notification = when (alert) {
            is RaceAlert.UpNext -> loud(
                context, prefs, id, clubUrl,
                title = "You're up next!",
                text = "${alert.race.title} · you're on ${alert.me.channel}",
                big = buildString {
                    append("${alert.race.title} is the next heat")
                    alert.before?.let { append(" after $it") }
                    append(". You're on ${alert.me.channel} — get your quad and goggles ready.\n\n")
                    append(alert.race.pilots.joinToString("  ·  ") { "${it.name} ${it.channel}" })
                }
            )
            is RaceAlert.OnTheLine -> loud(
                context, prefs, id, clubUrl,
                title = "Get to the line — you're racing now!",
                text = "${alert.race.title} · channel ${alert.me.channel}",
                big = "${alert.race.title} is set up and waiting to start. You're on ${alert.me.channel}.\n\n" +
                    alert.race.pilots.joinToString("  ·  ") { "${it.name} ${it.channel}" }
            )
            is RaceAlert.Result -> {
                val me = alert.me
                val line = if (alert.race.isTimeTrial) {
                    "Best ${alert.race.consecWindow}: " + (me.bestConsec?.let(LiveMath::formatLap) ?: "—")
                } else {
                    "P${me.position ?: "—"} of ${alert.fieldSize}"
                } + (me.bestLap?.let { " · best lap ${LiveMath.formatLap(it)}" } ?: "")
                NotificationCompat.Builder(context, CHANNEL_RESULTS)
                    .setSmallIcon(R.drawable.ic_stat_live)
                    .setColor(ACCENT)
                    .setContentTitle("${alert.race.title} done")
                    .setContentText(line)
                    .setAutoCancel(true)
                    .setContentIntent(openLiveIntent(context, clubUrl, id))
                    .build()
            }
        }
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            context.getSystemService(NotificationManager::class.java).notify(id, notification)
        }
    }

    /** A sample "up next" alert with the current settings, from the Settings screen. */
    fun showTest(context: Context) {
        val prefs = RaceAlertStore(context).alertPrefs
        val callsign = RaceAlertStore(context).callsign ?: "you"
        val notification = loud(
            context, prefs, TEST_ID, clubUrl = null,
            title = "You're up next! (test)",
            text = "Round 3 — Race 2 · you're on R1",
            big = "This is how your race alerts will look and sound, $callsign." +
                if (prefs.untilDismissed) " It keeps going until you tap Got it." else ""
        )
        context.getSystemService(NotificationManager::class.java).notify(TEST_ID, notification)
    }

    private fun loud(
        context: Context,
        prefs: AlertPrefs,
        id: Int,
        clubUrl: String?,
        title: String,
        text: String,
        big: String
    ): Notification {
        val notification = NotificationCompat.Builder(context, ensureAlertChannel(context, prefs))
            .setSmallIcon(R.drawable.ic_stat_live)
            .setColor(ACCENT)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openLiveIntent(context, clubUrl, id))
            .addAction(0, "Got it", serviceIntent(context, RaceAlertService.ACTION_DISMISS, id, id))
            .build()
        // Keep ringing until it's acknowledged, so it can't be missed at a noisy field.
        if (prefs.untilDismissed) notification.flags = notification.flags or Notification.FLAG_INSISTENT
        return notification
    }
}
