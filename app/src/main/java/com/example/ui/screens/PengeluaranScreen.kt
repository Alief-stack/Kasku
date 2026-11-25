package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.FinanceCategories
import com.example.data.model.TransactionRecord
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.EmptyFinanceCard
import com.example.ui.components.MonthSwitcherBar
import com.example.ui.components.TransactionRowCard
import com.example.ui.components.formatRupiah
import com.example.ui.state.BudgetStatus
import com.example.ui.state.FinanceDashboardData
import com.example.ui.theme.LocalFinanceColors

@Composable
fun PengeluaranScreen(
    data: FinanceDashboardData,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEditTransaction: (TransactionRecord) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onOpenSetBudget: (category: String?, currentLimit: Long?) -> Unit,
    onDeleteBudget: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalFinanceColors.current
    var selectedCategory by remember { mutableStateOf("Semua") }

    val filteredTransactions = remember(data.monthTransactions, selectedCategory) {
        if (selectedCategory == "Semua") data.monthTransactions
        else data.monthTransactions.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Pengeluaran & Anggaran",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            MonthSwitcherBar(
                monthKey = data.selectedMonthKey,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth
            )
        }

        // Income vs Expense Summary Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = financeColors.expenseContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Total Pengeluaran",
                            style = MaterialTheme.typography.labelMedium,
                            color = financeColors.expense
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatRupiah(data.monthExpense),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = financeColors.incomeContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Total Pemasukan",
                            style = MaterialTheme.typography.labelMedium,
                            color = financeColors.income
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatRupiah(data.monthIncome),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Category Breakdown Chart
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Distribusi Pengeluaran", style = MaterialTheme.typography.titleMedium)
                    CategoryDonutChart(
                        categories = data.categoryBreakdown,
                        totalExpense = data.monthExpense
                    )
                }
            }
        }

        // Monthly Budgets Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Batas Anggaran Kategori", style = MaterialTheme.typography.titleLarge)
                TextButton(
                    onClick = { onOpenSetBudget(null, null) },
                    modifier = Modifier.testTag("add_budget_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Atur")
                }
            }
        }

        if (data.monthBudgets.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Belum ada batas anggaran", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Tetapkan batas belanja bulanan per kategori agar pengeluaran tetap terkendali.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onOpenSetBudget(null, null) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = "Atur anggaran")
                        }
                    }
                }
            }
        } else {
            items(data.monthBudgets, key = { it.budget.id }) { status ->
                BudgetProgressCard(
                    status = status,
                    onEdit = { onOpenSetBudget(status.budget.category, status.budget.limitAmount) },
                    onDelete = { onDeleteBudget(status.budget.id) }
                )
            }
        }

        // Transaction Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Riwayat Transaksi", style = MaterialTheme.typography.titleLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val allFilters = listOf("Semua") + FinanceCategories.expenseCategories + FinanceCategories.incomeCategories.filter { it != "Lainnya" }
                    allFilters.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                EmptyFinanceCard(
                    title = "Tidak ada transaksi",
                    subtitle = if (selectedCategory == "Semua") {
                        "Catat pengeluaran atau pemasukan pertama Anda di bulan ini."
                    } else {
                        "Belum ada transaksi pada kategori $selectedCategory."
                    }
                )
            }
        } else {
            items(filteredTransactions, key = { it.id }) { tx ->
                TransactionRowCard(
                    transaction = tx,
                    onEdit = { onEditTransaction(tx) },
                    onDelete = { onDeleteTransaction(tx.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun BudgetProgressCard(
    status: BudgetStatus,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val barColor = when {
        status.isOverBudget -> financeColors.expense
        status.isNearLimit -> financeColors.warning
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(status.budget.category, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${formatRupiah(status.spentAmount)} / ${formatRupiah(status.budget.limitAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.isOverBudget) financeColors.expense else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_delete_item),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { status.progress.coerceIn(0f, 1f) },
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )
            val statusLabel = when {
                status.isOverBudget -> "Melebihi batas ${formatRupiah(status.spentAmount - status.budget.limitAmount)}"
                status.isNearLimit -> "Hampir mencapai batas (${(status.progress * 100).toInt()}%)"
                else -> "Sisa ${formatRupiah((status.budget.limitAmount - status.spentAmount).coerceAtLeast(0L))}"
            }
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = barColor
            )
        }
    }
}
