package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import kotlin.random.Random

class SafaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing SafaOne.")
    }

    private fun userDocRef(uid: String) = db.collection("users").document(uid)

    fun observeProfile(userId: String): Flow<AccountProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            userDocRef(userId)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(AccountProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun fetchProfileFromServer(userId: String): Result<AccountProfile?> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Authenticated user mismatch" }
            val snap = userDocRef(uid).get().await()
            val profile = if (snap.exists()) {
                snap.toObject(AccountProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    fun observeTransactions(userId: String): Flow<List<WalletTransaction>> = flow {
        val path = "users/$userId/transactions"
        emitAll(
            userDocRef(userId).collection("transactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeActiveBundles(userId: String): Flow<List<ActiveBundle>> = flow {
        val path = "users/$userId/active_bundles"
        emitAll(
            userDocRef(userId).collection("active_bundles")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ActiveBundle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSavedPayees(userId: String): Flow<List<SavedPayee>> = flow {
        val path = "users/$userId/saved_payees"
        emitAll(
            userDocRef(userId).collection("saved_payees")
                .orderBy("name", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SavedPayee::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureProfileInitialized(
        userId: String,
        rawDisplayName: String?,
        preferredPhone: String? = null,
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Authenticated user mismatch" }
            val docRef = userDocRef(uid)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                return Result.success(Unit)
            }

            val safeName = rawDisplayName?.trim()?.takeIf { it.isNotEmpty() }?.take(80) ?: "SafaOne Member"
            val safePhone = preferredPhone?.trim()?.takeIf { it.length in 7..20 } ?: "0712 345 678"
            val batch = db.batch()

            val profilePayload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "phoneNumber" to safePhone,
                "planName" to "SafaOne PrePay 5G",
                "airtimeBalance" to 245.50,
                "mpesaBalance" to 12450.0,
                "savingsBalance" to 3500.0,
                "fulizaLimit" to 5000.0,
                "fulizaUsed" to 0.0,
                "dataBalanceMb" to 4250L,
                "minutesBalance" to 145L,
                "smsBalance" to 420L,
                "bongaPoints" to 1280L,
                "fibrePlanName" to "Gold 60 Mbps",
                "fibreMonthlyFee" to 6299.0,
                "fibreActive" to true,
                "fibreExpiryLabel" to "Renews in 18 days",
                "hideBalances" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            batch.set(docRef, profilePayload)

            val welcomeTxRef = docRef.collection("transactions").document()
            batch.set(
                welcomeTxRef,
                mapOf(
                    "userId" to uid,
                    "receiptCode" to generateReceiptCode(),
                    "type" to "DEPOSIT",
                    "title" to "M-Pesa Opening Wallet Credit",
                    "counterparty" to "Agent 204810 • Nairobi",
                    "amount" to 12450.0,
                    "fee" to 0.0,
                    "direction" to "CREDIT",
                    "source" to "MPESA",
                    "balanceAfter" to 12450.0,
                    "note" to "Welcome to SafaOne",
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )

            val starterBundleRef = docRef.collection("active_bundles").document()
            batch.set(
                starterBundleRef,
                mapOf(
                    "userId" to uid,
                    "name" to "All-in-One 500 Starter",
                    "category" to "ALL_IN_ONE",
                    "dataMb" to 4250L,
                    "minutes" to 145L,
                    "smsCount" to 420L,
                    "validityLabel" to "Valid 30 days",
                    "priceKes" to 500.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )

            val kplcRef = docRef.collection("saved_payees").document()
            batch.set(
                kplcRef,
                mapOf(
                    "userId" to uid,
                    "name" to "Kenya Power Prepaid",
                    "payeeType" to "PAYBILL",
                    "identifier" to "888880",
                    "accountRef" to "1420988711",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )

            val waterRef = docRef.collection("saved_payees").document()
            batch.set(
                waterRef,
                mapOf(
                    "userId" to uid,
                    "name" to "Nairobi Water",
                    "payeeType" to "PAYBILL",
                    "identifier" to "444400",
                    "accountRef" to "8901245",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleHideBalances(userId: String, hide: Boolean): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            userDocRef(uid).update(
                mapOf(
                    "hideBalances" to hide,
                    "updatedAt" to FieldValue.serverTimestamp(),
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

    suspend fun updateProfileDetails(
        userId: String,
        displayName: String,
        phoneNumber: String,
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanName = displayName.trim()
            val cleanPhone = phoneNumber.trim()
            require(cleanName.length in 1..80) { "Display name must be between 1 and 80 characters." }
            require(cleanPhone.length in 7..20) { "Phone number must be between 7 and 20 characters." }
            userDocRef(uid).update(
                mapOf(
                    "displayName" to cleanName,
                    "phoneNumber" to cleanPhone,
                    "updatedAt" to FieldValue.serverTimestamp(),
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

    suspend fun executeMpesaDebit(
        profile: AccountProfile,
        type: String,
        title: String,
        counterparty: String,
        amount: Double,
        fee: Double,
        note: String?,
        allowFuliza: Boolean,
        extraAirtimeCredit: Double = 0.0,
        bongaEarned: Long = 10L,
        fibrePlan: FibrePlan? = null,
    ): Result<String> {
        val docPath = "users/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId)
            require(amount > 0.0 && amount <= 1_000_000.0) { "Enter a valid amount between KES 1 and 1,000,000." }
            val cleanTitle = title.trim().take(80)
            val cleanCounterparty = counterparty.trim().take(80)
            require(cleanTitle.isNotEmpty() && cleanCounterparty.isNotEmpty()) { "Recipient details are required." }

            val totalCost = amount + fee
            val availableWallet = profile.mpesaBalance
            val fulizaAvailable = max(0.0, profile.fulizaLimit - profile.fulizaUsed)

            val newMpesaBalance: Double
            val newFulizaUsed: Double
            if (availableWallet >= totalCost) {
                newMpesaBalance = availableWallet - totalCost
                newFulizaUsed = profile.fulizaUsed
            } else if (allowFuliza && (availableWallet + fulizaAvailable) >= totalCost) {
                val shortfall = totalCost - availableWallet
                newMpesaBalance = 0.0
                newFulizaUsed = profile.fulizaUsed + shortfall
            } else {
                val msg = if (allowFuliza) {
                    "Insufficient M-Pesa & Fuliza funds. Need KES %.2f".format(totalCost)
                } else {
                    "Insufficient M-Pesa balance (KES %.2f). Enable Fuliza or Deposit.".format(availableWallet)
                }
                return Result.failure(IllegalStateException(msg))
            }

            val receiptCode = generateReceiptCode()
            val batch = db.batch()
            val updates = mutableMapOf<String, Any>(
                "mpesaBalance" to newMpesaBalance,
                "fulizaUsed" to newFulizaUsed,
                "bongaPoints" to (profile.bongaPoints + bongaEarned),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (extraAirtimeCredit > 0.0) {
                updates["airtimeBalance"] = profile.airtimeBalance + extraAirtimeCredit
            }
            if (fibrePlan != null) {
                updates["fibrePlanName"] = fibrePlan.name
                updates["fibreMonthlyFee"] = fibrePlan.monthlyFee
                updates["fibreActive"] = true
                updates["fibreExpiryLabel"] = "Active • 30 days left"
            }

            batch.update(userDocRef(uid), updates)

            val txRef = userDocRef(uid).collection("transactions").document()
            val txPayload = buildMap<String, Any?> {
                put("userId", uid)
                put("receiptCode", receiptCode)
                put("type", type)
                put("title", cleanTitle)
                put("counterparty", cleanCounterparty)
                put("amount", amount)
                put("fee", fee)
                put("direction", "DEBIT")
                put("source", "MPESA")
                put("balanceAfter", newMpesaBalance)
                put("note", note?.trim()?.takeIf { it.isNotEmpty() }?.take(160))
                put("createdAt", FieldValue.serverTimestamp())
            }.filterValues { it != null } as Map<String, Any>

            batch.set(txRef, txPayload)
            batch.commit().await()
            Result.success(receiptCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun executeDeposit(
        profile: AccountProfile,
        amount: Double,
        agentLabel: String,
    ): Result<String> {
        val docPath = "users/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId)
            require(amount >= 10.0 && amount <= 500_000.0) { "Deposit amount must be between KES 10 and 500,000." }

            val repayFuliza = minOf(profile.fulizaUsed, amount)
            val newFulizaUsed = profile.fulizaUsed - repayFuliza
            val newMpesaBalance = profile.mpesaBalance + (amount - repayFuliza)

            val receiptCode = generateReceiptCode()
            val batch = db.batch()
            batch.update(
                userDocRef(uid),
                mapOf(
                    "mpesaBalance" to newMpesaBalance,
                    "fulizaUsed" to newFulizaUsed,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            val txRef = userDocRef(uid).collection("transactions").document()
            val noteText = if (repayFuliza > 0) "Auto-repaid KES %.2f Fuliza".format(repayFuliza) else null
            val txPayload = buildMap<String, Any?> {
                put("userId", uid)
                put("receiptCode", receiptCode)
                put("type", "DEPOSIT")
                put("title", "Cash Deposit")
                put("counterparty", agentLabel.trim().ifEmpty { "M-Pesa Agent" }.take(80))
                put("amount", amount)
                put("fee", 0.0)
                put("direction", "CREDIT")
                put("source", "MPESA")
                put("balanceAfter", newMpesaBalance)
                put("note", noteText)
                put("createdAt", FieldValue.serverTimestamp())
            }.filterValues { it != null } as Map<String, Any>

            batch.set(txRef, txPayload)
            batch.commit().await()
            Result.success(receiptCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun executeTransferSavings(
        profile: AccountProfile,
        toSavings: Boolean,
        amount: Double,
    ): Result<String> {
        val docPath = "users/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId)
            require(amount >= 10.0 && amount <= 500_000.0) { "Amount must be at least KES 10." }

            val newMpesa: Double
            val newSavings: Double
            if (toSavings) {
                if (profile.mpesaBalance < amount) {
                    return Result.failure(IllegalStateException("Insufficient M-Pesa balance."))
                }
                newMpesa = profile.mpesaBalance - amount
                newSavings = profile.savingsBalance + amount
            } else {
                if (profile.savingsBalance < amount) {
                    return Result.failure(IllegalStateException("Insufficient Savings Vault balance."))
                }
                newSavings = profile.savingsBalance - amount
                newMpesa = profile.mpesaBalance + amount
            }

            val receiptCode = generateReceiptCode()
            val batch = db.batch()
            batch.update(
                userDocRef(uid),
                mapOf(
                    "mpesaBalance" to newMpesa,
                    "savingsBalance" to newSavings,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            val txRef = userDocRef(uid).collection("transactions").document()
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "receiptCode" to receiptCode,
                    "type" to if (toSavings) "SAVINGS_IN" else "SAVINGS_OUT",
                    "title" to if (toSavings) "Moved to Savings Vault" else "Withdrawn from Savings",
                    "counterparty" to "SafaOne M-Shwari Lock",
                    "amount" to amount,
                    "fee" to 0.0,
                    "direction" to if (toSavings) "DEBIT" else "CREDIT",
                    "source" to "MPESA",
                    "balanceAfter" to newMpesa,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(receiptCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun executeBuyBundle(
        profile: AccountProfile,
        offer: BundleOffer,
        payWithMpesa: Boolean,
    ): Result<String> {
        val docPath = "users/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId)
            val newMpesa: Double
            val newAirtime: Double
            val sourceLabel: String
            val balanceAfter: Double

            if (payWithMpesa) {
                if (profile.mpesaBalance < offer.priceKes) {
                    return Result.failure(IllegalStateException("Insufficient M-Pesa balance for ${offer.name}."))
                }
                newMpesa = profile.mpesaBalance - offer.priceKes
                newAirtime = profile.airtimeBalance
                sourceLabel = "MPESA"
                balanceAfter = newMpesa
            } else {
                if (profile.airtimeBalance < offer.priceKes) {
                    return Result.failure(IllegalStateException("Insufficient Airtime balance for ${offer.name}."))
                }
                newMpesa = profile.mpesaBalance
                newAirtime = profile.airtimeBalance - offer.priceKes
                sourceLabel = "AIRTIME"
                balanceAfter = newAirtime
            }

            val receiptCode = generateReceiptCode()
            val batch = db.batch()
            batch.update(
                userDocRef(uid),
                mapOf(
                    "mpesaBalance" to newMpesa,
                    "airtimeBalance" to newAirtime,
                    "dataBalanceMb" to (profile.dataBalanceMb + offer.dataMb),
                    "minutesBalance" to (profile.minutesBalance + offer.minutes),
                    "smsBalance" to (profile.smsBalance + offer.smsCount),
                    "bongaPoints" to (profile.bongaPoints + max(5L, (offer.priceKes / 10.0).toLong())),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )

            val bundleRef = userDocRef(uid).collection("active_bundles").document()
            batch.set(
                bundleRef,
                mapOf(
                    "userId" to uid,
                    "name" to offer.name.take(60),
                    "category" to offer.category,
                    "dataMb" to offer.dataMb,
                    "minutes" to offer.minutes,
                    "smsCount" to offer.smsCount,
                    "validityLabel" to offer.validityLabel.take(30),
                    "priceKes" to offer.priceKes,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )

            val txRef = userDocRef(uid).collection("transactions").document()
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "receiptCode" to receiptCode,
                    "type" to "BUY_BUNDLE",
                    "title" to offer.name.take(80),
                    "counterparty" to "SafaOne Bundles (${offer.validityLabel})",
                    "amount" to offer.priceKes,
                    "fee" to 0.0,
                    "direction" to "DEBIT",
                    "source" to sourceLabel,
                    "balanceAfter" to balanceAfter,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )

            batch.commit().await()
            Result.success(receiptCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun executeRedeemBonga(
        profile: AccountProfile,
        reward: BongaReward,
    ): Result<String> {
        val docPath = "users/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId)
            if (profile.bongaPoints < reward.pointsCost) {
                return Result.failure(IllegalStateException("Need ${reward.pointsCost} Bonga Points to redeem."))
            }

            val newPoints = profile.bongaPoints - reward.pointsCost
            val receiptCode = generateReceiptCode()
            val batch = db.batch()
            batch.update(
                userDocRef(uid),
                mapOf(
                    "bongaPoints" to newPoints,
                    "dataBalanceMb" to (profile.dataBalanceMb + reward.dataMb),
                    "airtimeBalance" to (profile.airtimeBalance + reward.airtimeKes),
                    "mpesaBalance" to (profile.mpesaBalance + reward.mpesaCashKes),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )

            if (reward.dataMb > 0) {
                val bundleRef = userDocRef(uid).collection("active_bundles").document()
                batch.set(
                    bundleRef,
                    mapOf(
                        "userId" to uid,
                        "name" to reward.title.take(60),
                        "category" to "DATA",
                        "dataMb" to reward.dataMb,
                        "minutes" to 0L,
                        "smsCount" to 0L,
                        "validityLabel" to "Bonga Reward",
                        "priceKes" to 0.0,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                )
            }

            val txRef = userDocRef(uid).collection("transactions").document()
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "receiptCode" to receiptCode,
                    "type" to "BONGA_REDEEM",
                    "title" to "Redeemed ${reward.title}",
                    "counterparty" to "Bonga Rewards (${reward.pointsCost} pts)",
                    "amount" to reward.pointsCost.toDouble(),
                    "fee" to 0.0,
                    "direction" to "INFO",
                    "source" to "BONGA",
                    "balanceAfter" to newPoints.toDouble(),
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )

            batch.commit().await()
            Result.success(receiptCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addSavedPayee(
        userId: String,
        name: String,
        payeeType: String,
        identifier: String,
        accountRef: String?,
    ): Result<Unit> {
        val docPath = "users/$userId/saved_payees"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanName = name.trim()
            val cleanId = identifier.trim()
            require(cleanName.length in 1..60) { "Name must be 1–60 characters." }
            require(cleanId.length in 1..30) { "Number must be 1–30 characters." }
            require(payeeType in listOf("PHONE", "PAYBILL", "TILL"))

            val ref = userDocRef(uid).collection("saved_payees").document()
            val payload = buildMap<String, Any?> {
                put("userId", uid)
                put("name", cleanName)
                put("payeeType", payeeType)
                put("identifier", cleanId)
                put("accountRef", accountRef?.trim()?.takeIf { it.isNotEmpty() }?.take(40))
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }.filterValues { it != null } as Map<String, Any>

            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSavedPayee(userId: String, payeeId: String): Result<Unit> {
        val docPath = "users/$userId/saved_payees/$payeeId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(payeeId.isNotBlank())
            userDocRef(uid).collection("saved_payees").document(payeeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    companion object {
        fun calculateTransferFee(amount: Double): Double = when {
            amount <= 100.0 -> 0.0
            amount <= 500.0 -> 7.0
            amount <= 1000.0 -> 13.0
            amount <= 5000.0 -> 33.0
            amount <= 20000.0 -> 57.0
            else -> 108.0
        }

        fun calculateWithdrawFee(amount: Double): Double = when {
            amount <= 100.0 -> 11.0
            amount <= 500.0 -> 29.0
            amount <= 2500.0 -> 29.0
            amount <= 5000.0 -> 69.0
            amount <= 20000.0 -> 185.0
            else -> 309.0
        }

        private fun generateReceiptCode(): String {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            return "UK" + (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        }
    }
}
