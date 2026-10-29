package com.jamiecarl.droneside.ui.screens.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jamiecarl.droneside.data.live.LapFeedItem
import com.jamiecarl.droneside.data.live.LiveClub
import com.jamiecarl.droneside.data.live.LiveConnection
import com.jamiecarl.droneside.data.live.LiveEventRef
import com.jamiecarl.droneside.data.live.LiveMath
import com.jamiecarl.droneside.data.live.LiveRace
import com.jamiecarl.droneside.data.live.LiveRaceState
import com.jamiecarl.droneside.data.live.LiveSession
import com.jamiecarl.droneside.data.live.LiveSessionState
import com.jamiecarl.droneside.data.live.Standing
import com.jamiecarl.droneside.data.live.UpcomingRace
import com.jamiecarl.droneside.ui.components.ClubLogo
import com.jamiecarl.droneside.ui.components.DsIcons
import com.jamiecarl.droneside.ui.components.EmptyState
import com.jamiecarl.droneside.ui.components.FloatingNavClearance
import com.jamiecarl.droneside.ui.components.FullScreenLoading
import com.jamiecarl.droneside.ui.components.SectionLabel
import com.jamiecarl.droneside.ui.components.dsCard
import com.jamiecarl.droneside.ui.screens.rounddetails.parseHexColor
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsCyanBorder
import com.jamiecarl.droneside.ui.theme.DsCyanSurface
import com.jamiecarl.droneside.ui.theme.DsDnf
import com.jamiecarl.droneside.ui.theme.DsFaint
import com.jamiecarl.droneside.ui.theme.DsHoleshot
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsInk2
import com.jamiecarl.droneside.ui.theme.DsLine
import com.jamiecarl.droneside.ui.theme.DsLive
import com.jamiecarl.droneside.ui.theme.DsLiveBorder
import com.jamiecarl.droneside.ui.theme.DsLiveInk
import com.jamiecarl.droneside.ui.theme.DsLiveSurface
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.DsNavBorder
import com.jamiecarl.droneside.ui.theme.DsRacingInk
import com.jamiecarl.droneside.ui.theme.DsRaised
import com.jamiecarl.droneside.ui.theme.DsSparkLine
import com.jamiecarl.droneside.ui.theme.DsSurface
import com.jamiecarl.droneside.ui.theme.DsSurface2
import com.jamiecarl.droneside.ui.theme.MonoDs
import kotlinx.coroutines.delay

@Composable
fun LiveScreen(refreshKey: Int, viewModel: LiveViewModel = viewModel()) {
    LaunchedEffect(refreshKey) { viewModel.load(refreshKey) }
    LifecycleStartEffect(viewModel) {
        viewModel.onVisible()
        onStopOrDispose { viewModel.onHidden() }
    }
    val state by viewModel.state.collectAsState()
    val club = state.club

    when {
        club == null && state.loadingClubs -> FullScreenLoading("Finding live timing...")
        club == null && state.clubsError -> EmptyState(
            DsIcons.Live,
            "Couldn't reach AUFPV",
            "Check your connection and tap refresh",
            modifier = Modifier.padding(bottom = FloatingNavClearance)
        )
        club == null -> ClubPicker(state.clubs, onSelect = viewModel::selectClub)
        else -> LiveContent(
            state = state,
            club = club,
            onSelectClub = viewModel::selectClub,
            onSelectEvent = viewModel::selectEvent
        )
    }
}

@Composable
private fun ClubPicker(clubs: List<LiveClub>, onSelect: (LiveClub) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = FloatingNavClearance),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Live timing comes from AUFPV club sites. Pick a club to watch.",
                color = DsMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
        items(clubs, key = { it.baseUrl }) { club ->
            Row(
                modifier = Modifier.fillMaxWidth().dsCard().clickable { onSelect(club) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClubLogo(club.logoUrl, modifier = Modifier.size(40.dp), cornerRadius = 12.dp)
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(club.name, color = DsInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(club.baseUrl.substringAfter("://"), fontFamily = MonoDs, fontSize = 11.sp, color = DsMuted)
                }
                Icon(DsIcons.ChevronRight, contentDescription = null, tint = DsMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun LiveContent(
    state: LiveUiState,
    club: LiveClub,
    onSelectClub: (LiveClub) -> Unit,
    onSelectEvent: (Int?) -> Unit
) {
    val session = state.session
    val snapshot = session.snapshot
    val current = snapshot?.current
    val standings = remember(current) { current?.let(LiveMath::standings).orEmpty() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = FloatingNavClearance),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "club") {
            ClubBar(club, state.clubs, session.connection.takeIf { snapshot != null }, onSelectClub)
        }
        if (snapshot != null) {
            item(key = "event") {
                EventBar(snapshot.eventName, snapshot.events, snapshot.eventId, state.pinnedEventId, onSelectEvent)
            }
        }

        when {
            snapshot == null && session.error != null -> item(key = "error") {
                MessageCard("Couldn't load the live dashboard", "${session.error}. Retrying...")
            }
            snapshot == null -> item(key = "loading") {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DsCyan, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                }
            }
            else -> {
                if (current != null) {
                    item(key = "clock") { RaceClockCard(current, standings, session) }
                    item(key = "order-header") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (current.state == LiveRaceState.PENDING) "Line-up" else "Running order",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DsInk,
                                modifier = Modifier.weight(1f)
                            )
                            SectionLabel(if (current.isTimeTrial) "LAST · BEST ${current.consecWindow}" else "LAST · GAP")
                        }
                    }
                    val fastest = LiveMath.fastestLap(current)
                    items(standings, key = { "p${it.pilot.id}" }) { standing ->
                        StandingRow(standing, current, fastest, modifier = Modifier.animateItem())
                    }
                } else {
                    item(key = "no-race") { MessageCard("No race on right now", "The next heat shows here once it's set up.") }
                }

                if (session.feed.isNotEmpty()) {
                    item(key = "feed") { LapFeed(session.feed) }
                }
                snapshot.upcoming.take(2).forEachIndexed { i, race ->
                    item(key = "up$i") { UpNextCard(race, isNext = i == 0) }
                }
                items(snapshot.finished, key = { "r${it.id}" }) { race -> ResultCard(race) }
            }
        }
    }
}

@Composable
private fun ClubBar(club: LiveClub, clubs: List<LiveClub>, connection: LiveConnection?, onSelect: (LiveClub) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .dsCard()
                .clickable(role = Role.Button, onClickLabel = "Change club") { open = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClubLogo(club.logoUrl, modifier = Modifier.size(36.dp), cornerRadius = 11.dp)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                SectionLabel("WATCHING")
                Text(club.name, color = DsInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (connection != null) ConnectionPill(connection)
            Icon(DsIcons.ChevronDown, contentDescription = null, tint = DsMuted, modifier = Modifier.padding(start = 8.dp).size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(DsSurface)) {
            clubs.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.name, color = if (c.baseUrl == club.baseUrl) DsCyan else DsInk2, fontSize = 14.sp) },
                    onClick = { open = false; onSelect(c) }
                )
            }
        }
    }
}

private data class PillStyle(val label: String, val ink: Color, val dot: Color, val surface: Color, val border: Color)

@Composable
private fun ConnectionPill(connection: LiveConnection) {
    val style = when (connection) {
        LiveConnection.LIVE -> PillStyle("LIVE", DsLiveInk, DsLive, DsLiveSurface, DsLiveBorder)
        LiveConnection.CONNECTING -> PillStyle("CONNECTING", DsMuted, DsMuted, DsSurface2, DsLine)
        LiveConnection.RECONNECTING -> PillStyle("RECONNECTING", DsHoleshot, DsHoleshot, DsSurface2, DsLine)
    }
    Row(
        modifier = Modifier
            .background(style.surface, RoundedCornerShape(999.dp))
            .border(1.dp, style.border, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).background(style.dot, CircleShape))
        Text(
            style.label,
            fontFamily = MonoDs,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            color = style.ink,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun EventBar(
    eventName: String,
    events: List<LiveEventRef>,
    eventId: Int,
    pinnedEventId: Int?,
    onSelect: (Int?) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClickLabel = "Switch event") { open = true }
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(eventName, color = DsInk2, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    if (pinnedEventId == null) "CURRENT EVENT" else "PAST EVENT",
                    fontFamily = MonoDs,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = DsMuted
                )
            }
            Text("Switch", color = DsCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(DsSurface)) {
            DropdownMenuItem(
                text = { Text("Follow current event", color = if (pinnedEventId == null) DsCyan else DsInk2, fontSize = 14.sp) },
                onClick = { open = false; onSelect(null) }
            )
            events.forEach { e ->
                DropdownMenuItem(
                    text = {
                        Text(
                            e.name + if (e.live) "  ● live" else "",
                            color = if (pinnedEventId == e.id || (pinnedEventId == null && eventId == e.id)) DsCyan else DsInk2,
                            fontSize = 14.sp
                        )
                    },
                    onClick = { open = false; onSelect(e.id) }
                )
            }
        }
    }
}

/** Ticks while [active], for the race clock. */
@Composable
private fun rememberNow(active: Boolean): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active) {
        now = System.currentTimeMillis()
        while (active) {
            delay(100)
            now = System.currentTimeMillis()
        }
    }
    return now
}

@Composable
private fun RaceClockCard(race: LiveRace, standings: List<Standing>, session: LiveSessionState) {
    val lastRunning = session.lastRunningAt[race.id]
    val holding = lastRunning != null && System.currentTimeMillis() - lastRunning < LiveSession.TIMER_HOLD_MS
    val running = race.startMs > 0 && (race.state == LiveRaceState.RUNNING || holding)
    val now = rememberNow(running)
    val clock = when {
        running -> (now - session.clockSkewMs - race.startMs) / 1000.0
        race.state == LiveRaceState.ENDED -> standings.mapNotNull { it.total }.maxOrNull() ?: 0.0
        else -> 0.0
    }
    val (statusText, statusInk, dot) = when {
        running -> Triple("RACING", DsRacingInk, DsDnf)
        race.state == LiveRaceState.ENDED -> Triple("FINISHED", DsMuted, DsFaint)
        else -> Triple("ON DECK", DsCyan, DsCyan)
    }
    val fastest = LiveMath.fastestLap(race)
    val leader = standings.firstOrNull { it.position == 1 }

    Column(modifier = Modifier.fillMaxWidth().dsCard(radius = 24.dp).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(8.dp).background(dot, CircleShape)
            )
            Text(
                statusText,
                fontFamily = MonoDs,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = statusInk,
                modifier = Modifier.padding(start = 8.dp).weight(1f)
            )
            SectionLabel(
                if (race.isTimeTrial) "TIME TRIAL · BEST ${race.consecWindow}"
                else if (race.targetLaps > 0) "RACE TO ${race.targetLaps} LAPS" else race.heatType.uppercase()
            )
        }
        Text(
            race.title.ifEmpty { race.label },
            color = DsInk2,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
            Text(
                LiveMath.formatClock(clock),
                fontFamily = MonoDs,
                fontSize = 46.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-1.5).sp,
                color = if (running) DsCyan else DsInk,
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.End) {
                SectionLabel("FASTEST LAP")
                Text(
                    fastest?.let(LiveMath::formatLap) ?: "—",
                    fontFamily = MonoDs,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (fastest != null) DsHoleshot else DsMuted
                )
            }
        }
        if (!race.isTimeTrial && race.targetLaps > 0) {
            val done = leader?.lapsDone ?: 0
            Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(race.targetLaps) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(
                                when {
                                    i < done -> DsCyan
                                    i == done && running -> DsSparkLine
                                    else -> DsLine
                                },
                                RoundedCornerShape(3.dp)
                            )
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                SectionLabel(
                    if (leader == null) "WAITING FOR HOLESHOT"
                    else if (done >= race.targetLaps) "LEADER FINISHED" else "LEADER ON LAP ${done + 1}",
                    modifier = Modifier.weight(1f)
                )
                if (leader != null && done < race.targetLaps) SectionLabel("${race.targetLaps - done} TO GO")
            }
        }
    }
}

private fun initials(name: String): String {
    val words = name.split(' ', '_', '-').filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}"
        name.length >= 2 -> name.take(2)
        else -> name
    }.uppercase()
}

@Composable
private fun StandingRow(standing: Standing, race: LiveRace, fastest: Double?, modifier: Modifier = Modifier) {
    val leader = standing.position == 1 && standing.pilot.laps.isNotEmpty()
    val channelColor = parseHexColor(standing.pilot.color) ?: DsFaint
    Row(
        modifier = modifier
            .fillMaxWidth()
            .dsCard(
                radius = 16.dp,
                background = if (leader) DsCyanSurface else DsSurface,
                border = if (leader) DsCyanBorder else DsLine
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            standing.position?.toString() ?: "—",
            fontFamily = MonoDs,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (leader) DsCyan else DsInk2,
            modifier = Modifier.width(22.dp)
        )
        Box(
            modifier = Modifier.size(34.dp).background(DsRaised, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(initials(standing.pilot.name), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DsInk2)
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(vertical = 6.dp)
                    .width(3.dp)
                    .height(22.dp)
                    .background(channelColor, RoundedCornerShape(2.dp))
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(standing.pilot.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DsInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(standing.pilot.channel, fontFamily = MonoDs, fontSize = 10.sp, color = DsMuted)
                val segments = if (race.isTimeTrial) maxOf(standing.lapsDone, 1).coerceAtMost(12) else race.targetLaps
                Row(modifier = Modifier.padding(start = 6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(segments) { i ->
                        Box(
                            Modifier
                                .width(10.dp)
                                .height(4.dp)
                                .background(if (i < standing.lapsDone) DsCyan else DsNavBorder, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val last = standing.lastLap
            Text(
                last?.let(LiveMath::formatLap) ?: standing.pilot.laps[0]?.let { "HS " + LiveMath.formatLap(it) } ?: "—",
                fontFamily = MonoDs,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (last != null && fastest != null && last <= fastest) DsHoleshot else DsInk2
            )
            Text(
                secondaryValue(standing, race),
                fontFamily = MonoDs,
                fontSize = 11.sp,
                color = DsMuted
            )
        }
    }
}

private fun secondaryValue(standing: Standing, race: LiveRace): String = when {
    race.isTimeTrial -> standing.bestConsec?.let(LiveMath::formatLap)
        ?: standing.pilot.personalBest?.let { "PB " + LiveMath.formatLap(it) } ?: "—"
    standing.isLeader -> "LEADER"
    standing.gapToLeader != null -> LiveMath.formatGap(standing.gapToLeader)
    else -> standing.pilot.qualifying ?: ""
}

@Composable
private fun LapFeed(feed: List<LapFeedItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .dsCard(radius = 18.dp, background = DsSurface2)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionLabel("LAP FEED")
        feed.take(5).forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.raceClock?.let { LiveMath.formatClock(it).substringBefore('.') } ?: "",
                    fontFamily = MonoDs,
                    fontSize = 11.sp,
                    color = DsMuted,
                    modifier = Modifier.width(44.dp)
                )
                Box(Modifier.width(3.dp).height(14.dp).background(parseHexColor(item.color) ?: DsFaint, RoundedCornerShape(2.dp)))
                Text(
                    "${item.pilotName} · ${if (item.lapNumber == 0) "holeshot" else "lap ${item.lapNumber}"}",
                    fontSize = 13.sp,
                    color = DsInk2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                )
                val tag = when {
                    item.isRaceBest -> " FASTEST"
                    item.isPersonalBest -> " PB"
                    else -> ""
                }
                Text(
                    LiveMath.formatLap(item.seconds) + tag,
                    fontFamily = MonoDs,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (tag.isNotEmpty()) DsHoleshot else DsInk2
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun UpNextCard(race: UpcomingRace, isNext: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .dsCard(radius = 20.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(if (isNext) "UP NEXT" else "AFTER THAT", modifier = Modifier.weight(1f))
            SectionLabel("${race.pilots.size} PILOTS")
        }
        Text(race.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DsInk)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            race.pilots.forEach { p ->
                Row(
                    modifier = Modifier
                        .background(DsSurface2, RoundedCornerShape(10.dp))
                        .border(1.dp, DsLine, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).background(parseHexColor(p.color) ?: DsFaint, RoundedCornerShape(2.dp)))
                    Text(p.name, fontSize = 12.sp, color = DsInk2, modifier = Modifier.padding(start = 6.dp))
                    Text(p.channel, fontFamily = MonoDs, fontSize = 10.sp, color = DsMuted, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun ResultCard(race: LiveRace) {
    val standings = remember(race) { LiveMath.standings(race) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .dsCard(radius = 20.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(race.label.uppercase(), modifier = Modifier.weight(1f))
            SectionLabel(if (race.isTimeTrial) "BEST ${race.consecWindow}" else "LAPS · TOTAL")
        }
        Text(race.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DsInk)
        standings.forEachIndexed { i, s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (s.position ?: (i + 1)).toString(),
                    fontFamily = MonoDs,
                    fontSize = 12.sp,
                    color = if (i == 0) DsCyan else DsMuted,
                    modifier = Modifier.width(22.dp)
                )
                Box(Modifier.width(3.dp).height(14.dp).background(parseHexColor(s.pilot.color) ?: DsFaint, RoundedCornerShape(2.dp)))
                Text(
                    s.pilot.name,
                    fontSize = 13.sp,
                    color = DsInk2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                )
                Text(
                    if (race.isTimeTrial) s.bestConsec?.let(LiveMath::formatLap) ?: "—"
                    else "${s.lapsDone}  " + (s.total?.let(LiveMath::formatLap) ?: "—"),
                    fontFamily = MonoDs,
                    fontSize = 12.sp,
                    color = DsInk2
                )
            }
        }
    }
}

@Composable
private fun MessageCard(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().dsCard(radius = 20.dp).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DsInk)
        Text(subtitle, fontSize = 13.sp, color = DsMuted, lineHeight = 18.sp)
    }
}
