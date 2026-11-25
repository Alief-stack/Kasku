package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FinanceRepository
import com.example.data.model.MonthlyBudget
import com.example.data.model.SavingsActionType
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsLog
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.components.currentMonthKey
import com.example.ui.components.epochMsToMonthKey
import com.example.ui.components.shiftMonthKey
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class CategorySpend(
    val category: String,
    val amount: Long,
    val fraction: Float
)

data class BudgetStatus(
    val budget: MonthlyBudget,
    val spentAmount: Long
) {
    val progress: Float
        get() = if (budget.limitAmount <= 0L) 0f
        else (spentAmount.toFloat() / budget.limitAmount.toFloat()).coerceAtLeast(0f)

    val isOverBudget: Boolean
        get() = spentAmount > budget.limitAmount

    val isNearLimit: Boolean
        get() = !isOverBudget && progress >= 0.8f
}

data class DailySpend(
    val dayLabel: String,
    val amount: Long
)

data class FinanceDashboardData(
    val selectedMonthKey: String,
    val allTransactions: List<TransactionRecord>,
    val monthTransactions: List<TransactionRecord>,
    val monthExpense: Long,
    val monthIncome: Long,
    val netCashflow: Long,
    val savingsGoals: List<SavingsGoal>,
    val savingsLogs: List<SavingsLog>,
    val totalSavings: Long,
    val totalSavingsTarget: Long,
    val monthBudgets: List<BudgetStatus>,
    val totalBudgetLimit: Long,
    val totalBudgetSpent: Long,
    val categoryBreakdown: List<CategorySpend>,
    val weeklyTrend: List<DailySpend>
)

class FinanceViewModel(
    private val repository: FinanceRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedMonthKey = MutableStateFlow(currentMonthKey())
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val uiState: StateFlow<UiState<FinanceDashboardData>> = combine(
        repository.observeTransactions(currentUserId),
        repository.observeSavingsGoals(currentUserId),
        repository.observeSavingsLogs(currentUserId),
        repository.observeBudgets(currentUserId),
        _selectedMonthKey
    ) { transactions, goals, logs, budgets, monthKey ->
        val monthTxs = transactions.filter { epochMsToMonthKey(it.occurredAtEpochMs) == monthKey }
        val monthExpenses = monthTxs.filter { it.transactionType() == TransactionType.EXPENSE }
        val monthIncomes = monthTxs.filter { it.transactionType() == TransactionType.INCOME }

        val totalExpense = monthExpenses.sumOf { it.amount }
        val totalIncome = monthIncomes.sumOf { it.amount }
        val totalSavings = goals.sumOf { it.currentAmount }
        val totalTarget = goals.sumOf { it.targetAmount }

        val groupedCategory = monthExpenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }

        val breakdown = groupedCategory.map { (cat, amt) ->
            CategorySpend(
                category = cat,
                amount = amt,
                fraction = if (totalExpense > 0L) amt.toFloat() / totalExpense.toFloat() else 0f
            )
        }

        val spentByCat = groupedCategory.associate { it.key to it.value }
        val monthBudgets = budgets.filter { it.monthKey == monthKey }.map { budget ->
            BudgetStatus(
                budget = budget,
                spentAmount = spentByCat[budget.category] ?: 0L
            )
        }.sortedByDescending { it.progress }

        val weekBuckets = LongArray(4)
        val cal = Calendar.getInstance()
        for (tx in monthExpenses) {
            cal.timeInMillis = tx.occurredAtEpochMs
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val idx = when {
                day <= 7 -> 0
                day <= 14 -> 1
                day <= 21 -> 2
                else -> 3
            }
            weekBuckets[idx] += tx.amount
        }
        val weeklyTrend = listOf(
            DailySpend("Mg 1", weekBuckets[0]),
            DailySpend("Mg 2", weekBuckets[1]),
            DailySpend("Mg 3", weekBuckets[2]),
            DailySpend("Mg 4+", weekBuckets[3])
        )

        UiState.Success(
            FinanceDashboardData(
                selectedMonthKey = monthKey,
                allTransactions = transactions,
                monthTransactions = monthTxs,
                monthExpense = totalExpense,
                monthIncome = totalIncome,
                netCashflow = totalIncome - totalExpense,
                savingsGoals = goals,
                savingsLogs = logs,
                totalSavings = totalSavings,
                totalSavingsTarget = totalTarget,
                monthBudgets = monthBudgets,
                totalBudgetLimit = monthBudgets.sumOf { it.budget.limitAmount },
                totalBudgetSpent = monthBudgets.sumOf { it.spentAmount },
                categoryBreakdown = breakdown,
                weeklyTrend = weeklyTrend
            )
        ) as UiState<FinanceDashboardData>
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing finance state", rootError)
        emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat data keuangan."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    fun previousMonth() {
        _selectedMonthKey.value = shiftMonthKey(_selectedMonthKey.value, -1)
    }

    fun nextMonth() {
        _selectedMonthKey.value = shiftMonthKey(_selectedMonthKey.value, 1)
    }

    fun addTransaction(
        title: String,
        amount: Long,
        type: TransactionType,
        category: String,
        note: String?,
        occurredAtEpochMs: Long,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.addTransaction(title, amount, type, category, note, occurredAtEpochMs)
            _isSubmitting.value = false
            result.onSuccess {
                _events.emit("Transaksi berhasil dicatat.")
                onComplete()
            }.onFailure {
                _events.emit(it.message ?: "Gagal mencatat transaksi.")
            }
        }
    }

    fun updateTransaction(
        transactionId: String,
        title: String,
        amount: Long,
        type: TransactionType,
        category: String,
        note: String?,
        occurredAtEpochMs: Long,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.updateTransaction(
                transactionId, title, amount, type, category, note, occurredAtEpochMs
            )
            _isSubmitting.value = false
            result.onSuccess {
                _events.emit("Transaksi diperbarui.")
                onComplete()
            }.onFailure {
                _events.emit(it.message ?: "Gagal memperbarui transaksi.")
            }
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            val result = repository.deleteTransaction(transactionId)
            result.onSuccess {
                _events.emit("Transaksi dihapus.")
            }.onFailure {
                _events.emit(it.message ?: "Gagal menghapus transaksi.")
            }
        }
    }

    fun addSavingsGoal(
        name: String,
        targetAmount: Long,
        initialAmount: Long,
        iconKey: String,
        colorHex: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.addSavingsGoal(name, targetAmount, initialAmount, iconKey, colorHex)
            _isSubmitting.value = false
            result.onSuccess {
                _events.emit("Kantong tabungan \"$name\" dibuat.")
                onComplete()
            }.onFailure {
                _events.emit(it.message ?: "Gagal membuat kantong tabungan.")
            }
        }
    }

    fun updateSavingsGoal(
        goalId: String,
        name: String,
        targetAmount: Long,
        iconKey: String,
        colorHex: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.updateSavingsGoal(goalId, name, targetAmount, iconKey, colorHex)
            _isSubmitting.value = false
            result.onSuccess {
                _events.emit("Target tabungan diperbarui.")
                onComplete()
            }.onFailure {
                _events.emit(it.message ?: "Gagal memperbarui tabungan.")
            }
        }
    }

    fun deleteSavingsGoal(goalId: String) {
        viewModelScope.launch {
            val result = repository.deleteSavingsGoal(goalId)
            result.onSuccess {
                _events.emit("Kantong tabungan dihapus.")
            }.onFailure {
                _events.emit(it.message ?: "Gagal menghapus kantong tabungan.")
            }
        }
    }

    fun recordSavingsMovement(
        goal: SavingsGoal,
        amount: Long,
        actionType: SavingsActionType,
        note: String?,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.recordSavingsMovement(goal, amount, actionType, note)
            _isSubmitting.value = false
            result.onSuccess {
                val msg = if (actionType == SavingsActionType.DEPOSIT) {
                    "Setoran ke ${goal.name} berhasil."
                } else {
                    "Penarikan dari ${goal.name} berhasil."
                }
                _events.emit(msg)
                onComplete()
            }.onFailure {
                _events.emit(it.message ?: "Gagal memproses saldo tabungan.")
            }
        }
    }

    fun upsertBudget(
        category: String,
        limitAmount: Long,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.upsertBudget(category, _selectedMonthKey.value, limitAmount)
            _isSubmitting.value = false
            result.onSuccess {
                _events.emit("Anggaran $category disimpan.")
                onComplete()
            }.onFailure {
                _events.emit(it.message ?: "Gagal menyimpan anggaran.")
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            val result = repository.deleteBudget(budgetId)
            result.onSuccess {
                _events.emit("Anggaran kategori dihapus.")
            }.onFailure {
                _events.emit(it.message ?: "Gagal menghapus anggaran.")
            }
        }
    }

    private companion object {
        const val TAG = "FinanceVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
