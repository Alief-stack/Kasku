package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.MonthlyBudget
import com.example.data.model.SavingsActionType
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsLog
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FinanceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Silakan masuk dengan Google sebelum menyimpan data.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeTransactions(userId: String): Flow<List<TransactionRecord>> = flow {
        require(userId.isNotBlank()) { "User ID kosong." }
        val path = "users/$userId/transactions"
        emitAll(
            userDoc(userId).collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TransactionRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.occurredAtEpochMs }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSavingsGoals(userId: String): Flow<List<SavingsGoal>> = flow {
        require(userId.isNotBlank()) { "User ID kosong." }
        val path = "users/$userId/savingsGoals"
        emitAll(
            userDoc(userId).collection("savingsGoals")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SavingsGoal::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSavingsLogs(userId: String): Flow<List<SavingsLog>> = flow {
        require(userId.isNotBlank()) { "User ID kosong." }
        val path = "users/$userId/savingsLogs"
        emitAll(
            userDoc(userId).collection("savingsLogs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SavingsLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeBudgets(userId: String): Flow<List<MonthlyBudget>> = flow {
        require(userId.isNotBlank()) { "User ID kosong." }
        val path = "users/$userId/budgets"
        emitAll(
            userDoc(userId).collection("budgets")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MonthlyBudget::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.category }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addTransaction(
        title: String,
        amount: Long,
        type: TransactionType,
        category: String,
        note: String?,
        occurredAtEpochMs: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/transactions"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanCategory = category.trim()
            val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }
            require(cleanTitle.length in 1..80) { "Judul transaksi harus 1-80 karakter." }
            require(amount in 1L..1_000_000_000_000L) { "Nominal harus lebih dari 0." }
            require(cleanCategory.length in 1..40) { "Kategori tidak valid." }
            require(cleanNote == null || cleanNote.length <= 200) { "Catatan maksimal 200 karakter." }
            require(occurredAtEpochMs > 0L) { "Tanggal transaksi tidak valid." }

            val docRef = userDoc(uid).collection("transactions").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "amount" to amount,
                "type" to type.name,
                "category" to cleanCategory,
                "note" to cleanNote,
                "occurredAtEpochMs" to occurredAtEpochMs,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(
        transactionId: String,
        title: String,
        amount: Long,
        type: TransactionType,
        category: String,
        note: String?,
        occurredAtEpochMs: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/transactions/$transactionId"
        return try {
            val uid = requireUserId()
            require(transactionId.isNotBlank()) { "ID transaksi kosong." }
            val cleanTitle = title.trim()
            val cleanCategory = category.trim()
            val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }
            require(cleanTitle.length in 1..80) { "Judul transaksi harus 1-80 karakter." }
            require(amount in 1L..1_000_000_000_000L) { "Nominal harus lebih dari 0." }
            require(cleanCategory.length in 1..40) { "Kategori tidak valid." }
            require(cleanNote == null || cleanNote.length <= 200) { "Catatan maksimal 200 karakter." }
            require(occurredAtEpochMs > 0L) { "Tanggal transaksi tidak valid." }

            val docRef = userDoc(uid).collection("transactions").document(transactionId)
            docPath = docRef.path
            val updates = mutableMapOf<String, Any>(
                "title" to cleanTitle,
                "amount" to amount,
                "type" to type.name,
                "category" to cleanCategory,
                "occurredAtEpochMs" to occurredAtEpochMs,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanNote != null) {
                updates["note"] = cleanNote
            } else {
                updates["note"] = FieldValue.delete()
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/transactions/$transactionId"
        return try {
            val uid = requireUserId()
            require(transactionId.isNotBlank()) { "ID transaksi kosong." }
            val docRef = userDoc(uid).collection("transactions").document(transactionId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addSavingsGoal(
        name: String,
        targetAmount: Long,
        initialAmount: Long,
        iconKey: String,
        colorHex: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/savingsGoals"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim()
            require(cleanName.length in 1..60) { "Nama tabungan harus 1-60 karakter." }
            require(targetAmount in 1L..1_000_000_000_000L) { "Target tabungan harus lebih dari 0." }
            require(initialAmount in 0L..1_000_000_000_000L) { "Saldo awal tidak valid." }

            val goalRef = userDoc(uid).collection("savingsGoals").document()
            docPath = goalRef.path
            val batch = db.batch()
            val goalPayload = mapOf(
                "userId" to uid,
                "name" to cleanName,
                "targetAmount" to targetAmount,
                "currentAmount" to initialAmount,
                "iconKey" to iconKey.trim().take(30),
                "colorHex" to colorHex.trim().take(16),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(goalRef, goalPayload)

            if (initialAmount > 0L) {
                val logRef = userDoc(uid).collection("savingsLogs").document()
                val logPayload = mapOf(
                    "userId" to uid,
                    "goalId" to goalRef.id,
                    "goalName" to cleanName,
                    "amount" to initialAmount,
                    "actionType" to SavingsActionType.DEPOSIT.name,
                    "note" to "Saldo awal kantong tabungan",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                batch.set(logRef, logPayload)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSavingsGoal(
        goalId: String,
        name: String,
        targetAmount: Long,
        iconKey: String,
        colorHex: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/savingsGoals/$goalId"
        return try {
            val uid = requireUserId()
            require(goalId.isNotBlank()) { "ID tabungan kosong." }
            val cleanName = name.trim()
            require(cleanName.length in 1..60) { "Nama tabungan harus 1-60 karakter." }
            require(targetAmount in 1L..1_000_000_000_000L) { "Target tabungan harus lebih dari 0." }

            val docRef = userDoc(uid).collection("savingsGoals").document(goalId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "name" to cleanName,
                    "targetAmount" to targetAmount,
                    "iconKey" to iconKey.trim().take(30),
                    "colorHex" to colorHex.trim().take(16),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSavingsGoal(goalId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/savingsGoals/$goalId"
        return try {
            val uid = requireUserId()
            require(goalId.isNotBlank()) { "ID tabungan kosong." }
            val docRef = userDoc(uid).collection("savingsGoals").document(goalId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordSavingsMovement(
        goal: SavingsGoal,
        amount: Long,
        actionType: SavingsActionType,
        note: String?
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/savingsGoals/${goal.id}"
        return try {
            val uid = requireUserId()
            require(goal.id.isNotBlank()) { "ID tabungan kosong." }
            require(amount in 1L..1_000_000_000_000L) { "Nominal harus lebih dari 0." }
            if (actionType == SavingsActionType.WITHDRAW) {
                require(amount <= goal.currentAmount) {
                    "Saldo tabungan tidak mencukupi untuk penarikan ini."
                }
            }
            val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }
            require(cleanNote == null || cleanNote.length <= 200) { "Catatan maksimal 200 karakter." }

            val delta = if (actionType == SavingsActionType.DEPOSIT) amount else -amount
            val goalRef = userDoc(uid).collection("savingsGoals").document(goal.id)
            val logRef = userDoc(uid).collection("savingsLogs").document()
            docPath = goalRef.path

            val batch = db.batch()
            batch.update(
                goalRef,
                mapOf(
                    "currentAmount" to FieldValue.increment(delta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val logPayload = mapOf(
                "userId" to uid,
                "goalId" to goal.id,
                "goalName" to goal.name.take(60),
                "amount" to amount,
                "actionType" to actionType.name,
                "note" to cleanNote,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            batch.set(logRef, logPayload)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertBudget(
        category: String,
        monthKey: String,
        limitAmount: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/budgets"
        return try {
            val uid = requireUserId()
            val cleanCategory = category.trim()
            require(cleanCategory.length in 1..40) { "Kategori tidak valid." }
            require(monthKey.matches(Regex("^[0-9]{4}-[0-9]{2}$"))) { "Format bulan tidak valid." }
            require(limitAmount in 1L..1_000_000_000_000L) { "Batas anggaran harus lebih dari 0." }

            val slug = cleanCategory.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            val budgetId = "budget_${slug}_${monthKey.replace("-", "_")}"
            val docRef = userDoc(uid).collection("budgets").document(budgetId)
            docPath = docRef.path

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "limitAmount" to limitAmount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "category" to cleanCategory,
                        "limitAmount" to limitAmount,
                        "monthKey" to monthKey,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteBudget(budgetId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/budgets/$budgetId"
        return try {
            val uid = requireUserId()
            require(budgetId.isNotBlank()) { "ID anggaran kosong." }
            val docRef = userDoc(uid).collection("budgets").document(budgetId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
