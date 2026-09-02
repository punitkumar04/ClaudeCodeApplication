package com.punitkumar.gruhkharch.presentation.reports

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.punitkumar.gruhkharch.R
import com.punitkumar.gruhkharch.presentation.theme.ChartColors
import com.punitkumar.gruhkharch.util.CurrencyFormatter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

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
                    text = stringResource(R.string.select_project_reports),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_category),
        stringResource(R.string.tab_stage),
        stringResource(R.string.tab_member),
        stringResource(R.string.tab_payment),
        stringResource(R.string.tab_vendors)
    )
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reports_analytics)) },
                actions = {
                    IconButton(onClick = {
                        exportCsv(context, viewModel.generateCsvContent())
                    }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = stringResource(R.string.cd_export_csv))
                    }
                    IconButton(onClick = {
                        shareCsv(context, viewModel.generateCsvContent())
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.cd_share))
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.total_expenditure),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = CurrencyFormatter.formatIndianCurrency(state.totalSpent),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (state.budget > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val remaining = state.budget - state.totalSpent
                            Text(
                                text = stringResource(R.string.remaining_format, CurrencyFormatter.formatIndianCurrency(remaining)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (remaining >= 0) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Tab row
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 0.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }

            // Content based on tab
            val dataMap = when (selectedTab) {
                0 -> state.categoryBreakdown
                1 -> state.stageBreakdown
                2 -> state.memberBreakdown
                3 -> state.paymentModeBreakdown
                4 -> state.topVendors
                else -> emptyMap()
            }

            val totalForTab = dataMap.values.sum()

            items(dataMap.entries.toList()) { (label, amount) ->
                ReportRow(
                    label = label,
                    amount = amount,
                    total = totalForTab,
                    color = ChartColors[dataMap.keys.indexOf(label) % ChartColors.size]
                )
            }

            if (dataMap.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_data_available),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Monthly Trend
            if (state.monthlyTrend.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.monthly_trend),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(state.monthlyTrend.entries.toList()) { (month, amount) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = month,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = CurrencyFormatter.formatIndianCurrency(amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun ReportRow(label: String, amount: Double, total: Double, color: Color) {
    val percent = if (total > 0) (amount / total * 100) else 0.0
    val progress = if (total > 0) (amount / total).toFloat() else 0f

    Card(
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.1f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatIndianCurrency(amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.1f", percent)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun exportCsv(context: Context, csvContent: String) {
    try {
        val file = File(context.cacheDir, context.getString(R.string.export_filename))
        file.writeText(csvContent)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.export_expenses)))
    } catch (e: Exception) {
        android.util.Log.w("ReportsScreen", "Failed to export CSV", e)
        android.widget.Toast.makeText(context, context.getString(R.string.export_failed), android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun shareCsv(context: Context, csvContent: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.expenses_report_subject))
            putExtra(Intent.EXTRA_TEXT, csvContent)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_report)))
    } catch (e: Exception) {
        android.util.Log.w("ReportsScreen", "Failed to share report", e)
        android.widget.Toast.makeText(context, context.getString(R.string.share_failed), android.widget.Toast.LENGTH_SHORT).show()
    }
}
