package com.jamiecarl.droneside.alerts

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.jamiecarl.droneside.data.live.LiveClub
import com.jamiecarl.droneside.data.live.LiveRaceState
import com.jamiecarl.droneside.data.live.LiveSession
import com.jamiecarl.droneside.data.live.LiveSnapshot
import com.jamiecarl.droneside.data.storage.RaceAlertStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Watches a club's live timing in the background and rings when the pilot's heat is coming up. Runs as a
 * foreground service (with its ongoing "Watching" notification) so Android keeps it and its network alive
 * through a race day. Stops itself after [IDLE_STOP_MS] with no racing.
 */
class RaceAlertService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watchJob: Job? = null
    private lateinit var store: RaceAlertStore

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        store = RaceAlertStore(this)
        RaceNotifications.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopWatching()
                return START_NOT_STICKY
            }
            ACTION_DISMISS -> {
                getSystemService(NotificationManager::class.java)
                    .cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0))
                if (watchJob != null) return START_STICKY
                // Started only to dismiss (e.g. a test alert): don't begin watching.
                stopSelf(startId)
                return START_NOT_STICKY
            }
        }

        val clubUrl = store.watchedClubUrl
        val callsign = store.callsign
        if (clubUrl == null || callsign == null) {
            stopWatching()
            return START_NOT_STICKY
        }
        val clubName = store.watchedClubName ?: clubUrl.substringAfter("://")

        ServiceCompat.startForeground(
            this,
            RaceNotifications.WATCHING_ID,
            RaceNotifications.watching(this, clubName, clubUrl, "Connecting to live timing…"),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else 0
        )
        _running.value = true
        if (watchJob == null) watchJob = scope.launch { watch(clubUrl, clubName, callsign) }
        // If Android kills us mid-meeting, come back and carry on (sent alerts are remembered).
        return START_STICKY
    }

    private suspend fun watch(clubUrl: String, clubName: String, callsign: String): Unit = coroutineScope {
        val session = LiveSession(clubUrl, pinnedEventId = null)
        val detector = RaceAlertDetector(callsign, store.sentAlerts)
        var lastActivityAt = System.currentTimeMillis()
        var lastSignature: Any? = null
        var lastStatus: String? = null

        launch { session.run() }
        launch {
            while (true) {
                delay(60_000)
                if (System.currentTimeMillis() - lastActivityAt > IDLE_STOP_MS) stopWatching()
            }
        }

        session.state.collect { state ->
            val snapshot = state.snapshot ?: return@collect
            // Anything changing on the card (new heat, start, finish, laps) counts as the meeting being on.
            val signature = Triple(snapshot.current?.id, snapshot.current?.state, state.feed.size)
            if (signature != lastSignature) {
                if (lastSignature != null) lastActivityAt = System.currentTimeMillis()
                lastSignature = signature
            }

            val alerts = detector.update(snapshot)
            if (alerts.isNotEmpty()) {
                alerts.forEach { RaceNotifications.show(this@RaceAlertService, it, clubUrl) }
                store.sentAlerts = detector.sentKeys
            }

            val status = statusLine(snapshot, callsign)
            if (status != lastStatus) {
                lastStatus = status
                getSystemService(NotificationManager::class.java).notify(
                    RaceNotifications.WATCHING_ID,
                    RaceNotifications.watching(this@RaceAlertService, clubName, clubUrl, status)
                )
            }
        }
    }

    private fun stopWatching() {
        store.watchedClubUrl = null
        watchJob?.cancel()
        watchJob = null
        _running.value = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        _running.value = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.jamiecarl.droneside.alerts.START"
        const val ACTION_STOP = "com.jamiecarl.droneside.alerts.STOP"
        const val ACTION_DISMISS = "com.jamiecarl.droneside.alerts.DISMISS"
        const val EXTRA_NOTIFICATION_ID = "notificationId"

        /** Give up after this long with nothing happening on the dashboard (the meeting's over). */
        const val IDLE_STOP_MS = 3 * 60 * 60 * 1000L

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()

        fun start(context: Context, club: LiveClub) {
            RaceAlertStore(context).apply {
                watchedClubUrl = club.baseUrl
                watchedClubName = club.name
            }
            ContextCompat.startForegroundService(
                context,
                Intent(context, RaceAlertService::class.java).setAction(ACTION_START)
            )
        }

        /** A running watch keeps the callsign it started with; restart it so a change takes effect. */
        fun restartIfRunning(context: Context) {
            if (!running.value) return
            val store = RaceAlertStore(context)
            val url = store.watchedClubUrl ?: return
            val club = LiveClub(store.watchedClubName ?: url, url, null)
            stop(context)
            start(context, club)
        }

        fun stop(context: Context) {
            RaceAlertStore(context).watchedClubUrl = null
            context.stopService(Intent(context, RaceAlertService::class.java))
            _running.value = false
        }

        /** One line for the ongoing notification: where the pilot's next heat is. */
        fun statusLine(snapshot: LiveSnapshot, callsign: String): String {
            val current = snapshot.current
            if (current != null && current.pilots.any { isCallsign(it.name, callsign) }) {
                return when (current.state) {
                    LiveRaceState.RUNNING -> "You're racing: ${current.title}"
                    LiveRaceState.PENDING -> "You're on the line: ${current.title}"
                    LiveRaceState.ENDED -> "${current.title} finished"
                }
            }
            val index = snapshot.upcoming.indexOfFirst { race -> race.pilots.any { isCallsign(it.name, callsign) } }
            val now = current?.let { "Now: ${it.title}" } ?: "No heat on right now"
            return when {
                snapshot.upcoming.isEmpty() -> now
                index == -1 -> "$now · $callsign isn't in the next ${snapshot.upcoming.size} heats"
                index == 0 -> "$now · You're up next: ${snapshot.upcoming[0].title}"
                else -> "$now · You're in ${snapshot.upcoming[index].title} (${index + 1} heats away)"
            }
        }
    }
}
