package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.SavingsGoal
import com.example.data.model.TransactionRecord
import com.example.ui.components.EmptyFinanceCard
import com.example.ui.components.MonthSwitcherBar
import com.example.ui.components.SavingsProgressRing
import com.example.ui.components.TransactionRowCard
import com.example.ui.components.formatCompactRupiah
import com.example.ui.components.formatMonthLabel
import com.example.ui.components.formatRupiah
import com.example.ui.components.parseHexColor
import com.example.ui.components.savingsIconForKey
import com.example.ui.state.FinanceDashboardData
import com.example.ui.theme.LocalFinanceColors

@Composable
fun BerandaScreen(
    data: FinanceDashboardData,
    userName: String?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onAddSavingsClick: () -> Unit,
    onDepositToGoal: (SavingsGoal) -> Unit,
    onEditTransaction: (TransactionRecord) -> Unit,
    onOpenExpensesTab: () -> Unit,
    onOpenSavingsTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalFinanceColors.current
    val firstName = userName?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotEmpty() } ?: "Sahabat"

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Halo, $firstName",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Ringkasan keuangan pribadi untuk ${formatMonthLabel(data.selectedMonthKey)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            MonthSwitcherBar(
                monthKey = data.selectedMonthKey,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth
            )
        }

        // Hero Emerald Treasury Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Total Simpanan Pribadi",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                            Text(
                                text = formatRupiah(data.totalSavings),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Savings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDownward,
                                        contentDescription = null,
                                        tint = Color(0xFFA7F3D0),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Pemasukan",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                    )
                                }
                                Text(
                                    text = formatCompactRupiah(data.monthIncome),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowUpward,
                                        contentDescription = null,
                                        tint = Color(0xFFFDE68A),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Pengeluaran",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                    )
                                }
                                Text(
                                    text = formatCompactRupiah(data.monthExpense),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selisih kas bulan ini",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                        Text(
                            text = (if (data.netCashflow >= 0) "+" else "") + formatRupiah(data.netCashflow),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Quick Action CTA Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAddExpenseClick,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("quick_add_tx_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Catat Transaksi", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                FilledTonalButton(
                    onClick = onAddSavingsClick,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("quick_add_savings_button")
                ) {
                    Icon(Icons.Filled.Savings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buat Tabungan", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Monthly Budget Overview (if budgets configured)
        if (data.monthBudgets.isNotEmpty()) {
            item {
                val totalRatio = if (data.totalBudgetLimit > 0L) {
                    (data.totalBudgetSpent.toFloat() / data.totalBudgetLimit.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val overCount = data.monthBudgets.count { it.isOverBudget }
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pemakaian Anggaran", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = onOpenExpensesTab) {
                                Text("Detail")
                            }
                        }
                        LinearProgressIndicator(
                            progress = { totalRatio },
                            color = if (overCount > 0) financeColors.expense else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Terpakai ${formatRupiah(data.totalBudgetSpent)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Batas ${formatRupiah(data.totalBudgetLimit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (overCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = financeColors.expense,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$overCount kategori melewati batas anggaran bulan ini",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = financeColors.expense
                                )
                            }
                        }
                    }
                }
            }
        }

        // Savings Pockets Preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Kantong Tabungan", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onOpenSavingsTab) {
                    Text("Lihat Semua")
                }
            }
        }

        if (data.savingsGoals.isEmpty()) {
            item {
                EmptyFinanceCard(
                    title = "Belum ada kantong tabungan",
                    subtitle = "Buat kantong seperti Dana Darurat atau Tabungan Harian agar uang tersimpan terpisah dari belanja.",
                    showIllustration = data.monthTransactions.isEmpty()
                )
            }
        } else {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(data.savingsGoals, key = { it.id }) { goal ->
                        val accent = parseHexColor(goal.colorHex, MaterialTheme.colorScheme.primary)
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .width(220.dp)
                                .clip(MaterialTheme.shapes.large)
                                .clickable { onDepositToGoal(goal) }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = accent.copy(alpha = 0.14f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = savingsIconForKey(goal.iconKey),
                                                contentDescription = null,
                                                tint = accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    SavingsProgressRing(
                                        progress = goal.progressFraction,
                                        accentColor = accent,
                                        size = 44.dp
                                    )
                                }
                                Text(
                                    text = goal.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatCompactRupiah(goal.currentAmount)} / ${formatCompactRupiah(goal.targetAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Transactions Preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Transaksi Terbaru", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onOpenExpensesTab) {
                    Text("Semua")
                }
            }
        }

        if (data.monthTransactions.isEmpty()) {
            item {
                EmptyFinanceCard(
                    title = "Belum ada transaksi di bulan ini",
                    subtitle = "Ketuk \"Catat Transaksi\" untuk mulai mencatat pengeluaran atau pemasukan Anda.",
                    showIllustration = data.savingsGoals.isNotEmpty()
                )
            }
        } else {
            items(data.monthTransactions.take(5), key = { it.id }) { tx ->
                TransactionRowCard(
                    transaction = tx,
                    onEdit = { onEditTransaction(tx) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}
