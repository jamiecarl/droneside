package com.jamiecarl.droneside.ui.screens.eventdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jamiecarl.droneside.data.model.Channel
import com.jamiecarl.droneside.data.model.Pilot
import com.jamiecarl.droneside.data.model.RaceSummary
import com.jamiecarl.droneside.data.model.Round
import com.jamiecarl.droneside.data.repository.EventSessionCache
import com.jamiecarl.droneside.data.repository.FpvTracksideRepository
import com.jamiecarl.droneside.ui.components.EmptyState
import com.jamiecarl.droneside.ui.components.FaIcon
import com.jamiecarl.droneside.ui.components.FontAwesome
import com.jamiecarl.droneside.ui.components.FullScreenLoading
import com.jamiecarl.droneside.ui.components.PilotAvatar
import com.jamiecarl.droneside.ui.theme.DsBase
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsCyanSurface
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsInk2
import com.jamiecarl.droneside.ui.theme.DsLine
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.DsMuted2
import com.jamiecarl.droneside.ui.theme.DsSurface
import com.jamiecarl.droneside.ui.theme.MonoDs
import com.jamiecarl.droneside.util.formatRaceTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EventDetailsUiState(
    val loadingRounds: Boolean = true,
    val loadingPilots: Boolean = true,
    val loadingSummaries: Boolean = true,
    val rounds: List<Round> = emptyList(),
    val pilots: List<Pilot> = emptyList(),
    val raceSummaries: List<RaceSummary> = emptyList(),
    val channels: List<Channel> = emptyList()
)

class EventDetailsViewModel : ViewModel() {
    private val repository = FpvTracksideRepository()

    private val _state = MutableStateFlow(EventDetailsUiState())
    val state: StateFlow<EventDetailsUiState> = _state.asStateFlow()

    private var lastKey: Int? = null

    fun load(eventId: String, refreshKey: Int) {
        if (lastKey == refreshKey) return
        lastKey = refreshKey
        viewModelScope.launch {
            runCatching {
                val channels = repository.getChannelsForEvent(eventId)
                EventSessionCache.putChannels(eventId, channels)
                _state.value = _state.value.copy(channels = channels)
            }
            runCatching {
                val rounds = repository.getRounds(eventId)
                _state.value = _state.value.copy(loadingRounds = false, rounds = rounds)
            }.onFailure { _state.value = _state.value.copy(loadingRounds = false) }

            runCatching {
                val pilots = repository.getPilots(eventId)
                EventSessionCache.putPilots(eventId, pilots)
                _state.value = _state.value.copy(loadingPilots = false, pilots = pilots)
            }.onFailure { _state.value = _state.value.copy(loadingPilots = false) }

            runCatching {
                val summaries = repository.getRaceSummaries(eventId)
                _state.value = _state.value.copy(loadingSummaries = false, raceSummaries = summaries)
            }.onFailure { _state.value = _state.value.copy(loadingSummaries = false) }
        }
    }
}

@Composable
fun EventDetailsScreen(
    eventId: String,
    eventName: String,
    clubName: String,
    refreshKey: Int,
    onOpenRound: (Round) -> Unit,
    onOpenPilot: (Pilot) -> Unit,
    viewModel: EventDetailsViewModel = viewModel()
) {
    LaunchedEffect(eventId, refreshKey) { viewModel.load(eventId, refreshKey) }
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val bestLapByPilot = remember(state.raceSummaries) {
        val best = mutableMapOf<String, RaceSummary>()
        state.raceSummaries.forEach { summary ->
            val existing = best[summary.pilotId]
            val time = summary.pbLapTime
            if (existing == null || (time != null && time > 0 && (existing.pbLapTime == null || time < existing.pbLapTime!!))) {
                best[summary.pilotId] = summary
            }
        }
        best.values.filter { (it.pbLapTime ?: 0.0) > 0 }.sortedBy { it.pbLapTime }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp, 4.dp, 20.dp, 16.dp)) {
            Text(text = eventName, color = DsInk, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(text = clubName, color = DsMuted2, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }

        TabRow(selectedTabIndex = selectedTab, containerColor = DsBase, contentColor = DsCyan) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Rounds") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Pilots") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Top Laps") })
        }

        when (selectedTab) {
            0 -> when {
                state.loadingRounds -> FullScreenLoading("Loading rounds...")
                state.rounds.isEmpty() -> EmptyState(icon = FaIcon.FLAG_CHECKERED, title = "No rounds yet", subtitle = "")
                else -> LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(state.rounds) { round ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .background(DsSurface, RoundedCornerShape(16.dp))
                                .border(1.dp, DsLine, RoundedCornerShape(16.dp))
                                .clickable { onOpenRound(round) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = round.roundNumber.padStart(2, '0'),
                                fontFamily = MonoDs,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DsMuted,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = "Round ${round.roundNumber}",
                                color = DsInk2,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = round.eventType.uppercase(),
                                fontFamily = MonoDs,
                                fontSize = 10.sp,
                                color = DsMuted,
                                modifier = Modifier
                                    .border(1.dp, DsLine, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            1 -> when {
                state.loadingPilots -> FullScreenLoading("Loading pilots...")
                state.pilots.isEmpty() -> EmptyState(
                    icon = FaIcon.USER,
                    title = "No pilots registered",
                    subtitle = "Pilots will appear here once they register for this event"
                )
                else -> LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(state.pilots) { pilot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .background(DsSurface, RoundedCornerShape(16.dp))
                                .border(1.dp, DsLine, RoundedCornerShape(16.dp))
                                .clickable { onOpenPilot(pilot) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PilotAvatar(photoUrl = com.jamiecarl.droneside.data.model.resolveImageUrl(pilot.photoUrl), modifier = Modifier.size(44.dp))
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(text = pilot.name, color = DsInk, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                if (!pilot.firstName.isNullOrBlank() || !pilot.lastName.isNullOrBlank()) {
                                    Text(
                                        text = "${pilot.firstName.orEmpty()} ${pilot.lastName.orEmpty()}".trim(),
                                        color = DsMuted2,
                                        fontSize = 12.sp
                                    )
                                }
                                pilot.catchPhrase?.takeIf { it.isNotBlank() }?.let {
                                    Text(text = it, fontFamily = MonoDs, fontSize = 12.sp, color = DsMuted, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
            else -> when {
                state.loadingSummaries -> FullScreenLoading("Loading lap times...")
                bestLapByPilot.isEmpty() -> EmptyState(
                    icon = FaIcon.CLOCK,
                    title = "No lap times available",
                    subtitle = "Lap times will appear here once races are completed"
                )
                else -> LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    itemsIndexed(bestLapByPilot) { index, summary ->
                        val pilot = state.pilots.find { it.id == summary.pilotId }
                        val isFirst = index == 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .let {
                                    if (isFirst) {
                                        it.background(DsCyanSurface, RoundedCornerShape(16.dp))
                                            .border(1.dp, com.jamiecarl.droneside.ui.theme.DsCyanBorder, RoundedCornerShape(16.dp))
                                            .padding(14.dp)
                                    } else {
                                        it.border(0.dp, DsLine)
                                            .padding(vertical = 12.dp, horizontal = 14.dp)
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                fontFamily = MonoDs,
                                fontSize = 14.sp,
                                color = if (isFirst) DsCyan else DsMuted,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            PilotAvatar(
                                photoUrl = com.jamiecarl.droneside.data.model.resolveImageUrl(pilot?.photoUrl),
                                modifier = Modifier.size(if (isFirst) 36.dp else 32.dp)
                            )
                            Text(
                                text = pilot?.name ?: "Unknown Pilot",
                                color = if (isFirst) DsInk else DsInk2,
                                fontSize = 14.sp,
                                fontWeight = if (isFirst) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(start = 12.dp).weight(1f)
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatRaceTime(summary.pbLapTime?.toString()),
                                    fontFamily = MonoDs,
                                    fontSize = if (isFirst) 20.sp else 16.sp,
                                    fontWeight = if (isFirst) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isFirst) DsCyan else DsInk2
                                )
                                if (!isFirst) {
                                    val fastest = bestLapByPilot.firstOrNull()?.pbLapTime
                                    val gap = if (fastest != null && summary.pbLapTime != null) {
                                        "+" + "%.2f".format(summary.pbLapTime!! - fastest)
                                    } else ""
                                    Text(text = gap, fontFamily = MonoDs, fontSize = 10.sp, color = DsMuted2)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
