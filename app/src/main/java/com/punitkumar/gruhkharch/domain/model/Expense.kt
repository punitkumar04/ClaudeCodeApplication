package com.punitkumar.gruhkharch.domain.model

data class Expense(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val paidBy: Member = Member(),
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val transactionRef: String? = null,
    val category: String = "",
    val subCategory: String? = null,
    val stage: String = "",
    val vendor: String? = null,
    val notes: String? = null,
    val receiptUrl: String? = null,
    val tags: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val projectId: String = "",
    val isSynced: Boolean = false
)
