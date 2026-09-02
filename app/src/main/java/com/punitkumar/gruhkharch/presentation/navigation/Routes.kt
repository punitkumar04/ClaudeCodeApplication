package com.punitkumar.gruhkharch.presentation.navigation

sealed class Routes(val route: String) {
    data object Auth : Routes("auth")
    data object ProjectsList : Routes("projects_list")
    data object CreateProject : Routes("create_project")
    data object JoinProject : Routes("join_project")
    data object Home : Routes("home")
    data object AddExpense : Routes("add_expense?expenseId={expenseId}&duplicate={duplicate}") {
        fun createRoute(expenseId: String? = null, duplicate: Boolean = false): String {
            return buildString {
                append("add_expense")
                val params = mutableListOf<String>()
                if (expenseId != null) params.add("expenseId=$expenseId")
                if (duplicate) params.add("duplicate=true")
                if (params.isNotEmpty()) {
                    append("?")
                    append(params.joinToString("&"))
                }
            }
        }
    }
    data object Expenses : Routes("expenses")
    data object Reports : Routes("reports")
    data object Settings : Routes("settings")
}
