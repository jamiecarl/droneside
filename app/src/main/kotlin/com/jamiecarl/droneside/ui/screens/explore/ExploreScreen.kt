package com.jamiecarl.droneside.ui.screens.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jamiecarl.droneside.data.model.Event
import com.jamiecarl.droneside.data.repository.FpvTracksideRepository
import com.jamiecarl.droneside.ui.components.DsIcons
import com.jamiecarl.droneside.ui.components.EmptyState
import com.jamiecarl.droneside.ui.components.EventCard
import com.jamiecarl.droneside.ui.components.FloatingNavClearance
import com.jamiecarl.droneside.ui.components.FullScreenLoading
import com.jamiecarl.droneside.ui.components.SearchField
import com.jamiecarl.droneside.ui.components.SegmentedControl
import com.jamiecarl.droneside.util.parseApiDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

data class ExploreUiState(
    val loading: Boolean = true,
    val events: List<Event> = emptyList()
)

class ExploreViewModel : ViewModel() {
    private val repository = FpvTracksideRepository()

    private val _state = MutableStateFlow(ExploreUiState())
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()

    private var lastKey: Int? = null

    fun refresh(refreshKey: Int) {
        if (lastKey == refreshKey) return
        lastKey = refreshKey
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching {
                _state.value = ExploreUiState(loading = false, events = repository.getEvents())
            }.onFailure {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }
}

private fun startOfDay(date: Date): Date = Calendar.getInstance().apply {
    time = date
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.time

private val timeframes = listOf("Upcoming", "Recent", "Previous")

@Composable
fun ExploreScreen(
    refreshKey: Int,
    onOpenEvent: (Event) -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    LaunchedEffect(refreshKey) { viewModel.refresh(refreshKey) }
    val state by viewModel.state.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }

    val now = Date()
    val today = startOfDay(now)
    val thirtyDaysAgo = Date(now.time - 30L * 24 * 60 * 60 * 1000)

    val inTimeframe = state.events.filter { event ->
        val start = parseApiDate(event.start)
        val end = parseApiDate(event.end)
        val startDay = start?.let { startOfDay(it) }
        when (selectedTab) {
            0 -> (startDay != null && !startDay.before(today)) ||
                (start != null && end != null && !start.after(now) && !end.before(now))
            1 -> startDay != null && startDay.before(today) && !start.before(thirtyDaysAgo) &&
                end != null && end.before(now)
            else -> start != null && start.before(thirtyDaysAgo)
        }
    }
    // Upcoming reads soonest-first; past timeframes stay newest-first.
    val ordered = if (selectedTab == 0) inTimeframe.reversed() else inTimeframe
    val term = query.trim().lowercase()
    val events = if (term.isEmpty()) ordered else ordered.filter {
        it.name.lowercase().contains(term) || (it.club.name?.lowercase()?.contains(term) == true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SearchField(value = query, onValueChange = { query = it }, placeholder = "Search events or clubs")
            SegmentedControl(options = timeframes, selectedIndex = selectedTab, onSelect = { selectedTab = it })
        }

        when {
            state.loading -> FullScreenLoading("Loading events...")
            events.isEmpty() && term.isNotEmpty() ->
                EmptyState(DsIcons.Search, "No events match \"${query.trim()}\"")
            events.isEmpty() ->
                EmptyState(DsIcons.Calendar, "No ${timeframes[selectedTab].lowercase()} events found")
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = FloatingNavClearance),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventCard(event = event, onTap = { onOpenEvent(event) }, showClub = true)
                }
            }
        }
    }
}
