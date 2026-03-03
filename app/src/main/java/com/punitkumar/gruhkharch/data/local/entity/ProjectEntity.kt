package com.punitkumar.gruhkharch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdBy: String,
    val inviteCode: String,
    val budget: Double,
    val currency: String,
    val currentStage: String,
    val membersJson: String, // JSON string
    val stageBudgetsJson: String, // JSON string
    val categoryBudgetsJson: String, // JSON string
    val createdAt: Long
)
