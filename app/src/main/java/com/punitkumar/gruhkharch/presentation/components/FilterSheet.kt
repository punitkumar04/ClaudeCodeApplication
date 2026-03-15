package com.punitkumar.gruhkharch.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.punitkumar.gruhkharch.R
import com.punitkumar.gruhkharch.domain.model.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    filter: ExpenseFilter,
    members: List<Member>,
    onFilterChanged: (ExpenseFilter) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSortBy by remember { mutableStateOf(filter.sortBy) }
    var selectedGroupBy by remember { mutableStateOf(filter.groupBy) }
    var selectedCategories by remember { mutableStateOf(filter.categories.toSet()) }
    var selectedStages by remember { mutableStateOf(filter.stages.toSet()) }
    var selectedMembers by remember { mutableStateOf(filter.paidByUserIds.toSet()) }
    var selectedPaymentModes by remember { mutableStateOf(filter.paymentModes.toSet()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.filters_and_sort), style = MaterialTheme.typography.titleMedium)
                Row {
                    TextButton(onClick = onClear) { Text(stringResource(R.string.clear)) }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                    }
                }
            }

            // Sort By
            Text(stringResource(R.string.sort_by), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SortBy.entries.forEach { sort ->
                    FilterChip(
                        selected = selectedSortBy == sort,
                        onClick = { selectedSortBy = sort },
                        label = { Text(sort.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Group By
            Text(stringResource(R.string.group_by), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GroupBy.entries.forEach { group ->
                    FilterChip(
                        selected = selectedGroupBy == group,
                        onClick = { selectedGroupBy = group },
                        label = { Text(group.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Categories
            Text(stringResource(R.string.categories), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DefaultCategories.all.forEach { cat ->
                    FilterChip(
                        selected = cat.name in selectedCategories,
                        onClick = {
                            selectedCategories = if (cat.name in selectedCategories)
                                selectedCategories - cat.name
                            else selectedCategories + cat.name
                        },
                        label = { Text("${cat.emoji} ${cat.name}", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Stages
            Text(stringResource(R.string.construction_stage), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DefaultStages.all.forEach { stage ->
                    FilterChip(
                        selected = stage.name in selectedStages,
                        onClick = {
                            selectedStages = if (stage.name in selectedStages)
                                selectedStages - stage.name
                            else selectedStages + stage.name
                        },
                        label = { Text(stage.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Members
            if (members.isNotEmpty()) {
                Text(stringResource(R.string.paid_by), style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    members.forEach { member ->
                        FilterChip(
                            selected = member.userId in selectedMembers,
                            onClick = {
                                selectedMembers = if (member.userId in selectedMembers)
                                    selectedMembers - member.userId
                                else selectedMembers + member.userId
                            },
                            label = { Text(member.name) }
                        )
                    }
                }
            }

            // Payment Modes
            Text(stringResource(R.string.payment_mode), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PaymentMode.entries.forEach { mode ->
                    FilterChip(
                        selected = mode in selectedPaymentModes,
                        onClick = {
                            selectedPaymentModes = if (mode in selectedPaymentModes)
                                selectedPaymentModes - mode
                            else selectedPaymentModes + mode
                        },
                        label = { Text(mode.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Apply button
            Button(
                onClick = {
                    onFilterChanged(
                        filter.copy(
                            sortBy = selectedSortBy,
                            groupBy = selectedGroupBy,
                            categories = selectedCategories.toList(),
                            stages = selectedStages.toList(),
                            paidByUserIds = selectedMembers.toList(),
                            paymentModes = selectedPaymentModes.toList()
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.apply_filters))
            }
        }
    }
}
