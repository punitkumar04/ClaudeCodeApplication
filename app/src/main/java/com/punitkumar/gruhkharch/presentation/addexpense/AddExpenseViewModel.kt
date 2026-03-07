package com.punitkumar.gruhkharch.presentation.addexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punitkumar.gruhkharch.domain.CurrentProjectHolder
import com.punitkumar.gruhkharch.domain.model.*
import com.punitkumar.gruhkharch.domain.repository.AuthRepository
import com.punitkumar.gruhkharch.domain.repository.ExpenseRepository
import com.punitkumar.gruhkharch.domain.repository.ProjectRepository
import com.punitkumar.gruhkharch.domain.usecase.AddExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddExpenseState(
    val title: String = "",
    val amount: String = "",
    val date: Long = System.currentTimeMillis(),
    val paidByMember: Member? = null,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val transactionRef: String = "",
    val category: String = "",
    val subCategory: String = "",
    val stage: String = "",
    val vendor: String = "",
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val members: List<Member> = emptyList(),
    val categories: List<Category> = DefaultCategories.all,
    val stages: List<ConstructionStage> = DefaultStages.all,
    val subCategories: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val addExpenseUseCase: AddExpenseUseCase,
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository,
    private val currentProjectHolder: CurrentProjectHolder
) : ViewModel() {

    private val _state = MutableStateFlow(AddExpenseState())
    val state: StateFlow<AddExpenseState> = _state.asStateFlow()

    private var projectId: String = ""

    init {
        loadProjectData()
    }

    private fun loadProjectData() {
        viewModelScope.launch {
            projectId = currentProjectHolder.projectId.value ?: return@launch
            val project = projectRepository.getProject(projectId) ?: return@launch

            val currentUser = authRepository.currentUser
            val defaultMember = project.members.find { it.userId == currentUser?.id }

            _state.update {
                it.copy(
                    members = project.members,
                    paidByMember = defaultMember ?: project.members.firstOrNull(),
                    stage = project.currentStage
                )
            }
        }
    }

    fun loadExpense(expenseId: String) {
        viewModelScope.launch {
            val expense = expenseRepository.getExpenseById(expenseId) ?: return@launch
            val category = DefaultCategories.all.find { it.name == expense.category }
            _state.update {
                it.copy(
                    title = expense.title,
                    amount = expense.amount.toString(),
                    date = expense.date,
                    paidByMember = expense.paidBy,
                    paymentMode = expense.paymentMode,
                    transactionRef = expense.transactionRef ?: "",
                    category = expense.category,
                    subCategory = expense.subCategory ?: "",
                    stage = expense.stage,
                    vendor = expense.vendor ?: "",
                    notes = expense.notes ?: "",
                    tags = expense.tags,
                    subCategories = category?.subCategories ?: emptyList(),
                    isEditing = true
                )
            }
            projectId = expense.projectId
        }
    }

    fun updateTitle(title: String) { _state.update { it.copy(title = title, error = null) } }
    fun updateAmount(amount: String) { _state.update { it.copy(amount = amount, error = null) } }
    fun updateDate(date: Long) { _state.update { it.copy(date = date) } }
    fun updatePaidBy(member: Member) { _state.update { it.copy(paidByMember = member) } }
    fun updatePaymentMode(mode: PaymentMode) { _state.update { it.copy(paymentMode = mode) } }
    fun updateTransactionRef(ref: String) { _state.update { it.copy(transactionRef = ref) } }
    fun updateVendor(vendor: String) { _state.update { it.copy(vendor = vendor) } }
    fun updateNotes(notes: String) { _state.update { it.copy(notes = notes) } }
    fun updateTagInput(input: String) { _state.update { it.copy(tagInput = input) } }

    fun updateCategory(category: String) {
        val cat = DefaultCategories.all.find { it.name == category }
        _state.update {
            it.copy(
                category = category,
                subCategory = "",
                subCategories = cat?.subCategories ?: emptyList()
            )
        }
    }

    fun updateSubCategory(subCategory: String) {
        _state.update { it.copy(subCategory = subCategory) }
    }

    fun updateStage(stage: String) { _state.update { it.copy(stage = stage) } }

    fun addTag() {
        val tag = _state.value.tagInput.trim()
        if (tag.isNotBlank() && tag !in _state.value.tags) {
            _state.update { it.copy(tags = it.tags + tag, tagInput = "") }
        }
    }

    fun removeTag(tag: String) {
        _state.update { it.copy(tags = it.tags - tag) }
    }

    fun saveExpense() {
        val s = _state.value
        if (s.title.isBlank()) { _state.update { it.copy(error = "Title is required") }; return }
        if (s.amount.toDoubleOrNull() == null || s.amount.toDouble() <= 0) {
            _state.update { it.copy(error = "Enter a valid amount") }; return
        }
        if (s.category.isBlank()) { _state.update { it.copy(error = "Select a category") }; return }
        if (s.stage.isBlank()) { _state.update { it.copy(error = "Select a construction stage") }; return }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val expense = Expense(
                title = s.title.trim(),
                amount = s.amount.toDouble(),
                date = s.date,
                paidBy = s.paidByMember ?: Member(),
                paymentMode = s.paymentMode,
                transactionRef = s.transactionRef.ifBlank { null },
                category = s.category,
                subCategory = s.subCategory.ifBlank { null },
                stage = s.stage,
                vendor = s.vendor.ifBlank { null },
                notes = s.notes.ifBlank { null },
                tags = s.tags,
                createdBy = authRepository.currentUserId ?: "",
                projectId = projectId
            )

            addExpenseUseCase(expense)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, isSaved = true) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
