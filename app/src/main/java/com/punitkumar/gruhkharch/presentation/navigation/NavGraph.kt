package com.punitkumar.gruhkharch.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.punitkumar.gruhkharch.presentation.addexpense.AddExpenseScreen
import com.punitkumar.gruhkharch.presentation.auth.AuthScreen
import com.punitkumar.gruhkharch.presentation.expenses.ExpensesScreen
import com.punitkumar.gruhkharch.presentation.home.HomeScreen
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
    BottomNavItem("Add", Routes.AddExpense.createRoute(), Icons.Filled.AddCircle, Icons.Outlined.AddCircle),
    BottomNavItem("Expenses", Routes.Expenses.route, Icons.Filled.Receipt, Icons.Outlined.Receipt),
    BottomNavItem("Reports", Routes.Reports.route, Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BottomNavItem("Settings", Routes.Settings.route, Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun AppNavGraph(isSignedIn: Boolean, hasProject: Boolean) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.hierarchy?.any { dest ->
        bottomNavItems.any { it.route == dest.route }
    } == true

    val startDestination = when {
        !isSignedIn -> Routes.Auth.route
        !hasProject -> Routes.CreateProject.route
        else -> Routes.Home.route
    }

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
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showBottomBar && currentDestination?.route != Routes.AddExpense.createRoute()) {
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
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Auth.route) {
                AuthScreen(
                    onSignInSuccess = {
                        navController.navigate(Routes.CreateProject.route) {
                            popUpTo(Routes.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.CreateProject.route) {
                com.punitkumar.gruhkharch.presentation.auth.CreateProjectScreen(
                    onProjectCreated = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(0) { inclusive = true }
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
                            popUpTo(0) { inclusive = true }
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
                    }
                )
            }
        }
    }
}
