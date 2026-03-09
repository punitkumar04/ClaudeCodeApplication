package com.punitkumar.gruhkharch.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.punitkumar.gruhkharch.R
import com.punitkumar.gruhkharch.domain.model.DefaultStages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSignOut: () -> Unit,
    onProjectDeleted: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showStageMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.isProjectDeleted) {
        if (state.isProjectDeleted) {
            onProjectDeleted()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.delete_project)) },
            text = {
                Text(
                    "This will permanently delete the project \"${state.project?.name}\" and all its data including expenses, members, and settings. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteProject()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = stringResource(R.string.warning), tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.delete_account)) },
            text = {
                Text(stringResource(R.string.delete_account_warning))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        viewModel.deleteAccount { onSignOut() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ─── PROFILE SETTINGS ───
            item {
                Text(
                    text = stringResource(R.string.profile_settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = state.userName.ifEmpty { "User" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                if (state.userEmail.isNotEmpty()) {
                                    Text(
                                        text = state.userEmail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // Sign Out
                        OutlinedButton(
                            onClick = {
                                viewModel.signOut()
                                onSignOut()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.Logout, contentDescription = stringResource(R.string.sign_out))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.sign_out))
                        }

                        OutlinedButton(
                            onClick = { showDeleteAccountDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.PersonRemove, contentDescription = stringResource(R.string.delete_account))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.delete_account))
                        }
                    }
                }
            }

            // ─── PROJECT SETTINGS ───
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.project_settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!state.isProjectOwner) {
                    Text(
                        text = stringResource(R.string.owner_only_settings),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Project Name
            item {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Home, contentDescription = "Project", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = state.project?.name ?: stringResource(R.string.no_project),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Invite Code
            item {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Invite Code",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = state.inviteCode,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = MaterialTheme.typography.headlineSmall.letterSpacing
                                )
                            }
                            if (state.isProjectOwner) {
                                IconButton(onClick = { viewModel.regenerateInviteCode() }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Regenerate")
                                }
                            }
                        }
                    }
                }
            }

            // Members
            item {
                Text(
                    text = "Members (${state.members.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(state.members) { member ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Member",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = member.role.name.replace("_", " "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Budget
            item {
                Text(
                    text = "Budget",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = state.budget,
                            onValueChange = { viewModel.updateBudget(it) },
                            label = { Text(stringResource(R.string.total_budget_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = state.isProjectOwner,
                            leadingIcon = { Icon(Icons.Filled.CurrencyRupee, contentDescription = null) }
                        )
                        if (state.isProjectOwner) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.saveBudget() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(stringResource(R.string.save_budget))
                            }
                        }
                    }
                }
            }

            // Current Construction Stage
            item {
                Text(
                    text = stringResource(R.string.current_construction_stage),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = showStageMenu,
                    onExpandedChange = { if (state.isProjectOwner) showStageMenu = it }
                ) {
                    OutlinedTextField(
                        value = state.currentStage,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        enabled = state.isProjectOwner,
                        trailingIcon = {
                            if (state.isProjectOwner) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStageMenu)
                            }
                        },
                        leadingIcon = { Icon(Icons.Filled.Engineering, contentDescription = null) }
                    )
                    if (state.isProjectOwner) {
                        ExposedDropdownMenu(
                            expanded = showStageMenu,
                            onDismissRequest = { showStageMenu = false }
                        ) {
                            DefaultStages.all.forEach { stage ->
                                DropdownMenuItem(
                                    text = { Text("${stage.emoji} ${stage.name}") },
                                    onClick = {
                                        viewModel.updateCurrentStage(stage.name)
                                        showStageMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Delete Project (owner only)
            if (state.isProjectOwner) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.danger_zone),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.delete_project_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isLoading,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Icon(Icons.Filled.DeleteForever, contentDescription = "Delete project")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.delete_project))
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}
