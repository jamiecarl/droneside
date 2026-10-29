package com.jamiecarl.droneside.ui.screens.clubdetails

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.jamiecarl.droneside.ui.components.ClubEventHeaderCard
import com.jamiecarl.droneside.ui.components.ClubLogo
import com.jamiecarl.droneside.ui.components.EmptyState
import com.jamiecarl.droneside.ui.components.FaIcon
import com.jamiecarl.droneside.ui.components.FontAwesome
import com.jamiecarl.droneside.ui.components.FullScreenLoading
import com.jamiecarl.droneside.ui.components.SectionLabel
import com.jamiecarl.droneside.ui.theme.DsBase
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsFaint
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsInk2
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.DsSurface
import com.jamiecarl.droneside.ui.theme.DsSurface2
import com.jamiecarl.droneside.ui.theme.MonoDs
import com.jamiecarl.droneside.util.formatLongDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab, clubId, refreshKey) {
        if (selectedTab == 1) viewModel.loadEvents(clubId)
    }

    when {
        state.loading -> FullScreenLoading("Loading club details...")
        state.club == null -> EmptyState(icon = FaIcon.USERS, title = "Unable to load club details", subtitle = "")
        else -> {
            val club = state.club!!
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp, 12.dp, 20.dp, 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ClubLogo(logoUrl = club.logoUrl, modifier = Modifier.size(56.dp))
                    Text(
                        text = club.name,
                        color = DsInk,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 14.dp).weight(1f)
                    )
                    Text(
                        text = FaIcon.STAR,
                        fontFamily = FontAwesome,
                        fontSize = 24.sp,
                        color = if (state.isHomeClub) DsCyan else DsFaint,
                        modifier = Modifier.clickable { viewModel.toggleHomeClub(clubId) }
                    )
                }

                TabRow(selectedTabIndex = selectedTab, containerColor = DsBase, contentColor = DsCyan) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Overview") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Events") })
                }

                if (selectedTab == 0) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DsSurface, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            SectionLabel("FOUNDED", modifier = Modifier.padding(bottom = 8.dp))
                            Text(text = formatLongDate(club.creation), color = DsInk2, fontSize = 16.sp)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .background(DsSurface, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            SectionLabel("LOCATION", modifier = Modifier.padding(bottom = 8.dp))
                            club.address?.let {
                                Text(text = it, color = DsInk2, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                            }
                            Text(text = "Timezone: ${club.timezone ?: "-"}", color = DsMuted, fontSize = 14.sp)
                            if (club.latitude != null && club.longitude != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .background(DsSurface2, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        SectionLabel("COORDINATES")
                                        Text(
                                            text = "%.6f, %.6f".format(club.latitude, club.longitude),
                                            fontFamily = MonoDs,
                                            fontSize = 12.sp,
                                            color = DsMuted,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    when {
                        state.loadingEvents -> FullScreenLoading("Loading events...")
                        state.events.isEmpty() -> EmptyState(icon = FaIcon.CLOCK, title = "No events found for this club", subtitle = "")
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(state.events) { event ->
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
        }
    }
}
