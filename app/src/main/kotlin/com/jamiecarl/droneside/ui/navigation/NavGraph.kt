package com.jamiecarl.droneside.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jamiecarl.droneside.data.model.Event
import com.jamiecarl.droneside.data.storage.LiveClubStore
import com.jamiecarl.droneside.ui.screens.live.LiveScreen
import com.jamiecarl.droneside.ui.screens.settings.SettingsScreen
import com.jamiecarl.droneside.ui.components.DroneSideMark
import com.jamiecarl.droneside.ui.components.DroneSideWordmark
import com.jamiecarl.droneside.ui.components.DsIcons
import com.jamiecarl.droneside.ui.components.RoundIconButton
import com.jamiecarl.droneside.ui.screens.clubdetails.ClubDetailsScreen
import com.jamiecarl.droneside.ui.screens.clubs.ClubsScreen
import com.jamiecarl.droneside.ui.screens.dashboard.DashboardScreen
import com.jamiecarl.droneside.ui.screens.eventdetails.EventDetailsScreen
import com.jamiecarl.droneside.ui.screens.explore.ExploreScreen
import com.jamiecarl.droneside.ui.screens.pilotdetails.PilotDetailsScreen
import com.jamiecarl.droneside.ui.screens.racedetails.RaceDetailsScreen
import com.jamiecarl.droneside.ui.screens.rounddetails.RoundDetailsScreen
import com.jamiecarl.droneside.ui.theme.DsBase
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsCyanSurface
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.DsNavBorder
import com.jamiecarl.droneside.ui.theme.DsSurface
import com.jamiecarl.droneside.ui.theme.MonoDs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.DASHBOARD, "Home", DsIcons.Home),
    TabItem(Routes.LIVE, "Live", DsIcons.Live),
    TabItem(Routes.EXPLORE, "Explore", DsIcons.Search),
    TabItem(Routes.CLUBS, "Clubs", DsIcons.Users)
)

/** Home's header shows the logo and wordmark instead of a plain title. */
private const val BRAND_TITLE = "DroneSide"

private fun NavBackStackEntry.arg(name: String): String = decodeArg(arguments?.getString(name))

/** Small context line above the title, and the title, for each screen's header. */
private fun headerFor(entry: NavBackStackEntry?): Pair<String, String> = when (entry?.destination?.route) {
    Routes.DASHBOARD, null ->
        SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date()).uppercase() to BRAND_TITLE
    Routes.LIVE -> "AUFPV LIVE TIMING" to "Live"
    Routes.EXPLORE -> "" to "Explore"
    Routes.CLUBS -> "" to "Clubs"
    Routes.SETTINGS -> "" to "Settings"
    Routes.CLUB_DETAILS -> "" to "Club"
    Routes.EVENT_DETAILS -> "" to "Event"
    Routes.ROUND_DETAILS -> entry.arg("eventName") to "Round ${entry.arg("roundNumber")}"
    Routes.RACE_DETAILS -> entry.arg("eventName") to
        "Round ${entry.arg("roundNumber")} · Race ${entry.arguments?.getInt("raceNumber")}"
    Routes.PILOT_DETAILS -> entry.arg("eventName") to "Pilot"
    else -> "" to "DroneSide"
}

private fun NavHostController.openEvent(event: Event) =
    navigate(Routes.eventDetails(event.id, event.name, event.club.name ?: "Unknown Club"))

/** Points the Live tab at an AUFPV club site and switches to it. */
private fun NavHostController.openLive(context: Context, baseUrl: String) {
    LiveClubStore(context).set(baseUrl)
    switchTab(Routes.LIVE)
}

private fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun DronesideApp(openLiveRequests: Int = 0) {
    val navController = rememberNavController()
    LaunchedEffect(openLiveRequests) {
        if (openLiveRequests > 0) navController.switchTab(Routes.LIVE)
    }
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTabRoute = currentRoute == null || tabs.any { it.route == currentRoute }
    var refreshKey by remember { mutableIntStateOf(0) }
    val (headerContext, title) = headerFor(backStackEntry)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DsBase)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isTabRoute) {
                TabHeader(
                    context = headerContext,
                    title = title,
                    onRefresh = { refreshKey++ },
                    onSettings = { navController.navigate(Routes.SETTINGS) }
                )
            } else {
                DetailHeader(
                    context = headerContext,
                    title = title,
                    onBack = { navController.popBackStack() },
                    onRefresh = if (currentRoute == Routes.SETTINGS) null else ({ refreshKey++ })
                )
            }

            NavHost(
                navController = navController,
                startDestination = Routes.DASHBOARD,
                modifier = Modifier.weight(1f).let { if (isTabRoute) it else it.navigationBarsPadding() }
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardScreen(
                        refreshKey = refreshKey,
                        onOpenClub = { navController.navigate(Routes.clubDetails(it)) },
                        onOpenEvent = { navController.openEvent(it) },
                        onSeeAll = { navController.switchTab(Routes.EXPLORE) },
                        onOpenLive = { navController.openLive(context, it) }
                    )
                }
                composable(Routes.LIVE) {
                    LiveScreen(refreshKey = refreshKey, onOpenSettings = { navController.navigate(Routes.SETTINGS) })
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen()
                }
                composable(Routes.EXPLORE) {
                    ExploreScreen(refreshKey = refreshKey, onOpenEvent = { navController.openEvent(it) })
                }
                composable(Routes.CLUBS) {
                    ClubsScreen(refreshKey = refreshKey, onOpenClub = { navController.navigate(Routes.clubDetails(it)) })
                }
                composable(Routes.CLUB_DETAILS) { entry ->
                    ClubDetailsScreen(
                        clubId = entry.arg("clubId"),
                        refreshKey = refreshKey,
                        onOpenEvent = { navController.openEvent(it) },
                        onOpenLive = { navController.openLive(context, it) }
                    )
                }
                composable(
                    Routes.EVENT_DETAILS,
                    arguments = listOf(
                        navArgument("eventName") { defaultValue = "" },
                        navArgument("clubName") { defaultValue = "" }
                    )
                ) { entry ->
                    val eventId = entry.arg("eventId")
                    val eventName = entry.arg("eventName")
                    EventDetailsScreen(
                        eventId = eventId,
                        eventName = eventName,
                        clubName = entry.arg("clubName"),
                        refreshKey = refreshKey,
                        onOpenRound = { round ->
                            navController.navigate(
                                Routes.roundDetails(eventId, round.id, eventName, round.roundNumber, round.eventType)
                            )
                        },
                        onOpenPilot = { pilot ->
                            navController.navigate(Routes.pilotDetails(eventId, pilot.id, eventName))
                        }
                    )
                }
                composable(
                    Routes.ROUND_DETAILS,
                    arguments = listOf(
                        navArgument("eventName") { defaultValue = "" },
                        navArgument("roundNumber") { defaultValue = "" },
                        navArgument("eventType") { defaultValue = "" }
                    )
                ) { entry ->
                    val eventId = entry.arg("eventId")
                    val roundId = entry.arg("roundId")
                    val eventName = entry.arg("eventName")
                    val roundNumber = entry.arg("roundNumber")
                    val eventType = entry.arg("eventType")
                    RoundDetailsScreen(
                        eventId = eventId,
                        roundId = roundId,
                        eventType = eventType,
                        refreshKey = refreshKey,
                        onOpenRace = { race ->
                            navController.navigate(
                                Routes.raceDetails(
                                    eventId, roundId, race.id, eventName, roundNumber, race.raceNumber, eventType
                                )
                            )
                        }
                    )
                }
                composable(
                    Routes.RACE_DETAILS,
                    arguments = listOf(
                        navArgument("eventName") { defaultValue = "" },
                        navArgument("roundNumber") { defaultValue = "" },
                        navArgument("raceNumber") { type = NavType.IntType; defaultValue = 0 },
                        navArgument("eventType") { defaultValue = "" }
                    )
                ) { entry ->
                    val eventId = entry.arg("eventId")
                    val eventName = entry.arg("eventName")
                    RaceDetailsScreen(
                        eventId = eventId,
                        roundId = entry.arg("roundId"),
                        raceId = entry.arg("raceId"),
                        eventType = entry.arg("eventType"),
                        refreshKey = refreshKey,
                        onOpenPilot = { pilotId ->
                            navController.navigate(Routes.pilotDetails(eventId, pilotId, eventName))
                        }
                    )
                }
                composable(
                    Routes.PILOT_DETAILS,
                    arguments = listOf(navArgument("eventName") { defaultValue = "" })
                ) { entry ->
                    val eventId = entry.arg("eventId")
                    val eventName = entry.arg("eventName")
                    PilotDetailsScreen(
                        eventId = eventId,
                        pilotId = entry.arg("pilotId"),
                        refreshKey = refreshKey,
                        onOpenRace = { race, round ->
                            navController.navigate(
                                Routes.raceDetails(
                                    eventId, round.id, race.id, eventName, round.roundNumber, race.raceNumber, round.eventType
                                )
                            )
                        }
                    )
                }
            }
        }

        if (isTabRoute) {
            FloatingTabBar(
                currentRoute = currentRoute ?: Routes.DASHBOARD,
                onSelect = { navController.switchTab(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun TabHeader(context: String, title: String, onRefresh: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (context.isNotEmpty()) {
                Text(context, fontFamily = MonoDs, fontSize = 11.sp, letterSpacing = 1.5.sp, color = DsMuted)
            }
            if (title == BRAND_TITLE) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DroneSideMark(size = 34.dp)
                    DroneSideWordmark(fontSize = 28.sp, modifier = Modifier.padding(start = 10.dp))
                }
            } else {
                Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, color = DsInk)
            }
        }
        RoundIconButton(DsIcons.Settings, contentDescription = "Settings", onClick = onSettings)
        RoundIconButton(
            DsIcons.Refresh,
            contentDescription = "Refresh",
            onClick = onRefresh,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun DetailHeader(context: String, title: String, onBack: () -> Unit, onRefresh: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(DsIcons.ChevronLeft, contentDescription = "Back", onClick = onBack)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            if (context.isNotEmpty()) {
                Text(context, fontSize = 12.sp, color = DsMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DsInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onRefresh != null) RoundIconButton(DsIcons.Refresh, contentDescription = "Refresh", onClick = onRefresh)
    }
}

@Composable
private fun FloatingTabBar(currentRoute: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(24.dp, RoundedCornerShape(34.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .background(DsSurface.copy(alpha = 0.96f), RoundedCornerShape(34.dp))
            .border(1.dp, DsNavBorder, RoundedCornerShape(34.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val selected = tab.route == currentRoute
            val color = if (selected) DsCyan else DsMuted
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (selected) DsCyanSurface else Color.Transparent)
                    .semantics { this.selected = selected }
                    .clickable(role = Role.Tab) { onSelect(tab.route) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Text(
                    tab.label,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = color,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}
