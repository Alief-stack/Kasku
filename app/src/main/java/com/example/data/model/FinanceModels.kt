package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class SavingsActionType {
    DEPOSIT,
    WITHDRAW
}

@Keep
@IgnoreExtraProperties
data class TransactionRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Long = 0L,
    val type: String = TransactionType.EXPENSE.name,
    val category: String = "Makanan",
    val note: String? = null,
    val occurredAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun transactionType(): TransactionType =
        TransactionType.entries.firstOrNull { it.name == type } ?: TransactionType.EXPENSE
}

@Keep
@IgnoreExtraProperties
data class SavingsGoal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val targetAmount: Long = 0L,
    val currentAmount: Long = 0L,
    val iconKey: String = "emergency",
    val colorHex: String = "#0F5243",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val progressFraction: Float
        get() = if (targetAmount <= 0L) 0f
        else (currentAmount.toFloat() / targetAmount.toFloat()).coerceIn(0f, 1f)
}

@Keep
@IgnoreExtraProperties
data class SavingsLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val goalId: String = "",
    val goalName: String = "",
    val amount: Long = 0L,
    val actionType: String = SavingsActionType.DEPOSIT.name,
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun actionEnum(): SavingsActionType =
        SavingsActionType.entries.firstOrNull { it.name == actionType } ?: SavingsActionType.DEPOSIT
}

@Keep
@IgnoreExtraProperties
data class MonthlyBudget(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "",
    val limitAmount: Long = 0L,
    val monthKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object FinanceCategories {
    val expenseCategories = listOf(
        "Makanan",
        "Transportasi",
        "Tagihan",
        "Belanja",
        "Kesehatan",
        "Hiburan",
        "Pendidikan",
        "Lainnya"
    )

    val incomeCategories = listOf(
        "Gaji",
        "Bonus",
        "Usaha",
        "Investasi",
        "Lainnya"
    )

    val savingsIconKeys = listOf(
        "emergency",
        "travel",
        "home",
        "education",
        "gadget",
        "investment",
        "general"
    )

    val savingsColorHexes = listOf(
        "#0F5243",
        "#0E7490",
        "#B4690E",
        "#4F46E5",
        "#BE123C",
        "#15803D"
    )
}
