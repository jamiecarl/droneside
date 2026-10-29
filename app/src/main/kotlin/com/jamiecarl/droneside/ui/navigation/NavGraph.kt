package com.jamiecarl.droneside.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.jamiecarl.droneside.ui.components.FaIcon
import com.jamiecarl.droneside.ui.components.FontAwesome
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
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsLine
import com.jamiecarl.droneside.ui.theme.DsMuted2

private data class TabItem(val route: String, val label: String, val icon: String)

private val tabs = listOf(
    TabItem(Routes.DASHBOARD, "Home", FaIcon.HOME),
    TabItem(Routes.EXPLORE, "Explore", FaIcon.SEARCH),
    TabItem(Routes.CLUBS, "Clubs", FaIcon.USERS)
)

private fun NavBackStackEntry.arg(name: String): String = decodeArg(arguments?.getString(name))

private fun titleFor(entry: NavBackStackEntry?): String {
    return when (entry?.destination?.route) {
        Routes.DASHBOARD, null -> "DroneSide"
        Routes.EXPLORE -> "Explore Events"
        Routes.CLUBS -> "Clubs"
        Routes.CLUB_DETAILS -> "Club"
        Routes.EVENT_DETAILS -> "Event"
        Routes.ROUND_DETAILS -> "Round ${entry.arg("roundNumber")}"
        Routes.RACE_DETAILS -> "Round ${entry.arg("roundNumber")} · Race ${entry.arguments?.getInt("raceNumber")}"
        Routes.PILOT_DETAILS -> "Pilot"
        else -> "DroneSide"
    }
}

private fun NavHostController.openEvent(event: Event) =
    navigate(Routes.eventDetails(event.id, event.name, event.club.name ?: "Unknown Club"))

@Composable
fun DronesideApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTabRoute = tabs.any { it.route == currentRoute } || currentRoute == null
    var refreshKey by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DsBase)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isTabRoute) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DsInk)
                }
            } else {
                Box(Modifier.padding(start = 12.dp))
            }
            Text(
                text = titleFor(backStackEntry),
                color = DsInk,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { refreshKey++ }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = DsInk)
            }
        }

        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.weight(1f)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    refreshKey = refreshKey,
                    onOpenClub = { navController.navigate(Routes.clubDetails(it)) },
                    onOpenEvent = { navController.openEvent(it) }
                )
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
                    onOpenEvent = { navController.openEvent(it) }
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
                    onOpenPilot = { pilot -> navController.navigate(Routes.pilotDetails(eventId, pilot.id)) }
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
                val roundNumber = entry.arg("roundNumber")
                val eventType = entry.arg("eventType")
                RoundDetailsScreen(
                    eventId = eventId,
                    roundId = roundId,
                    eventName = entry.arg("eventName"),
                    roundNumber = roundNumber,
                    eventType = eventType,
                    refreshKey = refreshKey,
                    onOpenRace = { race ->
                        navController.navigate(
                            Routes.raceDetails(eventId, roundId, race.id, roundNumber, race.raceNumber, eventType)
                        )
                    }
                )
            }
            composable(
                Routes.RACE_DETAILS,
                arguments = listOf(
                    navArgument("roundNumber") { defaultValue = "" },
                    navArgument("raceNumber") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("eventType") { defaultValue = "" }
                )
            ) { entry ->
                val eventId = entry.arg("eventId")
                RaceDetailsScreen(
                    eventId = eventId,
                    roundId = entry.arg("roundId"),
                    raceId = entry.arg("raceId"),
                    eventType = entry.arg("eventType"),
                    refreshKey = refreshKey,
                    onOpenPilot = { pilotId -> navController.navigate(Routes.pilotDetails(eventId, pilotId)) }
                )
            }
            composable(Routes.PILOT_DETAILS) { entry ->
                val eventId = entry.arg("eventId")
                PilotDetailsScreen(
                    eventId = eventId,
                    pilotId = entry.arg("pilotId"),
                    refreshKey = refreshKey,
                    onOpenRace = { race, round ->
                        navController.navigate(
                            Routes.raceDetails(eventId, round.id, race.id, round.roundNumber, race.raceNumber, round.eventType)
                        )
                    }
                )
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(DsLine))
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            tabs.forEach { tab ->
                val selected = backStackEntry?.destination?.route == tab.route ||
                    (isTabRoute && currentRoute == null && tab.route == Routes.DASHBOARD)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val color = if (selected) DsCyan else DsMuted2
                    Text(tab.icon, fontFamily = FontAwesome, fontSize = 20.sp, color = color)
                    Text(tab.label, fontSize = 11.sp, color = color, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
