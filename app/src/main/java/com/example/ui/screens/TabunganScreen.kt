package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.SavingsActionType
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsLog
import com.example.ui.components.EmptyFinanceCard
import com.example.ui.components.SavingsProgressRing
import com.example.ui.components.formatRupiah
import com.example.ui.components.formatShortDate
import com.example.ui.components.parseHexColor
import com.example.ui.components.savingsIconForKey
import com.example.ui.state.FinanceDashboardData
import com.example.ui.theme.LocalFinanceColors

@Composable
fun TabunganScreen(
    data: FinanceDashboardData,
    onCreateGoal: () -> Unit,
    onEditGoal: (SavingsGoal) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onDeposit: (SavingsGoal) -> Unit,
    onWithdraw: (SavingsGoal) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Penyimpanan & Tabungan",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${data.savingsGoals.size} kantong aktif",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = onCreateGoal,
                    modifier = Modifier.testTag("create_savings_goal_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kantong Baru")
                }
            }
        }

        // Total Vault Summary Card
        item {
            val overallProgress = if (data.totalSavingsTarget > 0L) {
                (data.totalSavings.toFloat() / data.totalSavingsTarget.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Total Dana Tersimpan",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                    Text(
                        text = formatRupiah(data.totalSavings),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    if (data.totalSavingsTarget > 0L) {
                        LinearProgressIndicator(
                            progress = { overallProgress },
                            color = MaterialTheme.colorScheme.onPrimary,
                            trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                        )
                        Text(
                            text = "Target gabungan: ${formatRupiah(data.totalSavingsTarget)} (${(overallProgress * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        if (data.savingsGoals.isEmpty()) {
            item {
                EmptyFinanceCard(
                    title = "Belum ada kantong tabungan",
                    subtitle = "Buat kantong penyimpanan pribadi untuk Dana Darurat, Target Pembelian, atau Tabungan Masa Depan.",
                    showIllustration = true
                )
            }
        } else {
            items(data.savingsGoals, key = { it.id }) { goal ->
                SavingsGoalCard(
                    goal = goal,
                    onEdit = { onEditGoal(goal) },
                    onDelete = { onDeleteGoal(goal.id) },
                    onDeposit = { onDeposit(goal) },
                    onWithdraw = { onWithdraw(goal) }
                )
            }
        }

        if (data.savingsLogs.isNotEmpty()) {
            item {
                Text(
                    text = "Riwayat Setor & Tarik",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(data.savingsLogs.take(12), key = { it.id }) { log ->
                SavingsLogRow(log = log)
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoal,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit
) {
    val accent = parseHexColor(goal.colorHex, MaterialTheme.colorScheme.primary)
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("savings_goal_card_${goal.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.14f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = savingsIconForKey(goal.iconKey),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Terkumpul ${formatRupiah(goal.currentAmount)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                    Text(
                        text = "Target ${formatRupiah(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SavingsProgressRing(progress = goal.progressFraction, accentColor = accent)
            }

            LinearProgressIndicator(
                progress = { goal.progressFraction },
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onDeposit,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("deposit_goal_${goal.id}")
                ) {
                    Text("Setor")
                }
                OutlinedButton(
                    onClick = onWithdraw,
                    enabled = goal.currentAmount > 0L,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("withdraw_goal_${goal.id}")
                ) {
                    Text("Tarik")
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_edit_item))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_delete_item),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SavingsLogRow(log: SavingsLog) {
    val financeColors = LocalFinanceColors.current
    val isDeposit = log.actionEnum() == SavingsActionType.DEPOSIT
    val color = if (isDeposit) financeColors.income else financeColors.expense
    val container = if (isDeposit) financeColors.incomeContainer else financeColors.expenseContainer
    val dateMs = log.createdAt?.toDate()?.time ?: System.currentTimeMillis()

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(container)
            ) {
                Icon(
                    imageVector = if (isDeposit) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                    contentDescription = if (isDeposit) "Setoran" else "Penarikan",
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${if (isDeposit) "Setor ke" else "Tarik dari"} ${log.goalName}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(formatShortDate(dateMs))
                        if (!log.note.isNullOrBlank()) {
                            append(" • ")
                            append(log.note)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = (if (isDeposit) "+" else "-") + formatRupiah(log.amount),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}
