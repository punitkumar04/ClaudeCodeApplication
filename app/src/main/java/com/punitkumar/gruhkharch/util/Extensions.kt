package com.punitkumar.gruhkharch.util

import com.punitkumar.gruhkharch.data.local.entity.ExpenseEntity
import com.punitkumar.gruhkharch.data.local.entity.ProjectEntity
import com.punitkumar.gruhkharch.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    title = title,
    amount = amount,
    date = date,
    paidByUserId = paidBy.userId,
    paidByName = paidBy.name,
    paymentMode = paymentMode.name,
    transactionRef = transactionRef,
    category = category,
    subCategory = subCategory,
    stage = stage,
    vendor = vendor,
    notes = notes,
    receiptUrl = receiptUrl,
    tags = json.encodeToString(tags),
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    projectId = projectId,
    isSynced = isSynced
)

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    title = title,
    amount = amount,
    date = date,
    paidBy = Member(userId = paidByUserId, name = paidByName),
    paymentMode = try { PaymentMode.valueOf(paymentMode) } catch (e: Exception) { PaymentMode.OTHER },
    transactionRef = transactionRef,
    category = category,
    subCategory = subCategory,
    stage = stage,
    vendor = vendor,
    notes = notes,
    receiptUrl = receiptUrl,
    tags = try { json.decodeFromString(tags) } catch (e: Exception) { emptyList() },
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    projectId = projectId,
    isSynced = isSynced
)

fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    name = name,
    createdBy = createdBy,
    inviteCode = inviteCode,
    budget = budget,
    currency = currency,
    currentStage = currentStage,
    membersJson = json.encodeToString(members.map { mapOf("userId" to it.userId, "name" to it.name, "role" to it.role.name, "color" to it.color) }),
    stageBudgetsJson = json.encodeToString(stageBudgets),
    categoryBudgetsJson = json.encodeToString(categoryBudgets),
    createdAt = createdAt
)

fun ProjectEntity.toDomain(): Project {
    val membersList = try {
        val parsed: List<Map<String, String>> = json.decodeFromString(membersJson)
        parsed.map { m ->
            Member(
                userId = m["userId"] ?: "",
                name = m["name"] ?: "",
                role = try { MemberRole.valueOf(m["role"] ?: "FAMILY_MEMBER") } catch (e: Exception) { MemberRole.FAMILY_MEMBER },
                color = m["color"] ?: "#8B5E3C"
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    val stageBudgetsMap: Map<String, Double> = try {
        json.decodeFromString(stageBudgetsJson)
    } catch (e: Exception) {
        emptyMap()
    }

    val categoryBudgetsMap: Map<String, Double> = try {
        json.decodeFromString(categoryBudgetsJson)
    } catch (e: Exception) {
        emptyMap()
    }

    return Project(
        id = id,
        name = name,
        createdBy = createdBy,
        inviteCode = inviteCode,
        budget = budget,
        currency = currency,
        currentStage = currentStage,
        members = membersList,
        stageBudgets = stageBudgetsMap,
        categoryBudgets = categoryBudgetsMap,
        createdAt = createdAt
    )
}
