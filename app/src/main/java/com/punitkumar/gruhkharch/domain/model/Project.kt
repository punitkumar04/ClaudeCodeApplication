package com.punitkumar.gruhkharch.domain.model

data class Project(
    val id: String = "",
    val name: String = "",
    val createdBy: String = "",
    val inviteCode: String = "",
    val budget: Double = 0.0,
    val currency: String = "INR",
    val currentStage: String = "Pre-Construction",
    val members: List<Member> = emptyList(),
    val stageBudgets: Map<String, Double> = emptyMap(),
    val categoryBudgets: Map<String, Double> = emptyMap(),
    val customCategories: List<Category> = emptyList(),
    val customStages: List<ConstructionStage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
