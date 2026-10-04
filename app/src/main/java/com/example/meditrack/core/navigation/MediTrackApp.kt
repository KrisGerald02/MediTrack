package com.example.meditrack.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.meditrack.core.presentation.MediTrackViewModel
import com.example.meditrack.feature.activity.ActivityScreen
import com.example.meditrack.feature.home.HomeScreen
import com.example.meditrack.feature.dashboard.DashboardScreen
import com.example.meditrack.feature.inventory.InventoryScreen
import com.example.meditrack.feature.medications.MedicationFormScreen
import com.example.meditrack.feature.medications.TreatmentsScreen
import com.example.meditrack.feature.settings.SettingsScreen
import com.example.meditrack.feature.alerts.AlertsScreen

private data class NavItem(val destination: Destination, val icon: ImageVector)

private val mainItems = listOf(
    NavItem(Destination.Home, Icons.Default.CalendarMonth),
    NavItem(Destination.Dashboard, Icons.Default.Insights),
    NavItem(Destination.Treatments, Icons.Default.Medication),
    NavItem(Destination.Inventory, Icons.Default.Inventory2)
)

@Composable
fun MediTrackApp(viewModel: MediTrackViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination
    val isMainDestination = mainItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.destination.route } == true
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 700.dp
        val content: @Composable (Modifier) -> Unit = { modifier ->
            NavHost(
                navController = navController,
                startDestination = Destination.Home.route,
                modifier = modifier
            ) {
                composable(Destination.Home.route) {
                    HomeScreen(viewModel, onSettings = { navController.navigate(Destination.Settings.route) })
                }
                composable(Destination.Dashboard.route) { ActivityScreen(viewModel) }
                composable(Destination.Treatments.route) {
                    TreatmentsScreen(
                        viewModel = viewModel,
                        onAdd = { navController.navigate(Destination.AddMedication.route()) },
                        onEdit = { navController.navigate(Destination.AddMedication.route(it)) },
                        onAlerts = { navController.navigate(Destination.Alerts.route) }
                    )
                }
                composable(Destination.Inventory.route) { InventoryScreen(viewModel) }
                composable(Destination.Settings.route) {
                    SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable(Destination.Alerts.route) {
                    AlertsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable(
                    route = Destination.AddMedication.route,
                    arguments = listOf(navArgument("id") { defaultValue = 0L; type = androidx.navigation.NavType.LongType })
                ) { entry ->
                    MedicationFormScreen(
                        medicationId = entry.arguments?.getLong("id") ?: 0,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        if (expanded && isMainDestination) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail {
                    mainItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.destination.route } == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTopLevel(navController, item.destination.route) },
                            icon = { Icon(item.icon, item.destination.label) },
                            label = { Text(item.destination.label, maxLines = 1, fontSize = 10.sp) }
                        )
                    }
                }
                content(Modifier.weight(1f))
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (isMainDestination) {
                        NavigationBar {
                            mainItems.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any { it.route == item.destination.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(navController, item.destination.route) },
                                    icon = { Icon(item.icon, item.destination.label) },
                                    label = { Text(item.destination.label, maxLines = 1, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            ) { padding -> content(Modifier.padding(padding)) }
        }
    }
}

private fun navigateTopLevel(navController: androidx.navigation.NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
