package com.punitkumar.gruhkharch.presentation.expenses

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.punitkumar.gruhkharch.R
import com.punitkumar.gruhkharch.domain.model.Expense
import com.punitkumar.gruhkharch.presentation.components.ExpenseCard
import com.punitkumar.gruhkharch.presentation.components.FilterSheet
import com.punitkumar.gruhkharch.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onExpenseClick: (String) -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.error) {
        state.error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    if (!state.hasProject) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = stringResource(R.string.cd_no_project),
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.no_project_selected),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.select_project_expenses),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    // Delete confirmation dialog
    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            icon = { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.delete_expense_title)) },
            text = {
                Text(stringResource(R.string.delete_expense_confirm, expense.title, CurrencyFormatter.formatIndianCurrency(expense.amount)))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExpense(expense)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expenses)) },
                actions = {
                    IconButton(onClick = { viewModel.toggleFilters() }) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = stringResource(R.string.filter_icon),
                            tint = if (state.filter != com.punitkumar.gruhkharch.domain.model.ExpenseFilter())
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearch,
                placeholder = { Text(stringResource(R.string.search_expenses)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateSearch("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_icon))
                        }
                    }
                }
            )

            // Summary bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            stringResource(R.string.total_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            CurrencyFormatter.formatIndianCurrency(state.totalAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        stringResource(R.string.expense_count_format, state.totalCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Filter sheet
            if (state.showFilters) {
                FilterSheet(
                    filter = state.filter,
                    members = state.members,
                    onFilterChanged = viewModel::updateFilter,
                    onClear = viewModel::clearFilters,
                    onDismiss = { viewModel.toggleFilters() }
                )
            }

            // Expense list
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.expenses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.ReceiptLong,
                            contentDescription = stringResource(R.string.no_expenses_icon),
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.no_expenses_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.groupedExpenses.forEach { (group, expenses) ->
                        // Group header
                        item(key = "header_$group") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${expenses.size} items | ${CurrencyFormatter.formatIndianCurrency(state.groupTotals[group] ?: 0.0)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider()
                        }

                        // Expense items with swipe-to-delete (only for creator)
                        items(
                            items = expenses,
                            key = { it.id }
                        ) { expense ->
                            val canDelete = viewModel.canDeleteExpense(expense)
                            if (canDelete) {
                                SwipeToDismissExpenseCard(
                                    expense = expense,
                                    onClick = { onExpenseClick(expense.id) },
                                    onDelete = { expenseToDelete = expense }
                                )
                            } else {
                                ExpenseCard(
                                    expense = expense,
                                    onClick = { onExpenseClick(expense.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissExpenseCard(
    expense: Expense,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false // Don't actually dismiss; let the dialog handle it
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        ExpenseCard(
            expense = expense,
            onClick = onClick
        )
    }
}
