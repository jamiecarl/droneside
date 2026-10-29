package com.jamiecarl.droneside.ui.screens.pilotdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jamiecarl.droneside.data.model.Pilot
import com.jamiecarl.droneside.data.model.RaceDetail
import com.jamiecarl.droneside.data.model.RawRaceData
import com.jamiecarl.droneside.data.model.ResultSummary
import com.jamiecarl.droneside.data.model.Round
import com.jamiecarl.droneside.data.model.resolveImageUrl
import com.jamiecarl.droneside.data.repository.EventSessionCache
import com.jamiecarl.droneside.data.repository.FpvTracksideRepository
import com.jamiecarl.droneside.ui.components.FaIcon
import com.jamiecarl.droneside.ui.components.FullScreenLoading
import com.jamiecarl.droneside.ui.components.InlinePlaceholder
import com.jamiecarl.droneside.ui.components.LapSparkline
import com.jamiecarl.droneside.ui.components.LapTable
import com.jamiecarl.droneside.ui.components.PilotAvatar
import com.jamiecarl.droneside.ui.components.SectionLabel
import com.jamiecarl.droneside.ui.components.StatTile
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsCyanBorder
import com.jamiecarl.droneside.ui.theme.DsCyanSurface
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsInk2
import com.jamiecarl.droneside.ui.theme.DsLine
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.DsMuted2
import com.jamiecarl.droneside.ui.theme.DsSurface
import com.jamiecarl.droneside.ui.theme.MonoDs
import com.jamiecarl.droneside.util.formatRaceTime
import com.jamiecarl.droneside.util.lapsForPilot
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class PilotDetailsUiState(
    val loading: Boolean = true,
    val pilot: Pilot? = null,
    val rounds: List<Round> = emptyList(),
    val raceByRound: Map<String, RaceDetail> = emptyMap(),
    val summaryByRound: Map<String, ResultSummary> = emptyMap(),
    val rawByRace: Map<String, RawRaceData> = emptyMap()
)

class PilotDetailsViewModel : ViewModel() {
    private val repository = FpvTracksideRepository()

    private val _state = MutableStateFlow(PilotDetailsUiState())
    val state: StateFlow<PilotDetailsUiState> = _state.asStateFlow()

    private var lastKey: Int? = null

    fun load(eventId: String, pilotId: String, refreshKey: Int) {
        if (lastKey == refreshKey) return
        lastKey = refreshKey
        viewModelScope.launch {
            runCatching {
                val pilots = EventSessionCache.getPilots(eventId)
                    ?: repository.getPilots(eventId).also { EventSessionCache.putPilots(eventId, it) }
                val rounds = repository.getRounds(eventId).sortedBy { it.roundNumber.toIntOrNull() ?: 0 }
                val races = repository.getRacesForPilot(eventId, pilotId)
                val raceByRound = races.associateBy { it.round }
                val summaryByRound = races.mapNotNull { race ->
                    race.resultSummaries.find { it.pilot == pilotId }?.let { race.round to it }
                }.toMap()
                _state.value = _state.value.copy(
                    loading = false,
                    pilot = pilots.find { it.id == pilotId },
                    rounds = rounds,
                    raceByRound = raceByRound,
                    summaryByRound = summaryByRound
                )

                val raws = summaryByRound.values.map { summary ->
                    async { runCatching { summary.race to repository.getRawRace(eventId, summary.race) }.getOrNull() }
                }.awaitAll().filterNotNull().toMap()
                _state.value = _state.value.copy(rawByRace = raws)
            }.onFailure { _state.value = _state.value.copy(loading = false) }
        }
    }
}

@Composable
fun PilotDetailsScreen(
    eventId: String,
    pilotId: String,
    refreshKey: Int,
    onOpenRace: (race: RaceDetail, round: Round) -> Unit,
    viewModel: PilotDetailsViewModel = viewModel()
) {
    LaunchedEffect(pilotId, refreshKey) { viewModel.load(eventId, pilotId, refreshKey) }
    val state by viewModel.state.collectAsState()

    val bestLap = state.summaryByRound.values
        .mapNotNull { it.pbLapTime?.toDoubleOrNull() }
        .filter { it > 0 }
        .minOrNull()
        ?.let { String.format(Locale.US, "%.2f", it) }
    val totalPoints = state.summaryByRound.values.sumOf { it.points?.toIntOrNull() ?: 0 }
    val allLaps = state.rounds.flatMap { round ->
        val summary = state.summaryByRound[round.id] ?: return@flatMap emptyList()
        lapsForPilot(state.rawByRace[summary.race], pilotId).map { it.lengthSeconds }
    }

    if (state.loading) {
        FullScreenLoading("Loading pilot...")
        return
    }
    val pilot = state.pilot

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(20.dp, 8.dp, 20.dp, 16.dp), verticalAlignment = Alignment.CenterVertically) {
                PilotAvatar(photoUrl = resolveImageUrl(pilot?.photoUrl), modifier = Modifier.size(64.dp))
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(pilot?.name ?: "Unknown Pilot", color = DsInk, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    if (!pilot?.firstName.isNullOrBlank() || !pilot?.lastName.isNullOrBlank()) {
                        Text(
                            "${pilot?.firstName.orEmpty()} ${pilot?.lastName.orEmpty()}".trim(),
                            color = DsMuted2,
                            fontSize = 14.sp
                        )
                    }
                    pilot?.catchPhrase?.takeIf { it.isNotBlank() }?.let {
                        Text(it, fontFamily = MonoDs, fontSize = 12.sp, color = DsMuted)
                    }
                }
            }
        }
        item {
            Row(modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("BEST LAP", bestLap ?: "NA", Modifier.weight(1f), DsCyan)
                StatTile("POINTS", totalPoints.toString(), Modifier.weight(1f))
            }
        }
        if (allLaps.size > 1) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 0.dp, 16.dp, 12.dp)
                        .background(DsSurface, RoundedCornerShape(16.dp))
                        .border(1.dp, DsLine, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        SectionLabel("EVERY LAP THIS EVENT", modifier = Modifier.weight(1f))
                        Text("PB ${bestLap ?: "NA"}", fontFamily = MonoDs, fontSize = 10.sp, color = DsCyan)
                    }
                    LapSparkline(laps = allLaps, heightDp = 70)
                }
            }
        }
        items(state.rounds, key = { it.id }) { round ->
            val summary = state.summaryByRound[round.id]
            val race = state.raceByRound[round.id]
            val laps = summary?.let { lapsForPilot(state.rawByRace[it.race], pilotId) } ?: emptyList()
            val rawLoaded = summary != null && state.rawByRace.containsKey(summary.race)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 0.dp, 16.dp, 10.dp)
                    .background(DsSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, DsLine, RoundedCornerShape(16.dp))
                    .clickable(enabled = race != null) { race?.let { onOpenRace(it, round) } }
                    .padding(14.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("R${round.roundNumber}", fontFamily = MonoDs, fontSize = 14.sp, color = DsMuted, modifier = Modifier.padding(end = 10.dp))
                    Text("Round ${round.roundNumber}", color = DsInk2, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    if (summary != null && round.eventType != "TimeTrial") {
                        val first = summary.position == "1"
                        Text(
                            "P${summary.position?.takeIf { it.isNotBlank() } ?: "-"}",
                            fontFamily = MonoDs,
                            fontSize = 12.sp,
                            color = if (first) DsCyan else DsMuted,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .let { if (first) it.background(DsCyanSurface, RoundedCornerShape(6.dp)) else it }
                                .border(1.dp, if (first) DsCyanBorder else DsLine, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (summary != null) {
                        Text(
                            formatRaceTime(summary.pbLapTime?.takeIf { it.isNotBlank() } ?: summary.raceTime ?: "NA"),
                            fontFamily = MonoDs,
                            fontSize = 16.sp,
                            color = DsInk
                        )
                    }
                }
                when {
                    summary?.lapCount == "0" -> InlinePlaceholder(FaIcon.CLOCK, "No laps recorded", "This pilot hasn't completed any valid laps yet")
                    laps.isNotEmpty() -> LapTable(laps)
                    summary == null || rawLoaded -> InlinePlaceholder(FaIcon.CLOCK, "No data available", "Race data is not available for this round")
                    else -> InlinePlaceholder(FaIcon.SPINNER, "Loading lap times...", "Please wait while we fetch the data")
                }
            }
        }
    }
}
