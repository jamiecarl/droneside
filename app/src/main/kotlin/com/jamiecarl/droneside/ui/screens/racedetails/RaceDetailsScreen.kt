package com.jamiecarl.droneside.ui.screens.racedetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jamiecarl.droneside.data.model.Pilot
import com.jamiecarl.droneside.data.model.RaceDetail
import com.jamiecarl.droneside.data.model.RawRaceData
import com.jamiecarl.droneside.data.model.ResultSummary
import com.jamiecarl.droneside.data.model.resolveImageUrl
import com.jamiecarl.droneside.data.repository.EventSessionCache
import com.jamiecarl.droneside.data.repository.FpvTracksideRepository
import com.jamiecarl.droneside.ui.components.EmptyState
import com.jamiecarl.droneside.ui.components.FaIcon
import com.jamiecarl.droneside.ui.components.FullScreenLoading
import com.jamiecarl.droneside.ui.components.InlinePlaceholder
import com.jamiecarl.droneside.ui.components.LapSparkline
import com.jamiecarl.droneside.ui.components.LapTable
import com.jamiecarl.droneside.ui.components.PilotAvatar
import com.jamiecarl.droneside.ui.components.SectionLabel
import com.jamiecarl.droneside.ui.components.StatTile
import com.jamiecarl.droneside.ui.screens.rounddetails.displayTime
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsCyanBorder
import com.jamiecarl.droneside.ui.theme.DsCyanSurface
import com.jamiecarl.droneside.ui.theme.DsHoleshot
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsLine
import com.jamiecarl.droneside.ui.theme.DsLineSoft
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.DsMuted2
import com.jamiecarl.droneside.ui.theme.DsSurface
import com.jamiecarl.droneside.ui.theme.DsSurface2
import com.jamiecarl.droneside.ui.theme.MonoDs
import com.jamiecarl.droneside.util.formatClockTime
import com.jamiecarl.droneside.util.formatRaceTime
import com.jamiecarl.droneside.util.hasRaceStarted
import com.jamiecarl.droneside.util.isRaceFinished
import com.jamiecarl.droneside.util.lapsForPilot
import com.jamiecarl.droneside.util.raceDuration
import com.jamiecarl.droneside.util.sortResultsByPbTime
import com.jamiecarl.droneside.util.sortResultsByPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class RaceDetailsUiState(
    val loading: Boolean = true,
    val loadingLaps: Boolean = true,
    val race: RaceDetail? = null,
    val raw: RawRaceData? = null,
    val pilots: List<Pilot> = emptyList()
)

class RaceDetailsViewModel : ViewModel() {
    private val repository = FpvTracksideRepository()

    private val _state = MutableStateFlow(RaceDetailsUiState())
    val state: StateFlow<RaceDetailsUiState> = _state.asStateFlow()

    private var lastKey: Int? = null

    fun load(eventId: String, roundId: String, raceId: String, eventType: String, refreshKey: Int) {
        if (lastKey == refreshKey) return
        lastKey = refreshKey
        viewModelScope.launch {
            runCatching {
                val pilots = EventSessionCache.getPilots(eventId)
                    ?: repository.getPilots(eventId).also { EventSessionCache.putPilots(eventId, it) }
                val channels = EventSessionCache.getChannels(eventId)
                    ?: repository.getChannelsForEvent(eventId).also { EventSessionCache.putChannels(eventId, it) }
                val race = repository.getRacesForRound(eventId, roundId, eventType, pilots, channels)
                    .find { it.id == raceId }
                _state.value = _state.value.copy(loading = false, race = race, pilots = pilots)
            }.onFailure { _state.value = _state.value.copy(loading = false) }

            runCatching {
                val raw = repository.getRawRace(eventId, raceId)
                _state.value = _state.value.copy(loadingLaps = false, raw = raw)
            }.onFailure { _state.value = _state.value.copy(loadingLaps = false) }
        }
    }
}

@Composable
fun RaceDetailsScreen(
    eventId: String,
    roundId: String,
    raceId: String,
    eventType: String,
    refreshKey: Int,
    onOpenPilot: (String) -> Unit,
    viewModel: RaceDetailsViewModel = viewModel()
) {
    LaunchedEffect(raceId, refreshKey) { viewModel.load(eventId, roundId, raceId, eventType, refreshKey) }
    val state by viewModel.state.collectAsState()
    val isTimeTrial = eventType == "TimeTrial"

    val race = state.race
    when {
        state.loading -> FullScreenLoading("Loading race...")
        race == null -> EmptyState(icon = FaIcon.FLAG_CHECKERED, title = "Race not found", subtitle = "")
        else -> {
            fun pilotOf(id: String) = state.pilots.find { it.id == id }
            val podium: List<ResultSummary>? = if (isRaceFinished(race.start, race.end)) {
                val sorted = if (isTimeTrial) sortResultsByPbTime(race.resultSummaries) else sortResultsByPosition(race.resultSummaries)
                sorted.take(3)
            } else null
            val orderedPodium = podium?.let { if (it.size == 3) listOf(it[1], it[0], it[2]) else it }
            val started = hasRaceStarted(race.start)
            val duration = raceDuration(race.start, race.end)
            val results = sortResultsByPosition(race.resultSummaries)

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                if (orderedPodium != null) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp, 8.dp, 20.dp, 12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            orderedPodium.forEach { result ->
                                val first = result.position == "1"
                                val blockHeight = when (result.position?.toIntOrNull() ?: 3) {
                                    1 -> 92.dp
                                    2 -> 62.dp
                                    else -> 46.dp
                                }
                                Column(
                                    modifier = Modifier.weight(1f).padding(horizontal = 3.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    PilotAvatar(
                                        photoUrl = resolveImageUrl(pilotOf(result.pilot)?.photoUrl),
                                        modifier = Modifier.size(if (first) 40.dp else 32.dp)
                                    )
                                    Column(
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .fillMaxWidth()
                                            .height(blockHeight)
                                            .background(
                                                if (first) DsCyanSurface else DsSurface2,
                                                RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (first) DsCyanBorder else DsLine,
                                                RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                                            )
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = result.position ?: "?",
                                            fontFamily = MonoDs,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = if (first) 20.sp else 16.sp,
                                            color = if (first) DsCyan else DsMuted
                                        )
                                        Text(
                                            text = pilotOf(result.pilot)?.name ?: "Unknown Pilot",
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = if (first) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (first) DsInk else DsMuted2
                                        )
                                        Text(
                                            text = displayTime(result, eventType),
                                            fontFamily = MonoDs,
                                            fontSize = 11.sp,
                                            color = if (first) DsCyan else DsMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (started) {
                    item {
                        Column {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(DsLineSoft))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionLabel("START", modifier = Modifier.padding(end = 6.dp))
                                Text(formatClockTime(race.start), fontFamily = MonoDs, fontSize = 12.sp, color = DsMuted, modifier = Modifier.weight(1f))
                                SectionLabel("END", modifier = Modifier.padding(end = 6.dp))
                                Text(formatClockTime(race.end), fontFamily = MonoDs, fontSize = 12.sp, color = DsMuted, modifier = Modifier.weight(1f))
                                duration?.let { Text(it, fontFamily = MonoDs, fontSize = 12.sp, color = DsMuted) }
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(DsLineSoft))
                        }
                    }
                }

                items(results, key = { it.id }) { result ->
                    val laps = lapsForPilot(state.raw, result.pilot)
                    val pilot = pilotOf(result.pilot)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 10.dp, 16.dp, 0.dp)
                            .background(DsSurface, RoundedCornerShape(16.dp))
                            .border(1.dp, DsLine, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).clickable { onOpenPilot(result.pilot) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PilotAvatar(photoUrl = resolveImageUrl(pilot?.photoUrl), modifier = Modifier.size(44.dp))
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text(pilot?.name ?: "Unknown Pilot", color = DsInk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Text("${laps.size} laps", fontFamily = MonoDs, fontSize = 10.sp, color = DsMuted2)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (!isTimeTrial) {
                                    Text(
                                        text = result.position?.takeIf { it.isNotBlank() } ?: "NA",
                                        fontFamily = MonoDs,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (result.position == "1") DsCyan else DsInk
                                    )
                                }
                                val points = result.points?.toIntOrNull() ?: 0
                                if (points > 0) {
                                    Text("$points PTS", fontFamily = MonoDs, fontSize = 10.sp, color = DsMuted2)
                                }
                            }
                        }

                        if (!state.loadingLaps && laps.size > 1) {
                            LapSparkline(laps = laps.map { it.lengthSeconds }, heightDp = 46, modifier = Modifier.padding(bottom = 10.dp))
                        }

                        if (!isTimeTrial) {
                            val avg = if (laps.isEmpty()) "NA" else String.format(Locale.US, "%.2f", laps.map { it.lengthSeconds }.average())
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatTile("HOLESHOT", formatRaceTime(result.holeshotTime ?: "NA"), Modifier.weight(1f), DsHoleshot)
                                StatTile("BEST LAP", formatRaceTime(result.pbLapTime ?: "NA"), Modifier.weight(1f), DsCyan)
                                StatTile("AVG", avg, Modifier.weight(1f))
                            }
                        } else {
                            StatTile("BEST LAP", formatRaceTime(result.pbLapTime ?: "NA"), Modifier.fillMaxWidth(), DsCyan)
                        }

                        when {
                            state.loadingLaps -> InlinePlaceholder(FaIcon.SPINNER, "Loading race data...", "Please wait while we fetch the lap times")
                            laps.isNotEmpty() -> LapTable(laps, modifier = Modifier.padding(top = 10.dp))
                            else -> InlinePlaceholder(FaIcon.CLOCK, "No laps recorded", "This pilot hasn't completed any valid laps")
                        }
                    }
                }
            }
        }
    }
}
