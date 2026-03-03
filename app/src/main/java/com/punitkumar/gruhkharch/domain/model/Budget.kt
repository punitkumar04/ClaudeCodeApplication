package com.punitkumar.gruhkharch.domain.model

data class Budget(
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val stageBudgets: Map<String, Double> = emptyMap(),
    val stageSpent: Map<String, Double> = emptyMap(),
    val categoryBudgets: Map<String, Double> = emptyMap(),
    val categorySpent: Map<String, Double> = emptyMap()
) {
    val remaining: Double get() = totalBudget - totalSpent
    val percentUsed: Float get() = if (totalBudget > 0) (totalSpent / totalBudget * 100).toFloat() else 0f
}
