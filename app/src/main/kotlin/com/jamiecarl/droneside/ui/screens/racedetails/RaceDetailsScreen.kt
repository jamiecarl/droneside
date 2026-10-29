package com.jamiecarl.droneside.ui.screens.racedetails

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.jamiecarl.droneside.ui.components.Chip
import com.jamiecarl.droneside.ui.components.DsIcons
import com.jamiecarl.droneside.ui.components.EmptyState
import com.jamiecarl.droneside.ui.components.FullScreenLoading
import com.jamiecarl.droneside.ui.components.InlinePlaceholder
import com.jamiecarl.droneside.ui.components.LapSparkline
import com.jamiecarl.droneside.ui.components.LapTable
import com.jamiecarl.droneside.ui.components.PilotAvatar
import com.jamiecarl.droneside.ui.components.SectionLabel
import com.jamiecarl.droneside.ui.components.StatTile
import com.jamiecarl.droneside.ui.components.dsCard
import com.jamiecarl.droneside.ui.screens.rounddetails.displayTime
import com.jamiecarl.droneside.ui.screens.rounddetails.raceStatusLabel
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsCyanBorder
import com.jamiecarl.droneside.ui.theme.DsCyanSurface
import com.jamiecarl.droneside.ui.theme.DsHoleshot
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsInk2
import com.jamiecarl.droneside.ui.theme.DsLine
import com.jamiecarl.droneside.ui.theme.DsLineSoft
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.DsSurface
import com.jamiecarl.droneside.ui.theme.MonoDs
import com.jamiecarl.droneside.util.RaceStatus
import com.jamiecarl.droneside.util.formatClockTime
import com.jamiecarl.droneside.util.formatRaceTime
import com.jamiecarl.droneside.util.hasRaceStarted
import com.jamiecarl.droneside.util.isRaceFinished
import com.jamiecarl.droneside.util.lapsForPilot
import com.jamiecarl.droneside.util.raceDuration
import com.jamiecarl.droneside.util.raceStatus
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
    // null = untouched, so the top finisher starts expanded; afterwards it's whatever the user opened.
    var expanded by remember(raceId) { mutableStateOf<Set<String>?>(null) }

    val race = state.race
    when {
        state.loading -> FullScreenLoading("Loading race...")
        race == null -> EmptyState(DsIcons.Flag, "Race not found")
        else -> {
            fun pilotOf(id: String) = state.pilots.find { it.id == id }
            val podium: List<ResultSummary>? = if (isRaceFinished(race.start, race.end)) {
                val sorted = if (isTimeTrial) sortResultsByPbTime(race.resultSummaries) else sortResultsByPosition(race.resultSummaries)
                sorted.take(3)
            } else null
            val orderedPodium = podium?.let { if (it.size == 3) listOf(it[1], it[0], it[2]) else it }
            val started = hasRaceStarted(race.start)
            val duration = raceDuration(race.start, race.end)
            val status = raceStatus(race.start, race.end)
            val results = sortResultsByPosition(race.resultSummaries)
            val openIds = expanded ?: setOfNotNull(results.firstOrNull()?.id)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel("${results.size} PILOTS", modifier = Modifier.weight(1f))
                        val running = status == RaceStatus.RUNNING
                        Chip(
                            raceStatusLabel(status),
                            color = if (running) DsCyan else DsMuted,
                            background = if (running) DsCyanSurface else Color.Transparent,
                            border = if (running) DsCyanBorder else DsLine
                        )
                    }
                }

                if (orderedPodium != null) {
                    item { Podium(orderedPodium, eventType, ::pilotOf) }
                }

                if (started) {
                    item {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(DsLineSoft))
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                                TimingCell("START", formatClockTime(race.start), Modifier.weight(1f))
                                TimingCell("END", formatClockTime(race.end), Modifier.weight(1f))
                                TimingCell("TIME", duration ?: "—", Modifier.weight(1f))
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(DsLineSoft))
                        }
                    }
                }

                items(results, key = { it.id }) { result ->
                    val isOpen = result.id in openIds
                    PilotResultCard(
                        result = result,
                        pilot = pilotOf(result.pilot),
                        raw = state.raw,
                        loadingLaps = state.loadingLaps,
                        isTimeTrial = isTimeTrial,
                        expanded = isOpen,
                        onToggle = { expanded = if (isOpen) openIds - result.id else openIds + result.id },
                        onOpenPilot = { onOpenPilot(result.pilot) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimingCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SectionLabel(label)
        Text(value, fontFamily = MonoDs, fontSize = 13.sp, color = DsInk2)
    }
}

@Composable
private fun Podium(ordered: List<ResultSummary>, eventType: String, pilotOf: (String) -> Pilot?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        ordered.forEach { result ->
            val first = result.position == "1"
            val blockHeight = when (result.position?.toIntOrNull() ?: 3) {
                1 -> 104.dp
                2 -> 78.dp
                else -> 62.dp
            }
            val pilot = pilotOf(result.pilot)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PilotAvatar(
                    photoUrl = resolveImageUrl(pilot?.photoUrl),
                    cornerRadius = if (first) 16.dp else 14.dp,
                    modifier = Modifier
                        .size(if (first) 48.dp else 40.dp)
                        .let { if (first) it.border(2.dp, DsCyan, RoundedCornerShape(16.dp)) else it }
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(blockHeight)
                        .let {
                            val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                            if (first) it.background(DsCyanSurface, shape).border(1.dp, DsCyanBorder, shape)
                            else it.background(DsSurface, shape).border(1.dp, DsLine, shape)
                        }
                        .padding(horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        result.position ?: "?",
                        fontFamily = MonoDs,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (first) 22.sp else 16.sp,
                        color = if (first) DsCyan else DsMuted
                    )
                    Text(
                        pilot?.name ?: "Unknown Pilot",
                        fontSize = if (first) 13.sp else 12.sp,
                        fontWeight = if (first) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (first) DsInk else DsInk2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        displayTime(result, eventType),
                        fontFamily = MonoDs,
                        fontSize = 11.sp,
                        color = if (first) DsCyan else DsMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun PilotResultCard(
    result: ResultSummary,
    pilot: Pilot?,
    raw: RawRaceData?,
    loadingLaps: Boolean,
    isTimeTrial: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenPilot: () -> Unit
) {
    val laps = lapsForPilot(raw, result.pilot)
    val points = result.points?.toIntOrNull() ?: 0
    val position = result.position?.takeIf { it.isNotBlank() }
    val isWinner = position == "1"
    val meta = buildString {
        append("${laps.size} LAPS")
        if (points > 0) append(" · $points PTS")
    }

    Column(modifier = Modifier.fillMaxWidth().dsCard(radius = 22.dp).animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Collapse" else "Expand", onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                position ?: "–",
                fontFamily = MonoDs,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isWinner) DsCyan else DsInk2,
                modifier = Modifier.width(26.dp)
            )
            PilotAvatar(
                photoUrl = resolveImageUrl(pilot?.photoUrl),
                modifier = Modifier.size(if (expanded) 44.dp else 34.dp),
                cornerRadius = if (expanded) 14.dp else 11.dp
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(pilot?.name ?: "Unknown Pilot", color = DsInk, fontSize = if (expanded) 16.sp else 14.sp, fontWeight = FontWeight.SemiBold)
                if (expanded) SectionLabel(meta)
            }
            if (!expanded) {
                Text(
                    "BEST ${formatRaceTime(result.pbLapTime ?: "–")}",
                    fontFamily = MonoDs,
                    fontSize = 12.sp,
                    color = DsMuted,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Icon(
                DsIcons.ChevronDown,
                contentDescription = null,
                tint = DsMuted,
                modifier = Modifier.size(18.dp).rotate(if (expanded) 180f else 0f)
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!loadingLaps && laps.size > 1) {
                    LapSparkline(laps = laps.map { it.lengthSeconds }, heightDp = 56)
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
                    loadingLaps -> InlinePlaceholder(null, "Loading race data...", "Fetching the lap times")
                    laps.isNotEmpty() -> LapTable(laps)
                    else -> InlinePlaceholder(DsIcons.Clock, "No laps recorded", "This pilot hasn't completed any valid laps")
                }
                Text(
                    "View pilot",
                    color = DsCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .dsCard(radius = 14.dp, background = DsCyanSurface, border = DsCyanBorder)
                        .clickable(role = Role.Button, onClick = onOpenPilot)
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}
