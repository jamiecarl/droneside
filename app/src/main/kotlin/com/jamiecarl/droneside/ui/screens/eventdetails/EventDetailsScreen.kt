package com.jamiecarl.droneside.ui.screens.eventdetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.jamiecarl.droneside.data.model.resolveImageUrl
import com.jamiecarl.droneside.data.repository.EventSessionCache
import com.jamiecarl.droneside.data.repository.FpvTracksideRepository
import com.jamiecarl.droneside.ui.components.Chip
import com.jamiecarl.droneside.ui.components.DsIcons
import com.jamiecarl.droneside.ui.components.EmptyState
import com.jamiecarl.droneside.ui.components.FullScreenLoading
import com.jamiecarl.droneside.ui.components.PilotAvatar
import com.jamiecarl.droneside.ui.components.SegmentedControl
import com.jamiecarl.droneside.ui.components.dsCard
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsCyanBorder
import com.jamiecarl.droneside.ui.theme.DsCyanSurface
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsInk2
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.MonoDs
import com.jamiecarl.droneside.util.formatRaceTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

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
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val bestLapByPilot = remember(state.raceSummaries) {
        val best = mutableMapOf<String, RaceSummary>()
        state.raceSummaries.forEach { summary ->
            val existing = best[summary.pilotId]
            val time = summary.pbLapTime
            if (existing == null || (time != null && time > 0 && (existing.pbLapTime == null || time < existing.pbLapTime))) {
                best[summary.pilotId] = summary
            }
        }
        best.values.filter { (it.pbLapTime ?: 0.0) > 0 }.sortedBy { it.pbLapTime }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(clubName, color = DsMuted, fontSize = 13.sp)
            Text(eventName, color = DsInk, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp, letterSpacing = (-0.3).sp)
        }

        SegmentedControl(
            options = listOf("Rounds", "Pilots", "Top laps"),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        val listPadding = PaddingValues(16.dp)
        when (selectedTab) {
            0 -> when {
                state.loadingRounds -> FullScreenLoading("Loading rounds...")
                state.rounds.isEmpty() -> EmptyState(DsIcons.Flag, "No rounds yet", "Rounds will appear here once racing starts")
                else -> LazyColumn(contentPadding = listPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.rounds, key = { it.id }) { round ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .dsCard()
                                .clickable { onOpenRound(round) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                round.roundNumber.padStart(2, '0'),
                                fontFamily = MonoDs,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DsMuted,
                                modifier = Modifier.padding(end = 14.dp)
                            )
                            Text(
                                "Round ${round.roundNumber}",
                                color = DsInk2,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Chip(round.eventType.uppercase())
                            Icon(
                                DsIcons.ChevronRight,
                                contentDescription = null,
                                tint = DsMuted,
                                modifier = Modifier.padding(start = 8.dp).size(18.dp)
                            )
                        }
                    }
                }
            }
            1 -> when {
                state.loadingPilots -> FullScreenLoading("Loading pilots...")
                state.pilots.isEmpty() -> EmptyState(
                    DsIcons.User,
                    "No pilots registered",
                    "Pilots will appear here once they register for this event"
                )
                else -> LazyColumn(contentPadding = listPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.pilots, key = { it.id }) { pilot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .dsCard()
                                .clickable { onOpenPilot(pilot) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PilotAvatar(photoUrl = resolveImageUrl(pilot.photoUrl), modifier = Modifier.size(44.dp))
                            Column(
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(pilot.name, color = DsInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                val realName = "${pilot.firstName.orEmpty()} ${pilot.lastName.orEmpty()}".trim()
                                if (realName.isNotEmpty()) Text(realName, color = DsMuted, fontSize = 12.sp)
                                pilot.catchPhrase?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, fontFamily = MonoDs, fontSize = 11.sp, color = DsMuted)
                                }
                            }
                            Icon(DsIcons.ChevronRight, contentDescription = null, tint = DsMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            else -> when {
                state.loadingSummaries -> FullScreenLoading("Loading lap times...")
                bestLapByPilot.isEmpty() -> EmptyState(
                    DsIcons.Clock,
                    "No lap times available",
                    "Lap times will appear here once races are completed"
                )
                else -> LazyColumn(contentPadding = listPadding, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(bestLapByPilot, key = { _, it -> it.pilotId }) { index, summary ->
                        val pilot = state.pilots.find { it.id == summary.pilotId }
                        val isFirst = index == 0
                        val fastest = bestLapByPilot.first().pbLapTime
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .let {
                                    if (isFirst) it.dsCard(radius = 16.dp, background = DsCyanSurface, border = DsCyanBorder)
                                    else it.dsCard(radius = 16.dp)
                                }
                                .clickable(enabled = pilot != null) { pilot?.let(onOpenPilot) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                (index + 1).toString(),
                                fontFamily = MonoDs,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isFirst) DsCyan else DsInk2,
                                modifier = Modifier.width(26.dp)
                            )
                            PilotAvatar(photoUrl = resolveImageUrl(pilot?.photoUrl), modifier = Modifier.size(34.dp), cornerRadius = 11.dp)
                            Text(
                                pilot?.name ?: "Unknown Pilot",
                                color = if (isFirst) DsInk else DsInk2,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    formatRaceTime(summary.pbLapTime?.toString()),
                                    fontFamily = MonoDs,
                                    fontSize = if (isFirst) 18.sp else 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isFirst) DsCyan else DsInk2
                                )
                                if (!isFirst && fastest != null && summary.pbLapTime != null) {
                                    Text(
                                        "+" + String.format(Locale.US, "%.2f", summary.pbLapTime - fastest),
                                        fontFamily = MonoDs,
                                        fontSize = 11.sp,
                                        color = DsMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
