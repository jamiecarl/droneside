package com.jamiecarl.droneside.ui.screens.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jamiecarl.droneside.data.model.Channel
import com.jamiecarl.droneside.data.model.Event
import com.jamiecarl.droneside.data.repository.FpvTracksideRepository
import com.jamiecarl.droneside.ui.components.ClubEventHeaderCard
import com.jamiecarl.droneside.ui.components.EmptyState
import com.jamiecarl.droneside.ui.components.FaIcon
import com.jamiecarl.droneside.ui.components.FullScreenLoading
import com.jamiecarl.droneside.ui.theme.DsBase
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.util.formatLongDate
import com.jamiecarl.droneside.util.parseApiDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

data class ExploreUiState(
    val loading: Boolean = true,
    val events: List<Event> = emptyList(),
    val channels: List<Channel> = emptyList()
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
                val events = repository.getEvents()
                val channels = repository.getChannels()
                _state.value = ExploreUiState(loading = false, events = events, channels = channels)
            }.onFailure {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }
}

private fun startOfDay(date: Date): Date {
    val calendar = Calendar.getInstance()
    calendar.time = date
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
}

@Composable
fun ExploreScreen(
    refreshKey: Int,
    onOpenEvent: (Event) -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    androidx.compose.runtime.LaunchedEffect(refreshKey) { viewModel.refresh(refreshKey) }
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val now = Date()
    val today = startOfDay(now)
    val thirtyDaysAgo = Date(now.time - 30L * 24 * 60 * 60 * 1000)

    val upcoming = state.events.filter { event ->
        val start = parseApiDate(event.start)
        val end = parseApiDate(event.end)
        val startDay = start?.let { startOfDay(it) }
        (startDay != null && !startDay.before(today)) ||
            (start != null && end != null && !start.after(now) && !end.before(now))
    }
    val recent = state.events.filter { event ->
        val start = parseApiDate(event.start)
        val end = parseApiDate(event.end)
        val startDay = start?.let { startOfDay(it) }
        startDay != null && startDay.before(today) && !start!!.before(thirtyDaysAgo) && end != null && end.before(now)
    }
    val previous = state.events.filter { event ->
        val start = parseApiDate(event.start)
        start != null && start.before(thirtyDaysAgo)
    }

    val tabs = listOf("Upcoming" to upcoming, "Recent" to recent, "Previous" to previous)

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab, containerColor = DsBase, contentColor = DsCyan) {
            tabs.forEachIndexed { index, (title, _) ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }

        val (_, events) = tabs[selectedTab]
        when {
            state.loading -> FullScreenLoading("Loading events...")
            events.isEmpty() -> EmptyState(
                icon = FaIcon.CLOCK,
                title = "No ${tabs[selectedTab].first.lowercase()} events found",
                subtitle = ""
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                items(events) { event ->
                    ClubEventHeaderCard(
                        event = event,
                        formattedDate = formatLongDate(event.start),
                        onTap = { onOpenEvent(event) },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }
        }
    }
}
