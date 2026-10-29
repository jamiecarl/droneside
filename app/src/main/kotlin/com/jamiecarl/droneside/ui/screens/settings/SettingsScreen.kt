package com.jamiecarl.droneside.ui.screens.settings

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jamiecarl.droneside.BuildConfig
import com.jamiecarl.droneside.alerts.RaceAlertService
import java.util.Locale
import com.jamiecarl.droneside.alerts.RaceNotifications
import com.jamiecarl.droneside.data.storage.AlertPrefs
import com.jamiecarl.droneside.data.storage.RaceAlertStore
import com.jamiecarl.droneside.ui.components.DsIcons
import com.jamiecarl.droneside.ui.components.SectionLabel
import com.jamiecarl.droneside.ui.components.dsCard
import com.jamiecarl.droneside.ui.screens.live.CallsignDialog
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsLine
import com.jamiecarl.droneside.ui.theme.DsLineSoft
import com.jamiecarl.droneside.ui.theme.DsLive
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.DsOnCyan
import com.jamiecarl.droneside.ui.theme.DsRaised
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val callsign: String? = null,
    val alerts: AlertPrefs = AlertPrefs(),
    val recordLive: Boolean = false,
    /** e.g. "2 recordings · 18.4 MB". */
    val recordings: String = ""
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = RaceAlertStore(application)

    private val _state = MutableStateFlow(
        SettingsUiState(store.callsign, store.alertPrefs, store.recordLiveData, recordingsSummary())
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private fun recordingsSummary(): String {
        val dirs = RaceAlertService.capturesDir(getApplication()).listFiles { f -> f.isDirectory }.orEmpty()
        val bytes = dirs.sumOf { d -> d.walkTopDown().filter { it.isFile }.sumOf { it.length() } }
        return "${dirs.size} recording${if (dirs.size == 1) "" else "s"} · " +
            String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
    }

    fun setRecordLive(on: Boolean) {
        store.recordLiveData = on
        _state.value = _state.value.copy(recordLive = store.recordLiveData)
        // Takes effect from the next watch; restart a running one so it starts (or stops) now.
        RaceAlertService.restartIfRunning(getApplication())
    }

    fun deleteRecordings() {
        // Leave a recording that's still being written alone.
        if (!RaceAlertService.running.value) RaceAlertService.capturesDir(getApplication()).deleteRecursively()
        _state.value = _state.value.copy(recordings = recordingsSummary())
    }

    fun setCallsign(callsign: String) {
        store.callsign = callsign
        _state.value = _state.value.copy(callsign = store.callsign)
        RaceAlertService.restartIfRunning(getApplication())
    }

    fun updateAlerts(change: (AlertPrefs) -> AlertPrefs) {
        val prefs = change(_state.value.alerts)
        store.alertPrefs = prefs
        // Swap in the channel for the new style now, so the system's own settings page matches.
        RaceNotifications.ensureAlertChannel(getApplication(), prefs)
        _state.value = _state.value.copy(alerts = prefs)
    }

    fun sendTest() {
        RaceNotifications.showTest(getApplication())
    }
}

private fun soundTitle(context: Context, prefs: AlertPrefs): String {
    if (prefs.silent) return "Silent"
    fun titleOf(uri: Uri?) = uri?.let { runCatching { RingtoneManager.getRingtone(context, it)?.getTitle(context) }.getOrNull() }
    if (prefs.soundUri == null) {
        // The default URI is an alias ("Default (274)"); name the sound it points at instead.
        val actual = titleOf(RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM))
        return "Default alarm" + (actual?.let { " · $it" } ?: "")
    }
    return titleOf(Uri.parse(prefs.soundUri)) ?: "Custom sound"
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val alerts = state.alerts
    var editingCallsign by remember { mutableStateOf(false) }

    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val picked = result.data?.let {
            IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        }
        viewModel.updateAlerts { p ->
            p.copy(
                soundUri = when {
                    picked == null -> AlertPrefs.SILENT
                    picked == RaceNotifications.defaultAlertSound() || RingtoneManager.isDefault(picked) -> null
                    else -> picked.toString()
                }
            )
        }
    }
    val pickSound = {
        soundPicker.launch(
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Race alert sound")
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RaceNotifications.defaultAlertSound())
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, RaceNotifications.alertSound(alerts))
        )
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.sendTest()
        else Toast.makeText(context, "Allow notifications for DroneSide to get race alerts", Toast.LENGTH_LONG).show()
    }
    val sendTest = {
        val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsAsk) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else viewModel.sendTest()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Section("PILOT") {
            NavRow(
                title = "Callsign",
                value = state.callsign ?: "Not set",
                subtitle = "Must match the name on the race line-ups",
                onClick = { editingCallsign = true }
            )
        }

        Section("RACE ALERTS") {
            SwitchRow("You're up next", "When you're in the heat after the one being flown", alerts.upNext) {
                viewModel.updateAlerts { p -> p.copy(upNext = it) }
            }
            Divider()
            SwitchRow("Get to the line", "When your heat is set up and about to start", alerts.onTheLine) {
                viewModel.updateAlerts { p -> p.copy(onTheLine = it) }
            }
            Divider()
            SwitchRow("Heat results", "Your position and best lap after each heat", alerts.results) {
                viewModel.updateAlerts { p -> p.copy(results = it) }
            }
        }

        Section("ALERT STYLE") {
            NavRow(title = "Sound", value = soundTitle(context, alerts), onClick = pickSound)
            Divider()
            SwitchRow(
                "Keep ringing until dismissed",
                "Repeats until you tap Got it. Off plays the sound once.",
                alerts.untilDismissed
            ) { viewModel.updateAlerts { p -> p.copy(untilDismissed = it) } }
            Divider()
            SwitchRow(
                "Ring on silent",
                "Uses the alarm volume, so it sounds even when the phone is on silent",
                alerts.alarmVolume,
                enabled = !alerts.silent
            ) { viewModel.updateAlerts { p -> p.copy(alarmVolume = it) } }
            Divider()
            SwitchRow("Vibrate", "A long, strong pattern", alerts.vibrate) {
                viewModel.updateAlerts { p -> p.copy(vibrate = it) }
            }
            Divider()
            SwitchRow("Pop up on screen", "Shows over whatever you're doing. Off just goes to the notification shade.", alerts.popUp) {
                viewModel.updateAlerts { p -> p.copy(popUp = it) }
            }
            Text(
                "Send a test alert",
                color = DsOnCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(start = 14.dp, top = 6.dp, bottom = 14.dp)
                    .dsCard(radius = 20.dp, background = DsCyan, border = DsCyan)
                    .clickable(role = Role.Button, onClick = sendTest)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        Section("BACKGROUND") {
            NavRow(
                title = "Battery & autostart",
                subtitle = "On Xiaomi and some other phones, set battery saver to \"No restrictions\" and allow " +
                    "autostart, or the phone may stop race alerts during a long meeting.",
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    )
                }
            )
            Divider()
            NavRow(
                title = "System notification settings",
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }
            )
        }

        if (BuildConfig.DEBUG) {
            Section("DEVELOPER") {
                SwitchRow(
                    "Record live data",
                    "While race alerts are on, save the live stream and dashboard snapshots on the phone. " +
                        "Copy them off with tools/pull-captures.sh.",
                    state.recordLive
                ) { viewModel.setRecordLive(it) }
                Divider()
                NavRow(
                    title = "Delete recordings",
                    value = state.recordings,
                    subtitle = if (RaceAlertService.running.value) "Stop race alerts first" else null,
                    onClick = { viewModel.deleteRecordings() }
                )
            }
        }
    }

    if (editingCallsign) {
        CallsignDialog(
            initial = state.callsign.orEmpty(),
            pilotNames = emptyList(),
            onDismiss = { editingCallsign = false },
            onSave = { editingCallsign = false; viewModel.setCallsign(it) }
        )
    }
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel(label, modifier = Modifier.padding(start = 6.dp, top = 8.dp))
        Column(modifier = Modifier.fillMaxWidth().dsCard(radius = 20.dp)) { content() }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(color = DsLineSoft, modifier = Modifier.padding(horizontal = 14.dp))
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = if (enabled) DsInk else DsMuted, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = DsMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DsOnCyan,
                checkedTrackColor = DsLive,
                uncheckedThumbColor = DsMuted,
                uncheckedTrackColor = DsRaised,
                uncheckedBorderColor = DsLine
            )
        )
    }
}

@Composable
private fun NavRow(title: String, value: String? = null, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = DsInk, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (value != null) Text(value, color = DsCyan, fontSize = 13.sp)
            if (subtitle != null) Text(subtitle, color = DsMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Icon(DsIcons.ChevronRight, contentDescription = null, tint = DsMuted, modifier = Modifier.size(18.dp))
    }
}
