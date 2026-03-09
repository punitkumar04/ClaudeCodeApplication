package com.punitkumar.gruhkharch.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.punitkumar.gruhkharch.domain.CurrentProjectHolder
import com.punitkumar.gruhkharch.presentation.components.OfflineBanner
import com.punitkumar.gruhkharch.presentation.components.rememberConnectivityState
import com.punitkumar.gruhkharch.presentation.addexpense.AddExpenseScreen
import com.punitkumar.gruhkharch.presentation.auth.AuthScreen
import com.punitkumar.gruhkharch.presentation.expenses.ExpensesScreen
import com.punitkumar.gruhkharch.presentation.home.HomeScreen
import com.punitkumar.gruhkharch.presentation.projects.ProjectsListScreen
import com.punitkumar.gruhkharch.presentation.reports.ReportsScreen
import com.punitkumar.gruhkharch.presentation.settings.SettingsScreen

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Routes.Home.route, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("Expenses", Routes.Expenses.route, Icons.Filled.Receipt, Icons.Outlined.Receipt),
    BottomNavItem("Reports", Routes.Reports.route, Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BottomNavItem("Settings", Routes.Settings.route, Icons.Filled.Settings, Icons.Outlined.Settings),
    BottomNavItem("Projects", Routes.ProjectsList.route, Icons.Filled.Folder, Icons.Outlined.Folder)
)

private val fabExcludedRoutes = setOf(
    Routes.ProjectsList.route,
    Routes.Settings.route,
    Routes.AddExpense.route,
    Routes.AddExpense.createRoute()
)

@Composable
fun AppNavGraph(isSignedIn: Boolean, currentProjectHolder: CurrentProjectHolder) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentProjectId by currentProjectHolder.projectId.collectAsState()

    val showBottomBar = currentDestination?.hierarchy?.any { dest ->
        bottomNavItems.any { it.route == dest.route }
    } == true

    val showFab = showBottomBar
        && currentProjectId != null
        && currentDestination?.route !in fabExcludedRoutes

    val isConnected by rememberConnectivityState()
    val startDestination = if (!isSignedIn) Routes.Auth.route else Routes.ProjectsList.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = item.route != Routes.ProjectsList.route
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Routes.AddExpense.createRoute()) {
                            launchSingleTop = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (showBottomBar) {
                OfflineBanner(isOffline = !isConnected)
            }
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
            composable(Routes.Auth.route) {
                AuthScreen(
                    onSignInSuccess = {
                        navController.navigate(Routes.ProjectsList.route) {
                            popUpTo(Routes.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.ProjectsList.route) {
                ProjectsListScreen(
                    onProjectSelected = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.ProjectsList.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onCreateProject = {
                        navController.navigate(Routes.CreateProject.route)
                    }
                )
            }

            composable(Routes.CreateProject.route) {
                com.punitkumar.gruhkharch.presentation.auth.CreateProjectScreen(
                    onProjectCreated = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.ProjectsList.route)
                        }
                    },
                    onJoinProject = {
                        navController.navigate(Routes.JoinProject.route)
                    }
                )
            }

            composable(Routes.JoinProject.route) {
                com.punitkumar.gruhkharch.presentation.auth.JoinProjectScreen(
                    onProjectJoined = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.ProjectsList.route)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.Home.route) {
                HomeScreen(
                    onExpenseClick = { expenseId ->
                        navController.navigate(Routes.AddExpense.createRoute(expenseId))
                    },
                    onViewAllExpenses = {
                        navController.navigate(Routes.Expenses.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = Routes.AddExpense.route,
                arguments = listOf(
                    navArgument("expenseId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getString("expenseId")
                AddExpenseScreen(
                    expenseId = expenseId,
                    onExpenseAdded = { navController.popBackStack() }
                )
            }

            composable(Routes.Expenses.route) {
                ExpensesScreen(
                    onExpenseClick = { expenseId ->
                        navController.navigate(Routes.AddExpense.createRoute(expenseId))
                    }
                )
            }

            composable(Routes.Reports.route) {
                ReportsScreen()
            }

            composable(Routes.Settings.route) {
                SettingsScreen(
                    onSignOut = {
                        navController.navigate(Routes.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onProjectDeleted = {
                        navController.navigate(Routes.ProjectsList.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
        }
    }
}
