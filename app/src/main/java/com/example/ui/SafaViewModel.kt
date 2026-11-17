package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AccountProfile
import com.example.data.ActiveBundle
import com.example.data.BongaReward
import com.example.data.BundleOffer
import com.example.data.FibrePlan
import com.example.data.SafaRepository
import com.example.data.SavedPayee
import com.example.data.UiState
import com.example.data.WalletTransaction
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActionReceipt(
    val receiptCode: String,
    val title: String,
    val message: String,
    val isError: Boolean = false,
)

class SafaViewModel(
    private val repository: SafaRepository,
    private val currentUserId: String,
    private val initialDisplayName: String?,
    private val initialPhoneNumber: String? = null,
) : ViewModel() {

    private val _initError = MutableStateFlow<String?>(null)
    val initError: StateFlow<String?> = _initError.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _isRefreshingBalance = MutableStateFlow(false)
    val isRefreshingBalance: StateFlow<Boolean> = _isRefreshingBalance.asStateFlow()

    private val _receipt = MutableStateFlow<ActionReceipt?>(null)
    val receipt: StateFlow<ActionReceipt?> = _receipt.asStateFlow()

    val profileState: StateFlow<UiState<AccountProfile?>> = repository.observeProfile(currentUserId)
        .map<AccountProfile?, UiState<AccountProfile?>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Error observing profile", root)
            emit(UiState.Error(root.message ?: "Unable to load SafaOne account"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val transactionsState: StateFlow<UiState<List<WalletTransaction>>> = repository.observeTransactions(currentUserId)
        .map<List<WalletTransaction>, UiState<List<WalletTransaction>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Error observing transactions", root)
            emit(UiState.Error(root.message ?: "Unable to load M-Pesa statement"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val activeBundlesState: StateFlow<UiState<List<ActiveBundle>>> = repository.observeActiveBundles(currentUserId)
        .map<List<ActiveBundle>, UiState<List<ActiveBundle>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Error observing bundles", root)
            emit(UiState.Error(root.message ?: "Unable to load active bundles"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val savedPayeesState: StateFlow<UiState<List<SavedPayee>>> = repository.observeSavedPayees(currentUserId)
        .map<List<SavedPayee>, UiState<List<SavedPayee>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Error observing saved payees", root)
            emit(UiState.Error(root.message ?: "Unable to load saved favorites"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    init {
        initializeAccount()
    }

    fun initializeAccount() {
        viewModelScope.launch {
            _initError.value = null
            val res = repository.ensureProfileInitialized(currentUserId, initialDisplayName, initialPhoneNumber)
            res.onFailure { err ->
                _initError.value = err.localizedMessage ?: "Failed to initialize account"
            }
        }
    }

    fun refreshMpesaBalance() {
        if (_isRefreshingBalance.value) return
        viewModelScope.launch {
            _isRefreshingBalance.value = true
            try {
                repository.fetchProfileFromServer(currentUserId).onFailure { err ->
                    _receipt.value = ActionReceipt(
                        receiptCode = "ERR",
                        title = "Refresh Failed",
                        message = err.localizedMessage ?: "Unable to refresh M-Pesa balance",
                        isError = true
                    )
                }
            } finally {
                _isRefreshingBalance.value = false
            }
        }
    }

    fun dismissReceipt() {
        _receipt.value = null
    }

    fun toggleHideBalances(hide: Boolean) {
        viewModelScope.launch {
            repository.toggleHideBalances(currentUserId, hide).onFailure { err ->
                _receipt.value = ActionReceipt(
                    receiptCode = "ERR",
                    title = "Update Failed",
                    message = err.localizedMessage ?: "Unable to update privacy setting",
                    isError = true
                )
            }
        }
    }

    fun updateProfileDetails(displayName: String, phoneNumber: String, onSuccess: () -> Unit) {
        runAction(
            block = { repository.updateProfileDetails(currentUserId, displayName, phoneNumber) },
            successTitle = "Profile Updated",
            successMessage = { "Your SafaOne line ($phoneNumber) details have been saved." },
            onDone = onSuccess
        )
    }

    fun sendMoney(
        profile: AccountProfile,
        recipientName: String,
        phone: String,
        amount: Double,
        note: String?,
        allowFuliza: Boolean,
        onDone: () -> Unit,
    ) {
        val fee = SafaRepository.calculateTransferFee(amount)
        val displayRecipient = recipientName.trim().ifEmpty { phone.trim() }
        runCodeAction(
            block = {
                repository.executeMpesaDebit(
                    profile = profile,
                    type = "SEND_MONEY",
                    title = "Sent to $displayRecipient",
                    counterparty = phone.trim(),
                    amount = amount,
                    fee = fee,
                    note = note,
                    allowFuliza = allowFuliza
                )
            },
            successTitle = "M-Pesa Transfer Confirmed",
            successMessage = { code ->
                "$code Confirmed. KES %.2f sent to $displayRecipient ($phone). Transaction cost KES %.2f.".format(amount, fee)
            },
            onDone = onDone
        )
    }

    fun payBill(
        profile: AccountProfile,
        businessName: String,
        paybillNo: String,
        accountNo: String,
        amount: Double,
        allowFuliza: Boolean,
        onDone: () -> Unit,
    ) {
        val fee = SafaRepository.calculateTransferFee(amount) * 0.8
        val cleanFee = kotlin.math.round(fee)
        val bizLabel = businessName.trim().ifEmpty { "Paybill $paybillNo" }
        runCodeAction(
            block = {
                repository.executeMpesaDebit(
                    profile = profile,
                    type = "PAY_BILL",
                    title = "Paybill • $bizLabel",
                    counterparty = "$paybillNo • Acc $accountNo",
                    amount = amount,
                    fee = cleanFee,
                    note = "Account: $accountNo",
                    allowFuliza = allowFuliza
                )
            },
            successTitle = "Pay Bill Confirmed",
            successMessage = { code ->
                "$code Confirmed. KES %.2f paid to $bizLabel ($paybillNo) for account $accountNo.".format(amount)
            },
            onDone = onDone
        )
    }

    fun buyGoods(
        profile: AccountProfile,
        merchantName: String,
        tillNo: String,
        amount: Double,
        allowFuliza: Boolean,
        onDone: () -> Unit,
    ) {
        val label = merchantName.trim().ifEmpty { "Merchant Till $tillNo" }
        runCodeAction(
            block = {
                repository.executeMpesaDebit(
                    profile = profile,
                    type = "BUY_GOODS",
                    title = "Paid $label",
                    counterparty = "Till $tillNo",
                    amount = amount,
                    fee = 0.0,
                    note = null,
                    allowFuliza = allowFuliza
                )
            },
            successTitle = "Buy Goods Confirmed",
            successMessage = { code ->
                "$code Confirmed. KES %.2f paid to $label (Till $tillNo). Zero transaction fee.".format(amount)
            },
            onDone = onDone
        )
    }

    fun buyAirtime(
        profile: AccountProfile,
        targetPhone: String,
        isSelf: Boolean,
        amount: Double,
        allowFuliza: Boolean,
        onDone: () -> Unit,
    ) {
        val label = if (isSelf) "My Line (${profile.phoneNumber})" else targetPhone.trim()
        runCodeAction(
            block = {
                repository.executeMpesaDebit(
                    profile = profile,
                    type = "BUY_AIRTIME",
                    title = if (isSelf) "Airtime Top-Up (Self)" else "Airtime for $label",
                    counterparty = label,
                    amount = amount,
                    fee = 0.0,
                    note = null,
                    allowFuliza = allowFuliza,
                    extraAirtimeCredit = if (isSelf) amount else 0.0,
                    bongaEarned = kotlin.math.max(5L, (amount / 10.0).toLong())
                )
            },
            successTitle = "Airtime Purchased",
            successMessage = { code ->
                "$code Confirmed. KES %.2f airtime credited to $label.".format(amount)
            },
            onDone = onDone
        )
    }

    fun withdrawCash(
        profile: AccountProfile,
        agentNo: String,
        storeNo: String,
        amount: Double,
        onDone: () -> Unit,
    ) {
        val fee = SafaRepository.calculateWithdrawFee(amount)
        runCodeAction(
            block = {
                repository.executeMpesaDebit(
                    profile = profile,
                    type = "WITHDRAW",
                    title = "Withdraw from Agent $agentNo",
                    counterparty = "Agent $agentNo • Store $storeNo",
                    amount = amount,
                    fee = fee,
                    note = null,
                    allowFuliza = false
                )
            },
            successTitle = "Withdrawal Confirmed",
            successMessage = { code ->
                "$code Confirmed. Withdrew KES %.2f from Agent $agentNo (Store $storeNo). Fee KES %.2f.".format(amount, fee)
            },
            onDone = onDone
        )
    }

    fun depositCash(
        profile: AccountProfile,
        amount: Double,
        agentLabel: String,
        onDone: () -> Unit,
    ) {
        runCodeAction(
            block = { repository.executeDeposit(profile, amount, agentLabel) },
            successTitle = "Wallet Top-Up Confirmed",
            successMessage = { code ->
                "$code Confirmed. You received KES %.2f into your M-Pesa wallet.".format(amount)
            },
            onDone = onDone
        )
    }

    fun transferSavings(
        profile: AccountProfile,
        toSavings: Boolean,
        amount: Double,
        onDone: () -> Unit,
    ) {
        runCodeAction(
            block = { repository.executeTransferSavings(profile, toSavings, amount) },
            successTitle = if (toSavings) "Moved to Savings Vault" else "Savings Withdrawn",
            successMessage = { code ->
                if (toSavings) "$code Confirmed. KES %.2f locked in your Savings Vault.".format(amount)
                else "$code Confirmed. KES %.2f moved back to your M-Pesa wallet.".format(amount)
            },
            onDone = onDone
        )
    }

    fun buyBundle(
        profile: AccountProfile,
        offer: BundleOffer,
        payWithMpesa: Boolean,
        onDone: () -> Unit,
    ) {
        runCodeAction(
            block = { repository.executeBuyBundle(profile, offer, payWithMpesa) },
            successTitle = "Bundle Activated",
            successMessage = { code ->
                "$code Confirmed. ${offer.name} (${offer.validityLabel}) is now active on your SafaOne line."
            },
            onDone = onDone
        )
    }

    fun redeemBonga(profile: AccountProfile, reward: BongaReward) {
        runCodeAction(
            block = { repository.executeRedeemBonga(profile, reward) },
            successTitle = "Bonga Reward Redeemed",
            successMessage = { code ->
                "$code Confirmed. You redeemed ${reward.pointsCost} Bonga Points for ${reward.title}."
            },
            onDone = {}
        )
    }

    fun payFibre(profile: AccountProfile, plan: FibrePlan) {
        runCodeAction(
            block = {
                repository.executeMpesaDebit(
                    profile = profile,
                    type = "FIBRE_PAY",
                    title = "Home Fibre • ${plan.name}",
                    counterparty = "SafaOne Fibre (${plan.speedMbps} Mbps)",
                    amount = plan.monthlyFee,
                    fee = 0.0,
                    note = "30-day broadband renewal",
                    allowFuliza = true,
                    bongaEarned = 50L,
                    fibrePlan = plan
                )
            },
            successTitle = "Home Fibre Activated",
            successMessage = { code ->
                "$code Confirmed. Your ${plan.name} (${plan.speedMbps} Mbps) Home Fibre plan is active for 30 days."
            },
            onDone = {}
        )
    }

    fun addSavedPayee(
        name: String,
        payeeType: String,
        identifier: String,
        accountRef: String?,
        onDone: () -> Unit,
    ) {
        runAction(
            block = { repository.addSavedPayee(currentUserId, name, payeeType, identifier, accountRef) },
            successTitle = "Favorite Saved",
            successMessage = { "$name ($identifier) added to your quick-pay favorites." },
            onDone = onDone
        )
    }

    fun deleteSavedPayee(payeeId: String) {
        viewModelScope.launch {
            repository.deleteSavedPayee(currentUserId, payeeId).onFailure { err ->
                _receipt.value = ActionReceipt(
                    receiptCode = "ERR",
                    title = "Remove Failed",
                    message = err.localizedMessage ?: "Unable to remove favorite",
                    isError = true
                )
            }
        }
    }

    private fun runCodeAction(
        block: suspend () -> Result<String>,
        successTitle: String,
        successMessage: (String) -> String,
        onDone: () -> Unit,
    ) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = block()
                res.onSuccess { code ->
                    onDone()
                    _receipt.value = ActionReceipt(
                        receiptCode = code,
                        title = successTitle,
                        message = successMessage(code),
                        isError = false
                    )
                }.onFailure { err ->
                    _receipt.value = ActionReceipt(
                        receiptCode = "FAILED",
                        title = "Transaction Declined",
                        message = err.localizedMessage ?: "Operation could not be completed",
                        isError = true
                    )
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    private fun runAction(
        block: suspend () -> Result<Unit>,
        successTitle: String,
        successMessage: () -> String,
        onDone: () -> Unit,
    ) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = block()
                res.onSuccess {
                    onDone()
                    _receipt.value = ActionReceipt(
                        receiptCode = "DONE",
                        title = successTitle,
                        message = successMessage(),
                        isError = false
                    )
                }.onFailure { err ->
                    _receipt.value = ActionReceipt(
                        receiptCode = "ERR",
                        title = "Action Failed",
                        message = err.localizedMessage ?: "Please check your inputs and try again",
                        isError = true
                    )
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    private companion object {
        const val TAG = "SafaViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
