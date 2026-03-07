package com.punitkumar.gruhkharch.presentation.navigation

sealed class Routes(val route: String) {
    data object Auth : Routes("auth")
    data object ProjectsList : Routes("projects_list")
    data object CreateProject : Routes("create_project")
    data object JoinProject : Routes("join_project")
    data object Home : Routes("home")
    data object AddExpense : Routes("add_expense?expenseId={expenseId}") {
        fun createRoute(expenseId: String? = null): String {
            return if (expenseId != null) "add_expense?expenseId=$expenseId" else "add_expense"
        }
    }
    data object Expenses : Routes("expenses")
    data object Reports : Routes("reports")
    data object Settings : Routes("settings")
}
