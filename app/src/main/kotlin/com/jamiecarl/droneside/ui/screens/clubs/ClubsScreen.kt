package com.jamiecarl.droneside.ui.screens.clubs

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jamiecarl.droneside.data.model.Club
import com.jamiecarl.droneside.data.repository.FpvTracksideRepository
import com.jamiecarl.droneside.ui.components.ClubLogo
import com.jamiecarl.droneside.ui.components.DsIcons
import com.jamiecarl.droneside.ui.components.EmptyState
import com.jamiecarl.droneside.ui.components.FloatingNavClearance
import com.jamiecarl.droneside.ui.components.FullScreenLoading
import com.jamiecarl.droneside.ui.components.SearchField
import com.jamiecarl.droneside.ui.components.dsCard
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.MonoDs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ClubsUiState(
    val loading: Boolean = true,
    val clubs: List<Club> = emptyList()
)

class ClubsViewModel : ViewModel() {
    private val repository = FpvTracksideRepository()

    private val _state = MutableStateFlow(ClubsUiState())
    val state: StateFlow<ClubsUiState> = _state.asStateFlow()

    private var lastKey: Int? = null

    fun refresh(refreshKey: Int) {
        if (lastKey == refreshKey) return
        lastKey = refreshKey
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching {
                _state.value = ClubsUiState(loading = false, clubs = repository.getClubs())
            }.onFailure {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }
}

@Composable
fun ClubsScreen(
    refreshKey: Int,
    onOpenClub: (String) -> Unit,
    viewModel: ClubsViewModel = viewModel()
) {
    LaunchedEffect(refreshKey) { viewModel.refresh(refreshKey) }
    val state by viewModel.state.collectAsState()
    var searchText by rememberSaveable { mutableStateOf("") }

    val term = searchText.trim().lowercase()
    val filteredClubs = if (term.isEmpty()) state.clubs else state.clubs.filter { it.name.lowercase().contains(term) }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = "Search clubs by name",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
        )

        when {
            state.loading -> FullScreenLoading("Loading clubs...")
            filteredClubs.isEmpty() && term.isNotEmpty() ->
                EmptyState(DsIcons.Search, "No clubs match \"${searchText.trim()}\"")
            filteredClubs.isEmpty() -> EmptyState(DsIcons.Users, "No clubs found")
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = FloatingNavClearance),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredClubs, key = { it.id }) { club ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .dsCard()
                            .clickable { onOpenClub(club.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ClubLogo(logoUrl = club.logoUrl, modifier = Modifier.size(44.dp))
                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(club.name, color = DsInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            club.timezone?.let { Text(it, fontFamily = MonoDs, fontSize = 11.sp, color = DsMuted) }
                        }
                        Icon(DsIcons.ChevronRight, contentDescription = null, tint = DsMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
