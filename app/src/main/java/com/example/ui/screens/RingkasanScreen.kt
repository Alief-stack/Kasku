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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.components.MonthSwitcherBar
import com.example.ui.components.WeeklySpendBarChart
import com.example.ui.components.categoryColor
import com.example.ui.components.formatRupiah
import com.example.ui.state.FinanceDashboardData
import com.example.ui.theme.LocalFinanceColors
import kotlin.math.roundToInt

@Composable
fun RingkasanScreen(
    data: FinanceDashboardData,
    userName: String?,
    userEmail: String?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalFinanceColors.current
    var showSignOutConfirm by remember { mutableStateOf(false) }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Keluar dari KasKu?") },
            text = { Text("Semua catatan transaksi dan tabungan Anda tetap tersimpan aman di akun Google ini.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirm = false
                        onSignOutClick()
                    }
                ) { Text("Keluar") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Batal") }
            }
        )
    }

    val savingsRatePercent = if (data.monthIncome > 0L) {
        (((data.monthIncome - data.monthExpense).toFloat() / data.monthIncome.toFloat()) * 100f)
            .roundToInt()
            .coerceIn(-999, 100)
    } else 0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Ringkasan & Akun",
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

        // Financial Health Ratios
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
                    Text("Kesehatan Arus Kas", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Rasio Sisa Kas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = if (data.monthIncome > 0L) "$savingsRatePercent%" else "-",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (savingsRatePercent >= 20) financeColors.income else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = financeColors.savingsContainer,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Total Transaksi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${data.monthTransactions.size} catatan",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    val advice = when {
                        data.monthIncome == 0L && data.monthExpense == 0L ->
                            "Mulai catat pemasukan dan pengeluaran bulan ini untuk melihat analisis kesehatan kas Anda."
                        data.netCashflow < 0L ->
                            "Pengeluaran bulan ini melebihi pemasukan sebesar ${formatRupiah(-data.netCashflow)}. Evaluasi pos pengeluaran terbesar Anda."
                        savingsRatePercent >= 20 ->
                            "Bagus! Anda menyisihkan $savingsRatePercent% dari pemasukan bulan ini. Pertimbangkan memindahkan ke kantong tabungan."
                        else ->
                            "Arus kas positif ${formatRupiah(data.netCashflow)}. Target ideal tabungan bulanan adalah minimal 20% dari pemasukan."
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = advice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Weekly Spending Trend
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
                    Text("Tren Pengeluaran Mingguan", style = MaterialTheme.typography.titleMedium)
                    WeeklySpendBarChart(bars = data.weeklyTrend)
                }
            }
        }

        // Top Spending Categories
        if (data.categoryBreakdown.isNotEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Pos Pengeluaran Terbesar", style = MaterialTheme.typography.titleMedium)
                        data.categoryBreakdown.take(5).forEach { cat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(categoryColor(cat.category))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = cat.category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatRupiah(cat.amount),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Google Account & Sign Out Card
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName?.takeIf { it.isNotBlank() } ?: "Pengguna KasKu",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = userEmail?.takeIf { it.isNotBlank() } ?: "Tersinkron dengan Google",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showSignOutConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.cd_sign_out),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Keluar Akun")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}
