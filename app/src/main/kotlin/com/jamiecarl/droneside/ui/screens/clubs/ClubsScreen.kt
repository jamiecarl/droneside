package com.jamiecarl.droneside.ui.screens.clubs

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.jamiecarl.droneside.ui.components.EmptyState
import com.jamiecarl.droneside.ui.components.FaIcon
import com.jamiecarl.droneside.ui.components.FontAwesome
import com.jamiecarl.droneside.ui.components.FullScreenLoading
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsFaint
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsLine
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
    androidx.compose.runtime.LaunchedEffect(refreshKey) { viewModel.refresh(refreshKey) }
    val state by viewModel.state.collectAsState()
    var searchText by remember { mutableStateOf("") }

    val filteredClubs = if (searchText.isBlank()) {
        state.clubs
    } else {
        state.clubs.filter { it.name.lowercase().contains(searchText.lowercase().trim()) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, if (searchText.isNotBlank()) DsCyan else DsLine, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = DsInk, fontSize = 16.sp),
                    decorationBox = { inner ->
                        if (searchText.isEmpty()) {
                            Text("Search clubs by name...", color = DsMuted, fontSize = 16.sp)
                        }
                        inner()
                    }
                )
            }
            Text(
                text = FaIcon.TIMES,
                fontFamily = FontAwesome,
                color = if (searchText.isNotBlank()) DsCyan else DsFaint,
                fontSize = 18.sp,
                modifier = Modifier.clickable { searchText = "" }
            )
        }

        when {
            state.loading -> FullScreenLoading("Loading clubs...")
            filteredClubs.isEmpty() && searchText.isNotBlank() -> EmptyState(
                icon = FaIcon.SEARCH,
                title = "No clubs found matching \"$searchText\"",
                subtitle = ""
            )
            filteredClubs.isEmpty() -> EmptyState(icon = FaIcon.USERS, title = "No clubs found", subtitle = "")
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(filteredClubs) { club ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .border(1.dp, DsLine, RoundedCornerShape(16.dp))
                            .clickable { onOpenClub(club.id) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ClubLogo(logoUrl = club.logoUrl, modifier = Modifier.size(44.dp))
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(text = club.name, color = DsInk, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = club.timezone ?: "", fontFamily = MonoDs, fontSize = 12.sp, color = DsMuted)
                        }
                    }
                }
            }
        }
    }
}
