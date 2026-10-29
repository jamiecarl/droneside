package com.jamiecarl.droneside.ui.screens.rounddetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jamiecarl.droneside.data.model.RaceDetail
import com.jamiecarl.droneside.data.model.ResultSummary
import com.jamiecarl.droneside.data.repository.EventSessionCache
import com.jamiecarl.droneside.data.repository.FpvTracksideRepository
import com.jamiecarl.droneside.ui.components.EmptyState
import com.jamiecarl.droneside.ui.components.FaIcon
import com.jamiecarl.droneside.ui.components.FullScreenLoading
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsCyanBorder
import com.jamiecarl.droneside.ui.theme.DsCyanSurface
import com.jamiecarl.droneside.ui.theme.DsFaint
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsInk2
import com.jamiecarl.droneside.ui.theme.DsLine
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.DsMuted2
import com.jamiecarl.droneside.ui.theme.DsSurface
import com.jamiecarl.droneside.ui.theme.MonoDs
import com.jamiecarl.droneside.util.RaceStatus
import com.jamiecarl.droneside.util.formatRaceTime
import com.jamiecarl.droneside.util.raceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RoundDetailsUiState(
    val loading: Boolean = true,
    val races: List<RaceDetail> = emptyList()
)

class RoundDetailsViewModel : ViewModel() {
    private val repository = FpvTracksideRepository()

    private val _state = MutableStateFlow(RoundDetailsUiState())
    val state: StateFlow<RoundDetailsUiState> = _state.asStateFlow()

    private var lastKey: Int? = null

    fun refresh(eventId: String, roundId: String, eventType: String, refreshKey: Int) {
        if (lastKey == refreshKey) return
        lastKey = refreshKey
        viewModelScope.launch {
            runCatching {
                val pilots = EventSessionCache.getPilots(eventId)
                    ?: repository.getPilots(eventId).also { EventSessionCache.putPilots(eventId, it) }
                val channels = EventSessionCache.getChannels(eventId)
                    ?: repository.getChannelsForEvent(eventId).also { EventSessionCache.putChannels(eventId, it) }
                val races = repository.getRacesForRound(eventId, roundId, eventType, pilots, channels)
                _state.value = RoundDetailsUiState(loading = false, races = races)
            }.onFailure {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }
}

/** Time trials rank by best lap; other formats by total race time. Mirrors the original placeholder logic. */
fun displayTime(result: ResultSummary, eventType: String): String {
    val placeholder = if (!result.position.isNullOrBlank()) "DNF" else "-"
    val value = if (eventType == "TimeTrial") result.pbLapTime else result.raceTime
    return formatRaceTime(value?.takeIf { it.isNotBlank() } ?: placeholder)
}

fun parseHexColor(hex: String?): Color? = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrNull()

@Composable
fun RoundDetailsScreen(
    eventId: String,
    roundId: String,
    eventName: String,
    roundNumber: String,
    eventType: String,
    refreshKey: Int,
    onOpenRace: (RaceDetail) -> Unit,
    viewModel: RoundDetailsViewModel = viewModel()
) {
    LaunchedEffect(eventId, roundId, refreshKey) { viewModel.refresh(eventId, roundId, eventType, refreshKey) }
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp, 4.dp, 20.dp, 16.dp)) {
            Text(text = eventName, color = DsMuted2, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Round $roundNumber",
                    color = DsInk,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = eventType.uppercase(),
                    fontFamily = MonoDs,
                    fontSize = 10.sp,
                    color = DsCyan,
                    modifier = Modifier
                        .background(DsCyanSurface, RoundedCornerShape(6.dp))
                        .border(1.dp, DsCyanBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        when {
            state.loading -> FullScreenLoading("Loading races...")
            state.races.isEmpty() -> EmptyState(
                icon = FaIcon.FLAG_CHECKERED,
                title = "No races found",
                subtitle = "This round doesn't have any races"
            )
            else -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(state.races, key = { it.id }) { race ->
                    val status = raceStatus(race.start, race.end)
                    val statusText = when (status) {
                        RaceStatus.COMPLETE -> "COMPLETE"
                        RaceStatus.RUNNING -> "RUNNING"
                        RaceStatus.NOT_STARTED -> "NOT STARTED"
                    } + if (eventType != "TimeTrial") " · ${race.targetLaps ?: "-"} LAPS" else ""

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .background(DsSurface, RoundedCornerShape(16.dp))
                            .border(1.dp, DsLine, RoundedCornerShape(16.dp))
                            .clickable { onOpenRace(race) }
                            .padding(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                            Text(
                                text = "Race ${race.raceNumber}",
                                color = DsInk,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = statusText,
                                fontFamily = MonoDs,
                                fontSize = 10.sp,
                                color = if (status == RaceStatus.RUNNING) DsCyan else DsMuted
                            )
                        }
                        race.resultSummaries.forEach { result ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = result.position?.takeIf { it.isNotBlank() } ?: "-",
                                    fontFamily = MonoDs,
                                    fontSize = 12.sp,
                                    color = DsMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(14.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(start = 4.dp, end = 10.dp)
                                        .width(3.dp)
                                        .height(16.dp)
                                        .background(
                                            parseHexColor(result.resolvedChannel?.color) ?: DsFaint,
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                                Text(
                                    text = result.pilotName,
                                    color = DsInk2,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = displayTime(result, eventType),
                                    fontFamily = MonoDs,
                                    fontSize = 14.sp,
                                    color = DsInk
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
