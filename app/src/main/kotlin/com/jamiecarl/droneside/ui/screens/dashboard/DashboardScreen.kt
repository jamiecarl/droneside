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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jamiecarl.droneside.data.model.Club
import com.jamiecarl.droneside.data.model.Event
import com.jamiecarl.droneside.data.repository.FpvTracksideRepository
import com.jamiecarl.droneside.data.storage.HomeClubStore
import com.jamiecarl.droneside.ui.components.ClubLogo
import com.jamiecarl.droneside.ui.components.DroneSideMark
import com.jamiecarl.droneside.ui.components.DroneSideWordmark
import com.jamiecarl.droneside.ui.components.DsIcons
import com.jamiecarl.droneside.ui.components.EmptyState
import com.jamiecarl.droneside.ui.components.EventCard
import com.jamiecarl.droneside.ui.components.FloatingNavClearance
import com.jamiecarl.droneside.ui.components.FullScreenLoading
import com.jamiecarl.droneside.ui.components.SectionHeader
import com.jamiecarl.droneside.ui.components.SectionLabel
import com.jamiecarl.droneside.ui.components.dsCard
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsCyanBorder
import com.jamiecarl.droneside.ui.theme.DsCyanSurface
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.util.parseApiDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.jamiecarl.droneside.data.live.AufpvClient
import com.jamiecarl.droneside.data.live.LiveClub
import com.jamiecarl.droneside.data.live.matchTracksideClub
import com.jamiecarl.droneside.ui.components.LiveTimingLink
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardUiState(
    val loading: Boolean = false,
    val club: Club? = null,
    val events: List<Event> = emptyList(),
    /** The home club's AUFPV site, if it runs live timing there. */
    val liveClub: LiveClub? = null
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
                _state.value = DashboardUiState(loading = false, club = club, events = events)
                runCatching { AufpvClient.clubs().matchTracksideClub(club.name) }.getOrNull()?.let { live ->
                    _state.value = _state.value.copy(liveClub = live)
                }
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
    onSeeAll: () -> Unit,
    onOpenLive: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    // Re-runs whenever the tab is re-entered, so a newly starred home club shows up immediately.
    LaunchedEffect(refreshKey) { viewModel.refresh() }
    val state by viewModel.state.collectAsState()
    val now = Date()
    val thirtyDaysAgo = Date(now.time - 30L * 24 * 60 * 60 * 1000)

    val recentAndUpcoming = state.events.filter { event ->
        val start = parseApiDate(event.start)
        val end = parseApiDate(event.end)
        (start != null && start.after(thirtyDaysAgo)) || (end != null && !end.before(now))
    }.take(5)
    val raceDay = state.liveClub?.let { state.events.firstOrNull { isOnToday(it, now) } }

    val club = state.club
    when {
        club != null -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = FloatingNavClearance),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dsCard()
                        .clickable { onOpenClub(club.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClubLogo(logoUrl = club.logoUrl, modifier = Modifier.size(44.dp))
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        SectionLabel("HOME CLUB")
                        Text(club.name, color = DsInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Icon(DsIcons.ChevronRight, contentDescription = null, tint = DsMuted, modifier = Modifier.size(18.dp))
                }
            }
            val live = state.liveClub
            if (raceDay != null && live != null) {
                item {
                    LiveTimingLink(
                        title = raceDay.name,
                        subtitle = "RACE DAY · WATCH LIVE TIMING",
                        onClick = { onOpenLive(live.baseUrl) }
                    )
                }
            }
            item {
                SectionHeader(
                    "Upcoming & recent",
                    modifier = Modifier.padding(top = 4.dp),
                    action = "See all",
                    onAction = onSeeAll
                )
            }
            if (recentAndUpcoming.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                        EmptyState(DsIcons.Clock, "No events to show", "Check the Explore tab for more events")
                    }
                }
            } else {
                items(recentAndUpcoming, key = { it.id }) { event ->
                    EventCard(event = event, onTap = { onOpenEvent(event) }, showClub = false)
                }
            }
        }
        state.loading -> FullScreenLoading("Loading your home club...")
        else -> WelcomeState(onExplore = onSeeAll)
    }
}

/** Starts today, or is running now (multi-day events). */
private fun isOnToday(event: Event, now: Date): Boolean {
    val start = parseApiDate(event.start) ?: return false
    val end = parseApiDate(event.end)
    if (end != null && !start.after(now) && !end.before(now)) return true
    val day = SimpleDateFormat("yyyyMMdd", Locale.US)
    return day.format(start) == day.format(now)
}

@Composable
private fun WelcomeState(onExplore: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, bottom = FloatingNavClearance),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DroneSideMark(size = 96.dp)
        Text(
            "Welcome to",
            color = DsMuted,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 20.dp)
        )
        DroneSideWordmark(fontSize = 34.sp, modifier = Modifier.padding(bottom = 10.dp))
        Text(
            "Star a club from the Clubs tab to see its events here, or browse everything in Explore.",
            color = DsMuted,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center
        )
        Text(
            "Browse events",
            color = DsCyan,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 20.dp)
                .dsCard(radius = 22.dp, background = DsCyanSurface, border = DsCyanBorder)
                .clickable(onClick = onExplore)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}
