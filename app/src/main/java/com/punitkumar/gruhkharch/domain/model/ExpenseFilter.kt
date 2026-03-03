package com.punitkumar.gruhkharch.domain.model

data class ExpenseFilter(
    val dateRange: DateRange? = null,
    val paidByUserIds: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val subCategories: List<String> = emptyList(),
    val stages: List<String> = emptyList(),
    val paymentModes: List<PaymentMode> = emptyList(),
    val amountMin: Double? = null,
    val amountMax: Double? = null,
    val vendor: String? = null,
    val tags: List<String> = emptyList(),
    val searchQuery: String? = null,
    val sortBy: SortBy = SortBy.DATE_DESC,
    val groupBy: GroupBy = GroupBy.NONE
)

data class DateRange(
    val startDate: Long,
    val endDate: Long
)

enum class SortBy(val displayName: String) {
    DATE_DESC("Date (Newest)"),
    DATE_ASC("Date (Oldest)"),
    AMOUNT_DESC("Amount (Highest)"),
    AMOUNT_ASC("Amount (Lowest)"),
    CATEGORY_ASC("Category (A-Z)")
}

enum class GroupBy(val displayName: String) {
    NONE("No Grouping"),
    DATE("By Date"),
    CATEGORY("By Category"),
    STAGE("By Stage"),
    PAID_BY("By Family Member"),
    PAYMENT_MODE("By Payment Mode"),
    MONTH("By Month")
}
