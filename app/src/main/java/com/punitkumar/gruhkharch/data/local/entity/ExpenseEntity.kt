package com.punitkumar.gruhkharch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val amount: Double,
    val date: Long,
    val paidByUserId: String,
    val paidByName: String,
    val paymentMode: String,
    val transactionRef: String?,
    val category: String,
    val subCategory: String?,
    val stage: String,
    val vendor: String?,
    val notes: String?,
    val receiptUrl: String?,
    val tags: String, // JSON array stored as string
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val projectId: String,
    val isSynced: Boolean = false
)
