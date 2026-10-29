package com.jamiecarl.droneside.ui.screens.live

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jamiecarl.droneside.ui.components.DsIcons
import com.jamiecarl.droneside.ui.components.SectionLabel
import com.jamiecarl.droneside.ui.components.dsCard
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsCyanBorder
import com.jamiecarl.droneside.ui.theme.DsCyanSurface
import com.jamiecarl.droneside.ui.theme.DsFaint
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsInk2
import com.jamiecarl.droneside.ui.theme.DsLine
import com.jamiecarl.droneside.ui.theme.DsLive
import com.jamiecarl.droneside.ui.theme.DsLiveBorder
import com.jamiecarl.droneside.ui.theme.DsLiveInk
import com.jamiecarl.droneside.ui.theme.DsLiveSurface
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.DsOnCyan
import com.jamiecarl.droneside.ui.theme.DsRaised
import com.jamiecarl.droneside.ui.theme.DsSurface

/**
 * "Race alerts": the pilot's callsign and the switch for background alerts. [pilotNames] (from the dashboard)
 * feed the callsign picker so it matches the timing system's spelling exactly.
 */
@Composable
fun RaceAlertsCard(
    callsign: String?,
    watching: String?,
    clubName: String,
    pilotNames: List<String>,
    onSetCallsign: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onStart()
        else Toast.makeText(context, "Allow notifications for DroneSide to get race alerts", Toast.LENGTH_LONG).show()
    }
    val enable = {
        val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsAsk) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else onStart()
    }
    val on = watching != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dsCard(
                radius = 20.dp,
                background = if (on) DsLiveSurface else DsSurface,
                border = if (on) DsLiveBorder else DsLine
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(DsIcons.Bell, contentDescription = null, tint = if (on) DsLive else DsMuted, modifier = Modifier.size(18.dp))
            SectionLabel("RACE ALERTS", modifier = Modifier.padding(start = 8.dp).weight(1f), color = if (on) DsLiveInk else DsMuted)
            if (callsign != null) {
                Switch(
                    checked = on,
                    onCheckedChange = { if (it) enable() else onStop() },
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
        if (callsign == null) {
            Text(
                "Get a loud alert when you're in the next heat, even with the app closed.",
                color = DsInk2,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Text(
                "Set your callsign",
                color = DsOnCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .dsCard(radius = 20.dp, background = DsCyan, border = DsCyan)
                    .clickable(role = Role.Button) { editing = true }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    callsign,
                    color = DsInk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    "Edit",
                    color = DsCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClickLabel = "Edit callsign") { editing = true }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Alert settings",
                    color = DsCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onOpenSettings)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                )
            }
            Text(
                when {
                    watching != null -> "Watching $watching in the background. You'll get an alarm when you're up next and again when your heat is on the line."
                    else -> "Off. Turn it on at the field to watch $clubName for your heats."
                },
                color = if (on) DsLiveInk else DsMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }

    if (editing) {
        CallsignDialog(
            initial = callsign.orEmpty(),
            pilotNames = pilotNames,
            onDismiss = { editing = false },
            onSave = { editing = false; onSetCallsign(it) }
        )
    }
}

/** [pilotNames] are suggestions from a live dashboard; empty is fine. */
@Composable
fun CallsignDialog(initial: String, pilotNames: List<String>, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val matches = remember(text, pilotNames) {
        if (text.isBlank()) emptyList()
        else pilotNames.filter { it.contains(text.trim(), ignoreCase = true) && !it.equals(text.trim(), ignoreCase = true) }.take(5)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DsSurface,
        title = { Text("Your callsign", color = DsInk) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Use the name the timing system shows for you, e.g. on the race line-ups.",
                    color = DsMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("Callsign", color = DsFaint) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onSave(text) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DsInk,
                        unfocusedTextColor = DsInk,
                        focusedBorderColor = DsCyan,
                        unfocusedBorderColor = DsLine,
                        cursorColor = DsCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                matches.forEach { name ->
                    Text(
                        name,
                        color = DsCyan,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .dsCard(radius = 10.dp, background = DsCyanSurface, border = DsCyanBorder)
                            .clickable { text = name }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("Save", color = DsCyan) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = DsMuted) }
        }
    )
}
