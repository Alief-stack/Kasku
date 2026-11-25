package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.FinanceCategories
import com.example.data.model.SavingsActionType
import com.example.data.model.SavingsGoal
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType

fun savingsIconForKey(iconKey: String): ImageVector {
    return when (iconKey) {
        "emergency" -> Icons.Filled.Shield
        "travel" -> Icons.Filled.Flight
        "home" -> Icons.Filled.Home
        "education" -> Icons.Filled.School
        "gadget" -> Icons.Filled.Laptop
        "investment" -> Icons.Filled.TrendingUp
        else -> Icons.Filled.AccountBalance
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionSheet(
    existing: TransactionRecord?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Long, type: TransactionType, category: String, note: String?, occurredAtEpochMs: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember { mutableStateOf(existing?.transactionType() ?: TransactionType.EXPENSE) }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var amountText by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    val categories = if (type == TransactionType.EXPENSE) {
        FinanceCategories.expenseCategories
    } else {
        FinanceCategories.incomeCategories
    }
    var category by remember {
        mutableStateOf(
            existing?.category?.takeIf { it in categories } ?: categories.first()
        )
    }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var occurredAt by remember { mutableLongStateOf(existing?.occurredAtEpochMs ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = occurredAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { occurredAt = it }
                    showDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (existing == null) "Catat Transaksi" else "Ubah Transaksi",
                style = MaterialTheme.typography.headlineSmall
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == TransactionType.EXPENSE,
                    onClick = {
                        type = TransactionType.EXPENSE
                        if (category !in FinanceCategories.expenseCategories) {
                            category = FinanceCategories.expenseCategories.first()
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("tx_type_expense")
                ) { Text("Pengeluaran") }
                SegmentedButton(
                    selected = type == TransactionType.INCOME,
                    onClick = {
                        type = TransactionType.INCOME
                        if (category !in FinanceCategories.incomeCategories) {
                            category = FinanceCategories.incomeCategories.first()
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("tx_type_income")
                ) { Text("Pemasukan") }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = input.filter { it.isDigit() }.take(12)
                    validationError = null
                },
                label = { Text("Nominal (Rp)") },
                placeholder = { Text("Contoh: 50000") },
                prefix = { Text("Rp ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(20_000L to "20rb", 50_000L to "50rb", 100_000L to "100rb", 250_000L to "250rb").forEach { (preset, label) ->
                    FilterChip(
                        selected = amountText == preset.toString(),
                        onClick = {
                            amountText = preset.toString()
                            validationError = null
                        },
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it.take(80)
                    validationError = null
                },
                label = { Text("Keterangan") },
                placeholder = { Text(if (type == TransactionType.EXPENSE) "Contoh: Makan siang" else "Contoh: Gaji bulanan") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_title_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Kategori",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item) }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Tanggal: ${formatShortDate(occurredAt)}")
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(200) },
                label = { Text("Catatan (opsional)") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f)
                ) { Text("Batal") }
                Button(
                    onClick = {
                        val parsedAmount = amountText.toLongOrNull() ?: 0L
                        val trimmedTitle = title.trim()
                        when {
                            parsedAmount <= 0L -> validationError = "Masukkan nominal lebih dari Rp 0."
                            trimmedTitle.isEmpty() -> validationError = "Masukkan keterangan transaksi."
                            else -> onSave(trimmedTitle, parsedAmount, type, category, note.trim().takeIf { it.isNotEmpty() }, occurredAt)
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tx_save_button")
                ) {
                    Text(if (isSubmitting) "Menyimpan..." else "Simpan")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditSavingsGoalSheet(
    existing: SavingsGoal?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, targetAmount: Long, initialAmount: Long, iconKey: String, colorHex: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var targetText by remember { mutableStateOf(existing?.targetAmount?.toString() ?: "") }
    var initialText by remember { mutableStateOf("0") }
    var iconKey by remember { mutableStateOf(existing?.iconKey ?: FinanceCategories.savingsIconKeys.first()) }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: FinanceCategories.savingsColorHexes.first()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (existing == null) "Buat Kantong Tabungan" else "Ubah Kantong Tabungan",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.take(60)
                    validationError = null
                },
                label = { Text("Nama Kantong Tabungan") },
                placeholder = { Text("Contoh: Dana Darurat, Laptop Baru") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_name_input")
            )

            OutlinedTextField(
                value = targetText,
                onValueChange = {
                    targetText = it.filter { ch -> ch.isDigit() }.take(12)
                    validationError = null
                },
                label = { Text("Target Tabungan (Rp)") },
                prefix = { Text("Rp ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_target_input")
            )

            if (existing == null) {
                OutlinedTextField(
                    value = initialText,
                    onValueChange = {
                        initialText = it.filter { ch -> ch.isDigit() }.take(12)
                        validationError = null
                    },
                    label = { Text("Saldo Awal (opsional)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ikon", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinanceCategories.savingsIconKeys.forEach { key ->
                        val selected = iconKey == key
                        FilterChip(
                            selected = selected,
                            onClick = { iconKey = key },
                            label = {
                                Icon(
                                    imageVector = savingsIconForKey(key),
                                    contentDescription = key,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Warna Kartu", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FinanceCategories.savingsColorHexes.forEach { hex ->
                        val color = parseHexColor(hex)
                        val selected = colorHex == hex
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { colorHex = hex }
                        ) {
                            if (selected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDismiss, enabled = !isSubmitting, modifier = Modifier.weight(1f)) {
                    Text("Batal")
                }
                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        val target = targetText.toLongOrNull() ?: 0L
                        val initial = initialText.toLongOrNull() ?: 0L
                        when {
                            trimmedName.isEmpty() -> validationError = "Masukkan nama kantong tabungan."
                            target <= 0L -> validationError = "Target tabungan harus lebih dari Rp 0."
                            else -> onSave(trimmedName, target, initial, iconKey, colorHex)
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("goal_save_button")
                ) {
                    Text(if (isSubmitting) "Menyimpan..." else "Simpan")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsMovementSheet(
    goal: SavingsGoal,
    initialAction: SavingsActionType,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, actionType: SavingsActionType, note: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var actionType by remember { mutableStateOf(initialAction) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = goal.name,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Saldo saat ini: ${formatRupiah(goal.currentAmount)} dari target ${formatRupiah(goal.targetAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = actionType == SavingsActionType.DEPOSIT,
                    onClick = {
                        actionType = SavingsActionType.DEPOSIT
                        validationError = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Setor Dana") }
                SegmentedButton(
                    selected = actionType == SavingsActionType.WITHDRAW,
                    onClick = {
                        actionType = SavingsActionType.WITHDRAW
                        validationError = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Tarik Dana") }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it.filter { ch -> ch.isDigit() }.take(12)
                    validationError = null
                },
                label = { Text("Nominal (Rp)") },
                prefix = { Text("Rp ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("movement_amount_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(50_000L to "50rb", 100_000L to "100rb", 500_000L to "500rb", 1_000_000L to "1jt").forEach { (preset, label) ->
                    FilterChip(
                        selected = amountText == preset.toString(),
                        onClick = {
                            amountText = preset.toString()
                            validationError = null
                        },
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(200) },
                label = { Text("Catatan (opsional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDismiss, enabled = !isSubmitting, modifier = Modifier.weight(1f)) {
                    Text("Batal")
                }
                Button(
                    onClick = {
                        val amt = amountText.toLongOrNull() ?: 0L
                        when {
                            amt <= 0L -> validationError = "Masukkan nominal lebih dari Rp 0."
                            actionType == SavingsActionType.WITHDRAW && amt > goal.currentAmount -> {
                                validationError = "Saldo tabungan tidak mencukupi (${formatRupiah(goal.currentAmount)})."
                            }
                            else -> onConfirm(amt, actionType, note.trim().takeIf { it.isNotEmpty() })
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("movement_confirm_button")
                ) {
                    Text(if (isSubmitting) "Memproses..." else if (actionType == SavingsActionType.DEPOSIT) "Setor" else "Tarik")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SetBudgetSheet(
    monthKey: String,
    initialCategory: String?,
    initialLimit: Long?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (category: String, limitAmount: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var category by remember { mutableStateOf(initialCategory ?: FinanceCategories.expenseCategories.first()) }
    var limitText by remember { mutableStateOf(initialLimit?.toString() ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Atur Anggaran (${formatMonthLabel(monthKey)})",
                style = MaterialTheme.typography.headlineSmall
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kategori Pengeluaran", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FinanceCategories.expenseCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { if (initialCategory == null) category = cat },
                            enabled = initialCategory == null || initialCategory == cat,
                            label = { Text(cat) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = limitText,
                onValueChange = {
                    limitText = it.filter { ch -> ch.isDigit() }.take(12)
                    validationError = null
                },
                label = { Text("Batas Anggaran Bulanan (Rp)") },
                prefix = { Text("Rp ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_limit_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(500_000L to "500rb", 1_000_000L to "1jt", 2_000_000L to "2jt", 3_000_000L to "3jt").forEach { (preset, label) ->
                    FilterChip(
                        selected = limitText == preset.toString(),
                        onClick = {
                            limitText = preset.toString()
                            validationError = null
                        },
                        label = { Text(label) }
                    )
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDismiss, enabled = !isSubmitting, modifier = Modifier.weight(1f)) {
                    Text("Batal")
                }
                Button(
                    onClick = {
                        val limit = limitText.toLongOrNull() ?: 0L
                        if (limit <= 0L) {
                            validationError = "Masukkan batas anggaran lebih dari Rp 0."
                        } else {
                            onSave(category, limit)
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("budget_save_button")
                ) {
                    Text(if (isSubmitting) "Menyimpan..." else "Simpan")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
