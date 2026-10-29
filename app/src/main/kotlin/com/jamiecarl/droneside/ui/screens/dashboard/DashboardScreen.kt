package com.jamiecarl.droneside.ui.screens.dashboard

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jamiecarl.droneside.data.model.Channel
import com.jamiecarl.droneside.data.model.Club
import com.jamiecarl.droneside.data.model.Event
import com.jamiecarl.droneside.data.repository.FpvTracksideRepository
import com.jamiecarl.droneside.data.storage.HomeClubStore
import com.jamiecarl.droneside.ui.components.ClubEventHeaderCard
import com.jamiecarl.droneside.ui.components.ClubLogo
import com.jamiecarl.droneside.ui.components.EmptyState
import com.jamiecarl.droneside.ui.components.FaIcon
import com.jamiecarl.droneside.ui.components.FontAwesome
import com.jamiecarl.droneside.ui.components.FullScreenLoading
import com.jamiecarl.droneside.ui.components.SectionLabel
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.MonoDs
import com.jamiecarl.droneside.util.formatLongDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

data class DashboardUiState(
    val loading: Boolean = false,
    val club: Club? = null,
    val events: List<Event> = emptyList(),
    val channels: List<Channel> = emptyList()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FpvTracksideRepository()
    private val homeClubStore = HomeClubStore(application)

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    fun refresh() {
        val homeClubId = homeClubStore.getHomeClub()
        if (homeClubId == null) {
            _state.value = DashboardUiState()
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching {
                val club = repository.getClub(homeClubId)
                val events = repository.getClubEvents(homeClubId)
                val channels = repository.getChannels()
                _state.value = DashboardUiState(loading = false, club = club, events = events, channels = channels)
            }.onFailure {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }
}

@Composable
fun DashboardScreen(
    refreshKey: Int,
    onOpenClub: (String) -> Unit,
    onOpenEvent: (Event) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    // Re-runs whenever the tab is re-entered, so a newly starred home club shows up immediately.
    androidx.compose.runtime.LaunchedEffect(refreshKey) { viewModel.refresh() }
    val state by viewModel.state.collectAsState()
    val now = Date()
    val thirtyDaysAgo = Date(now.time - 30L * 24 * 60 * 60 * 1000)

    val recentAndUpcoming = state.events.filter { event ->
        val start = com.jamiecarl.droneside.util.parseApiDate(event.start)
        val end = com.jamiecarl.droneside.util.parseApiDate(event.end)
        (start != null && start.after(thirtyDaysAgo)) || (end != null && !end.before(now))
    }.take(5)

    Box(modifier = Modifier.fillMaxSize().padding(0.dp)) {
        when {
            state.club == null && !state.loading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = FaIcon.HOME, fontFamily = FontAwesome, fontSize = 56.sp, color = com.jamiecarl.droneside.ui.theme.DsCyan)
                    Text(
                        text = "Welcome to Droneside",
                        color = DsInk,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
                    )
                    Text(
                        text = "Browse events in the Explore tab or select a club from the Clubs tab to see personalized content here.",
                        color = DsMuted,
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            state.club != null -> {
                val club = state.club!!
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { onOpenClub(club.id) })
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ClubLogo(logoUrl = club.logoUrl, modifier = Modifier.size(44.dp))
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            SectionLabel("HOME CLUB")
                            Text(text = club.name, color = DsInk, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(text = FaIcon.CHEVRON_RIGHT, fontFamily = FontAwesome, color = DsMuted, fontSize = 18.sp)
                    }

                    if (recentAndUpcoming.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            item { SectionLabel("UPCOMING & RECENT", modifier = Modifier.padding(bottom = 8.dp)) }
                            items(recentAndUpcoming) { event ->
                                ClubEventHeaderCard(
                                    event = event,
                                    formattedDate = formatLongDate(event.start),
                                    onTap = { onOpenEvent(event) },
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }
                        }
                    } else {
                        EmptyState(
                            icon = FaIcon.CLOCK,
                            title = "No events to show",
                            subtitle = "Check the Explore tab for more events"
                        )
                    }
                }
            }
            state.loading -> FullScreenLoading("Loading your home club...")
        }
    }
}
