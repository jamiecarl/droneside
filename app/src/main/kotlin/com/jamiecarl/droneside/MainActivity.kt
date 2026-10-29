package com.jamiecarl.droneside

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jamiecarl.droneside.data.storage.LiveClubStore
import com.jamiecarl.droneside.ui.navigation.DronesideApp
import com.jamiecarl.droneside.ui.theme.DronesideTheme

class MainActivity : ComponentActivity() {

    /** Bumped when a race-alert notification asks for the Live tab. */
    private val openLiveRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setOnExitAnimationListener { splash ->
            // Let the Lap D finish drawing (Android 12+ reports the icon animation's timing), then zoom it
            // slightly while the splash fades into the app.
            val remaining = runCatching {
                splash.iconAnimationStartMillis + splash.iconAnimationDurationMillis - System.currentTimeMillis()
            }.getOrDefault(0L).coerceIn(0L, 1000L)
            splash.iconView.animate()
                .setStartDelay(remaining)
                .scaleX(1.12f).scaleY(1.12f)
                .setDuration(SPLASH_EXIT_MS)
                .start()
            splash.view.animate()
                .setStartDelay(remaining)
                .alpha(0f)
                .setDuration(SPLASH_EXIT_MS)
                .withEndAction { splash.remove() }
                .start()
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            DronesideTheme {
                DronesideApp(openLiveRequests = openLiveRequests.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val clubUrl = intent?.getStringExtra(EXTRA_OPEN_LIVE) ?: return
        if (clubUrl.isNotEmpty()) LiveClubStore(this).set(clubUrl)
        openLiveRequests.intValue++
    }

    companion object {
        /** Opens the Live tab; the value is the club site to show (or empty to keep the current one). */
        const val EXTRA_OPEN_LIVE = "openLive"

        private const val SPLASH_EXIT_MS = 280L
    }
}
