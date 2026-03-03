package com.punitkumar.gruhkharch.util

object Constants {
    const val DB_NAME = "gruhkharch_database"
    const val FIRESTORE_PROJECTS = "projects"
    const val FIRESTORE_EXPENSES = "expenses"
    const val FIRESTORE_USERS = "users"
    const val STORAGE_RECEIPTS = "receipts"
    const val PREFS_DATASTORE = "gruhkharch_prefs"
    const val PREFS_CURRENT_PROJECT_ID = "current_project_id"
    const val PREFS_USER_ID = "user_id"
    const val PREFS_THEME_MODE = "theme_mode"
    const val INVITE_CODE_LENGTH = 6
    const val MAX_RECEIPT_SIZE_MB = 5
    const val BUDGET_WARNING_THRESHOLD = 0.8f
    const val BUDGET_CRITICAL_THRESHOLD = 1.0f

    val MEMBER_COLORS = listOf(
        "#8B5E3C", "#D4763A", "#2E7D32", "#1565C0",
        "#6A1B9A", "#C62828", "#00838F", "#EF6C00"
    )
}
