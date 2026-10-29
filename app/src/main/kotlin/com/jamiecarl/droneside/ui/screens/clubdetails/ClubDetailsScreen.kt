package com.jamiecarl.droneside.ui.screens.clubdetails

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
import com.jamiecarl.droneside.ui.components.DsIcons
import com.jamiecarl.droneside.ui.components.EmptyState
import com.jamiecarl.droneside.ui.components.EventCard
import com.jamiecarl.droneside.ui.components.FullScreenLoading
import com.jamiecarl.droneside.ui.components.SectionLabel
import com.jamiecarl.droneside.ui.components.SegmentedControl
import com.jamiecarl.droneside.ui.components.dsCard
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsCyanBorder
import com.jamiecarl.droneside.ui.theme.DsCyanSurface
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsInk2
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.DsSurface2
import com.jamiecarl.droneside.ui.theme.MonoDs
import com.jamiecarl.droneside.util.formatLongDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class ClubDetailsUiState(
    val loading: Boolean = true,
    val club: Club? = null,
    val events: List<Event> = emptyList(),
    val loadingEvents: Boolean = false,
    val isHomeClub: Boolean = false
)

class ClubDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FpvTracksideRepository()
    private val homeClubStore = HomeClubStore(application)

    private val _state = MutableStateFlow(ClubDetailsUiState())
    val state: StateFlow<ClubDetailsUiState> = _state.asStateFlow()

    private var lastKey: Int? = null

    fun load(clubId: String, refreshKey: Int) {
        if (lastKey == refreshKey) return
        val isRefresh = lastKey != null
        lastKey = refreshKey
        _state.value = _state.value.copy(
            isHomeClub = homeClubStore.isHomeClub(clubId),
            events = if (isRefresh) emptyList() else _state.value.events
        )
        viewModelScope.launch {
            runCatching {
                val club = repository.getClub(clubId)
                _state.value = _state.value.copy(loading = false, club = club)
            }.onFailure {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun loadEvents(clubId: String) {
        if (_state.value.events.isNotEmpty() || _state.value.loadingEvents) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingEvents = true)
            runCatching {
                val events = repository.getClubEvents(clubId)
                _state.value = _state.value.copy(loadingEvents = false, events = events)
            }.onFailure {
                _state.value = _state.value.copy(loadingEvents = false)
            }
        }
    }

    fun toggleHomeClub(clubId: String) {
        if (_state.value.isHomeClub) {
            homeClubStore.removeHomeClub()
        } else {
            homeClubStore.setHomeClub(clubId)
        }
        _state.value = _state.value.copy(isHomeClub = homeClubStore.isHomeClub(clubId))
    }
}

@Composable
fun ClubDetailsScreen(
    clubId: String,
    refreshKey: Int,
    onOpenEvent: (Event) -> Unit,
    viewModel: ClubDetailsViewModel = viewModel()
) {
    LaunchedEffect(clubId, refreshKey) { viewModel.load(clubId, refreshKey) }
    val state by viewModel.state.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab, clubId, refreshKey) {
        if (selectedTab == 1) viewModel.loadEvents(clubId)
    }

    val club = state.club
    when {
        state.loading -> FullScreenLoading("Loading club details...")
        club == null -> EmptyState(DsIcons.Users, "Unable to load club details")
        else -> Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClubLogo(logoUrl = club.logoUrl, modifier = Modifier.size(56.dp), cornerRadius = 18.dp)
                Text(
                    club.name,
                    color = DsInk,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 27.sp,
                    modifier = Modifier.weight(1f).padding(horizontal = 14.dp)
                )
                HomeClubToggle(isHome = state.isHomeClub, onToggle = { viewModel.toggleHomeClub(clubId) })
            }

            SegmentedControl(
                options = listOf("Overview", "Events"),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (selectedTab == 0) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InfoCard(icon = DsIcons.Calendar, label = "FOUNDED") {
                        Text(formatLongDate(club.creation), color = DsInk2, fontSize = 16.sp)
                    }
                    InfoCard(icon = DsIcons.MapPin, label = "LOCATION") {
                        club.address?.takeIf { it.isNotBlank() }?.let {
                            Text(it.trim(), color = DsInk2, fontSize = 15.sp, lineHeight = 21.sp)
                        }
                        Text("Timezone: ${club.timezone ?: "-"}", color = DsMuted, fontSize = 13.sp)
                        if (club.latitude != null && club.longitude != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .dsCard(radius = 14.dp, background = DsSurface2)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SectionLabel("COORDINATES")
                                Text(
                                    String.format(Locale.US, "%.6f, %.6f", club.latitude, club.longitude),
                                    fontFamily = MonoDs,
                                    fontSize = 12.sp,
                                    color = DsInk2
                                )
                            }
                        }
                    }
                }
            } else {
                when {
                    state.loadingEvents -> FullScreenLoading("Loading events...")
                    state.events.isEmpty() -> EmptyState(DsIcons.Calendar, "No events found for this club")
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.events, key = { it.id }) { event ->
                            EventCard(event = event, onTap = { onOpenEvent(event) }, showClub = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeClubToggle(isHome: Boolean, onToggle: () -> Unit) {
    val label = if (isHome) "Remove as home club" else "Set as home club"
    Box(
        modifier = Modifier
            .size(44.dp)
            .let {
                if (isHome) it.dsCard(radius = 22.dp, background = DsCyanSurface, border = DsCyanBorder)
                else it.dsCard(radius = 22.dp)
            }
            .clickable(role = Role.Switch, onClickLabel = label, onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isHome) DsIcons.StarFilled else DsIcons.Star,
            contentDescription = label,
            tint = if (isHome) DsCyan else DsMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun InfoCard(icon: ImageVector, label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().dsCard(radius = 22.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = DsMuted, modifier = Modifier.size(14.dp))
            SectionLabel(label)
        }
        content()
    }
}
