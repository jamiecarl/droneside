package com.jamiecarl.droneside.ui.screens.live

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jamiecarl.droneside.data.live.AufpvClient
import com.jamiecarl.droneside.data.live.LiveClub
import com.jamiecarl.droneside.data.live.LiveSession
import com.jamiecarl.droneside.data.live.LiveSessionState
import com.jamiecarl.droneside.data.live.matchTracksideClub
import com.jamiecarl.droneside.data.repository.FpvTracksideRepository
import com.jamiecarl.droneside.data.storage.HomeClubStore
import com.jamiecarl.droneside.alerts.RaceAlertService
import com.jamiecarl.droneside.data.storage.LiveClubStore
import com.jamiecarl.droneside.data.storage.RaceAlertStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveUiState(
    val loadingClubs: Boolean = true,
    val clubsError: Boolean = false,
    val clubs: List<LiveClub> = emptyList(),
    val club: LiveClub? = null,
    /** Null follows the club's current event. */
    val pinnedEventId: Int? = null,
    val session: LiveSessionState = LiveSessionState(),
    val callsign: String? = null,
    /** Name of the club the background race alerts are watching, when they're on. */
    val alertsWatching: String? = null
)

class LiveViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FpvTracksideRepository()
    private val homeClubStore = HomeClubStore(application)
    private val liveClubStore = LiveClubStore(application)
    private val alertStore = RaceAlertStore(application)

    private val _state = MutableStateFlow(LiveUiState(callsign = alertStore.callsign))
    val state: StateFlow<LiveUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            RaceAlertService.running.collect { running ->
                _state.update { it.copy(alertsWatching = if (running) alertStore.watchedClubName else null) }
            }
        }
    }

    fun setCallsign(callsign: String) {
        alertStore.callsign = callsign
        _state.update { it.copy(callsign = alertStore.callsign) }
        RaceAlertService.restartIfRunning(getApplication())
    }

    /** Call once notification permission is sorted. */
    fun startAlerts() {
        val club = _state.value.club ?: return
        if (_state.value.callsign == null) return
        RaceAlertService.stop(getApplication())
        RaceAlertService.start(getApplication(), club)
        _state.update { it.copy(alertsWatching = club.name) }
    }

    fun stopAlerts() {
        RaceAlertService.stop(getApplication())
    }

    private var session: LiveSession? = null
    private var sessionJob: Job? = null
    private var visible = false
    private var lastKey: Int? = null

    fun load(refreshKey: Int) {
        // The callsign may have been changed in Settings.
        _state.update { it.copy(callsign = alertStore.callsign) }
        if (lastKey == refreshKey) {
            // Re-entering the tab: another screen may have picked a club to watch.
            followStoredClub()
            return
        }
        val isRefresh = lastKey != null
        lastKey = refreshKey
        if (isRefresh && _state.value.club != null) {
            session?.requestRefresh()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loadingClubs = true, clubsError = false) }
            runCatching { AufpvClient.clubs() }
                .onSuccess { clubs ->
                    _state.update { it.copy(loadingClubs = false, clubs = clubs) }
                    if (_state.value.club == null) pickInitialClub(clubs)?.let(::selectClub)
                }
                .onFailure { _state.update { it.copy(loadingClubs = false, clubsError = true) } }
        }
    }

    private suspend fun pickInitialClub(clubs: List<LiveClub>): LiveClub? {
        liveClubStore.get()?.let { url -> clubs.firstOrNull { it.baseUrl == url }?.let { return it } }
        val homeId = homeClubStore.getHomeClub() ?: return null
        val homeName = runCatching { repository.getClub(homeId).name }.getOrNull()
        return clubs.matchTracksideClub(homeName)
    }

    private fun followStoredClub() {
        val url = liveClubStore.get() ?: return
        if (_state.value.club?.baseUrl == url) return
        _state.value.clubs.firstOrNull { it.baseUrl == url }?.let(::selectClub)
    }

    fun selectClub(club: LiveClub) {
        liveClubStore.set(club.baseUrl)
        _state.update { it.copy(club = club, pinnedEventId = null) }
        restartSession()
    }

    fun selectEvent(eventId: Int?) {
        _state.update { it.copy(pinnedEventId = eventId) }
        restartSession()
    }

    /** The stream and polling only run while the screen is on show. */
    fun onVisible() {
        visible = true
        if (sessionJob == null) restartSession()
    }

    fun onHidden() {
        visible = false
        sessionJob?.cancel()
        sessionJob = null
    }

    private var sessionTarget: Pair<String, Int?>? = null

    private fun restartSession() {
        sessionJob?.cancel()
        sessionJob = null
        val club = _state.value.club ?: return
        val target = club.baseUrl to _state.value.pinnedEventId
        // A new club or event starts blank; coming back to the same one keeps the last state on screen
        // until the fresh snapshot lands.
        if (target != sessionTarget) {
            sessionTarget = target
            _state.update { it.copy(session = LiveSessionState()) }
        }
        if (!visible) return
        val next = LiveSession(club.baseUrl, _state.value.pinnedEventId).also { session = it }
        sessionJob = viewModelScope.launch {
            launch {
                next.state.collect { s ->
                    if (s.snapshot != null || s.error != null) _state.update { it.copy(session = s) }
                }
            }
            next.run()
        }
    }

    fun refresh() {
        session?.requestRefresh()
    }

    override fun onCleared() {
        sessionJob?.cancel()
    }
}
