package com.jamiecarl.droneside.ui.screens.live

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jamiecarl.droneside.ui.components.DsIcons
import com.jamiecarl.droneside.ui.components.dsCard
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsCyanBorder
import com.jamiecarl.droneside.ui.theme.DsCyanSurface
import com.jamiecarl.droneside.ui.theme.DsFaint
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsLine
import com.jamiecarl.droneside.ui.theme.DsLive
import com.jamiecarl.droneside.ui.theme.DsLiveBorder
import com.jamiecarl.droneside.ui.theme.DsLiveSurface
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.DsSurface
import com.jamiecarl.droneside.ui.theme.DsSurface2

/**
 * Race alerts as a bell button: hollow when off, solid green when on. With no callsign yet, the first tap asks
 * for one. Settings holds the rest (sound, which alerts, …); a toast says what changed.
 */
@Composable
fun RaceAlertToggle(
    callsign: String?,
    on: Boolean,
    pilotNames: List<String>,
    onSetCallsign: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var askingCallsign by remember { mutableStateOf(false) }

    fun started(name: String) {
        onStart()
        Toast.makeText(context, "Race alerts on: you'll be alerted when $name is up next", Toast.LENGTH_SHORT).show()
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) callsign?.let(::started)
        else Toast.makeText(context, "Allow notifications for DroneSide to get race alerts", Toast.LENGTH_LONG).show()
    }

    fun enable(name: String) {
        val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsAsk) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else started(name)
    }

    val label = if (on) "Turn off race alerts" else "Turn on race alerts"
    Box(
        modifier = modifier
            .size(40.dp)
            .dsCard(
                radius = 20.dp,
                background = if (on) DsLiveSurface else DsSurface2,
                border = if (on) DsLiveBorder else DsLine
            )
            .semantics { stateDescription = if (on) "On" else "Off" }
            .clickable(role = Role.Switch, onClickLabel = label) {
                when {
                    on -> {
                        onStop()
                        Toast.makeText(context, "Race alerts off", Toast.LENGTH_SHORT).show()
                    }
                    callsign == null -> askingCallsign = true
                    else -> enable(callsign)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (on) DsIcons.BellFilled else DsIcons.Bell,
            contentDescription = "Race alerts",
            tint = if (on) DsLive else DsMuted,
            modifier = Modifier.size(19.dp)
        )
    }

    if (askingCallsign) {
        CallsignDialog(
            initial = "",
            pilotNames = pilotNames,
            onDismiss = { askingCallsign = false },
            onSave = { name ->
                askingCallsign = false
                onSetCallsign(name)
                enable(name.trim())
            }
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
