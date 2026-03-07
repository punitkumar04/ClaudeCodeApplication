package com.punitkumar.gruhkharch.presentation.addexpense

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.punitkumar.gruhkharch.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    expenseId: String? = null,
    onExpenseAdded: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showPaidByMenu by remember { mutableStateOf(false) }
    var showPaymentModeMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showSubCategoryMenu by remember { mutableStateOf(false) }
    var showStageMenu by remember { mutableStateOf(false) }

    LaunchedEffect(expenseId) {
        expenseId?.let { viewModel.loadExpense(it) }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            try {
                val vibrator = context.getSystemService(Vibrator::class.java)
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (e: Exception) { /* ignore */ }
            onExpenseAdded()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Expense" else "Add Expense") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (state.isEditing && !state.canEdit) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cannot Edit",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You can only edit expenses that you added",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Title *") },
                placeholder = { Text("e.g., Tata Sheets for Storage Home") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null) }
            )

            // Amount
            OutlinedTextField(
                value = state.amount,
                onValueChange = viewModel::updateAmount,
                label = { Text("Amount (\u20B9) *") },
                placeholder = { Text("0.00") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Icon(Icons.Filled.CurrencyRupee, contentDescription = null) }
            )

            // Date
            OutlinedTextField(
                value = com.punitkumar.gruhkharch.util.DateUtils.formatDate(state.date),
                onValueChange = {},
                label = { Text("Date *") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                leadingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.EditCalendar, contentDescription = "Pick date")
                    }
                }
            )

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = state.date
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { viewModel.updateDate(it) }
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // Paid By
            ExposedDropdownMenuBox(
                expanded = showPaidByMenu,
                onExpandedChange = { showPaidByMenu = it }
            ) {
                OutlinedTextField(
                    value = state.paidByMember?.name ?: "Select",
                    onValueChange = {},
                    label = { Text("Paid By *") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPaidByMenu) }
                )
                ExposedDropdownMenu(
                    expanded = showPaidByMenu,
                    onDismissRequest = { showPaidByMenu = false }
                ) {
                    state.members.forEach { member ->
                        DropdownMenuItem(
                            text = { Text(member.name) },
                            onClick = {
                                viewModel.updatePaidBy(member)
                                showPaidByMenu = false
                            }
                        )
                    }
                }
            }

            // Payment Mode
            ExposedDropdownMenuBox(
                expanded = showPaymentModeMenu,
                onExpandedChange = { showPaymentModeMenu = it }
            ) {
                OutlinedTextField(
                    value = state.paymentMode.displayName,
                    onValueChange = {},
                    label = { Text("Payment Mode *") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Filled.Payment, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPaymentModeMenu) }
                )
                ExposedDropdownMenu(
                    expanded = showPaymentModeMenu,
                    onDismissRequest = { showPaymentModeMenu = false }
                ) {
                    PaymentMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.displayName) },
                            onClick = {
                                viewModel.updatePaymentMode(mode)
                                showPaymentModeMenu = false
                            }
                        )
                    }
                }
            }

            // Category
            ExposedDropdownMenuBox(
                expanded = showCategoryMenu,
                onExpandedChange = { showCategoryMenu = it }
            ) {
                OutlinedTextField(
                    value = state.category.ifBlank { "Select Category" },
                    onValueChange = {},
                    label = { Text("Category *") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) }
                )
                ExposedDropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false }
                ) {
                    state.categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.emoji} ${cat.name}") },
                            onClick = {
                                viewModel.updateCategory(cat.name)
                                showCategoryMenu = false
                            }
                        )
                    }
                }
            }

            // Sub-Category (if available)
            if (state.subCategories.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = showSubCategoryMenu,
                    onExpandedChange = { showSubCategoryMenu = it }
                ) {
                    OutlinedTextField(
                        value = state.subCategory.ifBlank { "Select Sub-Category" },
                        onValueChange = {},
                        label = { Text("Sub-Category") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSubCategoryMenu) }
                    )
                    ExposedDropdownMenu(
                        expanded = showSubCategoryMenu,
                        onDismissRequest = { showSubCategoryMenu = false }
                    ) {
                        state.subCategories.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub) },
                                onClick = {
                                    viewModel.updateSubCategory(sub)
                                    showSubCategoryMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Construction Stage
            ExposedDropdownMenuBox(
                expanded = showStageMenu,
                onExpandedChange = { showStageMenu = it }
            ) {
                OutlinedTextField(
                    value = state.stage.ifBlank { "Select Stage" },
                    onValueChange = {},
                    label = { Text("Construction Stage *") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Filled.Engineering, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStageMenu) }
                )
                ExposedDropdownMenu(
                    expanded = showStageMenu,
                    onDismissRequest = { showStageMenu = false }
                ) {
                    state.stages.forEach { stg ->
                        DropdownMenuItem(
                            text = { Text("${stg.emoji} ${stg.name}") },
                            onClick = {
                                viewModel.updateStage(stg.name)
                                showStageMenu = false
                            }
                        )
                    }
                }
            }

            // Vendor
            OutlinedTextField(
                value = state.vendor,
                onValueChange = viewModel::updateVendor,
                label = { Text("Vendor / Paid To (Optional)") },
                placeholder = { Text("e.g., Ramesh Contractor") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Store, contentDescription = null) }
            )

            // Notes
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Any additional details...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                leadingIcon = { Icon(Icons.Filled.Notes, contentDescription = null) },
                supportingText = {
                    Text("${state.notes.length}/200")
                }
            )

            // Error message
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = { viewModel.saveExpense() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isLoading,
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (state.isEditing) "Update Expense" else "Save Expense",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
