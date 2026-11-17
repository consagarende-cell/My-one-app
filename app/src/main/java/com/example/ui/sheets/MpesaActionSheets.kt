package com.example.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AccountProfile
import com.example.data.BundleOffer
import com.example.data.SafaRepository
import com.example.data.SavedPayee
import com.example.ui.components.formatDataMb
import com.example.ui.components.formatKes
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.SafaEmerald
import com.example.ui.theme.SafaGold

sealed interface MpesaSheet {
    data class SendMoney(val prefillName: String = "", val prefillPhone: String = "") : MpesaSheet
    data class PayBill(
        val prefillName: String = "",
        val prefillPaybill: String = "",
        val prefillAccount: String = "",
    ) : MpesaSheet
    data class BuyGoods(val prefillName: String = "", val prefillTill: String = "") : MpesaSheet
    data object BuyAirtime : MpesaSheet
    data object WithdrawCash : MpesaSheet
    data object DepositCash : MpesaSheet
    data class SavingsTransfer(val toSavings: Boolean = true) : MpesaSheet
    data class BuyBundle(val offer: BundleOffer) : MpesaSheet
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MpesaActionBottomSheet(
    sheet: MpesaSheet,
    profile: AccountProfile,
    savedPayees: List<SavedPayee>,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSendMoney: (name: String, phone: String, amount: Double, note: String?, allowFuliza: Boolean) -> Unit,
    onPayBill: (bizName: String, paybill: String, account: String, amount: Double, allowFuliza: Boolean) -> Unit,
    onBuyGoods: (merchantName: String, till: String, amount: Double, allowFuliza: Boolean) -> Unit,
    onBuyAirtime: (phone: String, isSelf: Boolean, amount: Double, allowFuliza: Boolean) -> Unit,
    onWithdrawCash: (agent: String, store: String, amount: Double) -> Unit,
    onDepositCash: (amount: Double, agentLabel: String) -> Unit,
    onTransferSavings: (toSavings: Boolean, amount: Double) -> Unit,
    onBuyBundle: (offer: BundleOffer, payWithMpesa: Boolean) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            when (sheet) {
                is MpesaSheet.SendMoney -> SendMoneyContent(
                    sheet = sheet,
                    profile = profile,
                    phonePayees = savedPayees.filter { it.payeeType == "PHONE" },
                    isBusy = isBusy,
                    onSubmit = onSendMoney
                )
                is MpesaSheet.PayBill -> PayBillContent(
                    sheet = sheet,
                    profile = profile,
                    paybillPayees = savedPayees.filter { it.payeeType == "PAYBILL" },
                    isBusy = isBusy,
                    onSubmit = onPayBill
                )
                is MpesaSheet.BuyGoods -> BuyGoodsContent(
                    sheet = sheet,
                    profile = profile,
                    tillPayees = savedPayees.filter { it.payeeType == "TILL" },
                    isBusy = isBusy,
                    onSubmit = onBuyGoods
                )
                is MpesaSheet.BuyAirtime -> BuyAirtimeContent(
                    profile = profile,
                    isBusy = isBusy,
                    onSubmit = onBuyAirtime
                )
                is MpesaSheet.WithdrawCash -> WithdrawContent(
                    profile = profile,
                    isBusy = isBusy,
                    onSubmit = onWithdrawCash
                )
                is MpesaSheet.DepositCash -> DepositContent(
                    isBusy = isBusy,
                    onSubmit = onDepositCash
                )
                is MpesaSheet.SavingsTransfer -> SavingsContent(
                    initialToSavings = sheet.toSavings,
                    profile = profile,
                    isBusy = isBusy,
                    onSubmit = onTransferSavings
                )
                is MpesaSheet.BuyBundle -> BuyBundleContent(
                    offer = sheet.offer,
                    profile = profile,
                    isBusy = isBusy,
                    onSubmit = onBuyBundle
                )
            }
        }
    }
}

@Composable
private fun SendMoneyContent(
    sheet: MpesaSheet.SendMoney,
    profile: AccountProfile,
    phonePayees: List<SavedPayee>,
    isBusy: Boolean,
    onSubmit: (String, String, Double, String?, Boolean) -> Unit,
) {
    var phone by remember { mutableStateOf(sheet.prefillPhone) }
    var name by remember { mutableStateOf(sheet.prefillName) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var allowFuliza by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val fee = if (amount > 0) SafaRepository.calculateTransferFee(amount) else 0.0

    Text("Send Money (M-Pesa)", style = MaterialTheme.typography.headlineSmall)
    Text(
        text = "Available: ${formatKes(profile.mpesaBalance, profile.hideBalances)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (phonePayees.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            phonePayees.forEach { payee ->
                SuggestionChip(
                    onClick = {
                        name = payee.name
                        phone = payee.identifier
                    },
                    label = { Text(payee.name) }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("Recipient Phone Number") },
        placeholder = { Text("07XX XXX XXX") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("send_money_phone_input")
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Recipient Name (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = { Text("Amount (KES)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("send_money_amount_input")
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("Reference Note (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    FulizaOptionRow(profile = profile, allowFuliza = allowFuliza, onToggle = { allowFuliza = it })

    if (amount > 0) {
        FeePreviewBanner(amount = amount, fee = fee)
    }

    Spacer(modifier = Modifier.height(16.dp))
    SubmitActionButton(
        label = if (amount > 0) "Send ${formatKes(amount)}" else "Send Money",
        enabled = !isBusy && phone.trim().length >= 7 && amount >= 1.0,
        isBusy = isBusy,
        testTag = "send_money_submit_button",
        onClick = { onSubmit(name, phone, amount, note.ifBlank { null }, allowFuliza) }
    )
}

@Composable
private fun PayBillContent(
    sheet: MpesaSheet.PayBill,
    profile: AccountProfile,
    paybillPayees: List<SavedPayee>,
    isBusy: Boolean,
    onSubmit: (String, String, String, Double, Boolean) -> Unit,
) {
    var paybill by remember { mutableStateOf(sheet.prefillPaybill) }
    var bizName by remember { mutableStateOf(sheet.prefillName) }
    var account by remember { mutableStateOf(sheet.prefillAccount) }
    var amountText by remember { mutableStateOf("") }
    var allowFuliza by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val fee = if (amount > 0) kotlin.math.round(SafaRepository.calculateTransferFee(amount) * 0.8) else 0.0

    Text("Lipa na M-Pesa • Pay Bill", style = MaterialTheme.typography.headlineSmall)
    Text(
        text = "Available: ${formatKes(profile.mpesaBalance, profile.hideBalances)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val presets = paybillPayees.ifEmpty {
            listOf(
                SavedPayee(name = "Kenya Power", payeeType = "PAYBILL", identifier = "888880", accountRef = "1420988711"),
                SavedPayee(name = "Nairobi Water", payeeType = "PAYBILL", identifier = "444400", accountRef = "8901245"),
                SavedPayee(name = "DSTV", payeeType = "PAYBILL", identifier = "444900", accountRef = "10294481")
            )
        }
        presets.forEach { preset ->
            SuggestionChip(
                onClick = {
                    bizName = preset.name
                    paybill = preset.identifier
                    account = preset.accountRef.orEmpty()
                },
                label = { Text("${preset.name} (${preset.identifier})") }
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = paybill,
        onValueChange = { paybill = it },
        label = { Text("Business Paybill Number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("paybill_number_input")
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = account,
        onValueChange = { account = it },
        label = { Text("Account Number") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("paybill_account_input")
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = bizName,
        onValueChange = { bizName = it },
        label = { Text("Business Name (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = { Text("Amount (KES)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("paybill_amount_input")
    )

    FulizaOptionRow(profile = profile, allowFuliza = allowFuliza, onToggle = { allowFuliza = it })
    if (amount > 0) FeePreviewBanner(amount = amount, fee = fee)

    Spacer(modifier = Modifier.height(16.dp))
    SubmitActionButton(
        label = if (amount > 0) "Pay ${formatKes(amount)}" else "Pay Bill",
        enabled = !isBusy && paybill.trim().isNotEmpty() && account.trim().isNotEmpty() && amount >= 1.0,
        isBusy = isBusy,
        testTag = "paybill_submit_button",
        onClick = { onSubmit(bizName, paybill, account, amount, allowFuliza) }
    )
}

@Composable
private fun BuyGoodsContent(
    sheet: MpesaSheet.BuyGoods,
    profile: AccountProfile,
    tillPayees: List<SavedPayee>,
    isBusy: Boolean,
    onSubmit: (String, String, Double, Boolean) -> Unit,
) {
    var till by remember { mutableStateOf(sheet.prefillTill) }
    var merchant by remember { mutableStateOf(sheet.prefillName) }
    var amountText by remember { mutableStateOf("") }
    var allowFuliza by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0

    Text("Buy Goods & Services (Till)", style = MaterialTheme.typography.headlineSmall)
    Text(
        text = "Zero transaction fee • Available: ${formatKes(profile.mpesaBalance, profile.hideBalances)}",
        style = MaterialTheme.typography.bodySmall,
        color = SafaEmerald
    )

    if (tillPayees.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tillPayees.forEach { preset ->
                SuggestionChip(
                    onClick = {
                        merchant = preset.name
                        till = preset.identifier
                    },
                    label = { Text("${preset.name} (${preset.identifier})") }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = till,
        onValueChange = { till = it },
        label = { Text("Till Number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("buy_goods_till_input")
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = merchant,
        onValueChange = { merchant = it },
        label = { Text("Merchant / Shop Name (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = { Text("Amount (KES)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("buy_goods_amount_input")
    )

    FulizaOptionRow(profile = profile, allowFuliza = allowFuliza, onToggle = { allowFuliza = it })
    if (amount > 0) FeePreviewBanner(amount = amount, fee = 0.0)

    Spacer(modifier = Modifier.height(16.dp))
    SubmitActionButton(
        label = if (amount > 0) "Pay Till ${formatKes(amount)}" else "Pay Till",
        enabled = !isBusy && till.trim().isNotEmpty() && amount >= 1.0,
        isBusy = isBusy,
        testTag = "buy_goods_submit_button",
        onClick = { onSubmit(merchant, till, amount, allowFuliza) }
    )
}

@Composable
private fun BuyAirtimeContent(
    profile: AccountProfile,
    isBusy: Boolean,
    onSubmit: (String, Boolean, Double, Boolean) -> Unit,
) {
    var isSelf by remember { mutableStateOf(true) }
    var otherPhone by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("100") }
    var allowFuliza by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0

    Text("Buy SafaOne Airtime", style = MaterialTheme.typography.headlineSmall)
    Text(
        text = "Current Airtime: ${formatKes(profile.airtimeBalance, profile.hideBalances)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(
            selected = isSelf,
            onClick = { isSelf = true },
            label = { Text("My Number (${profile.phoneNumber})") }
        )
        FilterChip(
            selected = !isSelf,
            onClick = { isSelf = false },
            label = { Text("Another Number") }
        )
    }

    if (!isSelf) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = otherPhone,
            onValueChange = { otherPhone = it },
            label = { Text("Recipient Phone Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("20", "50", "100", "250", "500").forEach { quick ->
            SuggestionChip(
                onClick = { amountText = quick },
                label = { Text("KES $quick") }
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = { Text("Airtime Amount (KES)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("airtime_amount_input")
    )

    FulizaOptionRow(profile = profile, allowFuliza = allowFuliza, onToggle = { allowFuliza = it })

    Spacer(modifier = Modifier.height(16.dp))
    SubmitActionButton(
        label = if (amount > 0) "Buy ${formatKes(amount)} Airtime" else "Buy Airtime",
        enabled = !isBusy && amount >= 5.0 && (isSelf || otherPhone.trim().length >= 7),
        isBusy = isBusy,
        testTag = "buy_airtime_submit_button",
        onClick = { onSubmit(otherPhone, isSelf, amount, allowFuliza) }
    )
}

@Composable
private fun WithdrawContent(
    profile: AccountProfile,
    isBusy: Boolean,
    onSubmit: (String, String, Double) -> Unit,
) {
    var agentNo by remember { mutableStateOf("") }
    var storeNo by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val fee = if (amount >= 50.0) SafaRepository.calculateWithdrawFee(amount) else 0.0

    Text("Withdraw Cash at Agent", style = MaterialTheme.typography.headlineSmall)
    Text(
        text = "M-Pesa Balance: ${formatKes(profile.mpesaBalance, profile.hideBalances)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = agentNo,
        onValueChange = { agentNo = it },
        label = { Text("Agent Number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = storeNo,
        onValueChange = { storeNo = it },
        label = { Text("Store Number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = { Text("Amount (min KES 50)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    if (amount >= 50.0) {
        Spacer(modifier = Modifier.height(10.dp))
        FeePreviewBanner(amount = amount, fee = fee)
    }

    Spacer(modifier = Modifier.height(16.dp))
    SubmitActionButton(
        label = "Withdraw Cash",
        enabled = !isBusy && agentNo.isNotBlank() && storeNo.isNotBlank() && amount >= 50.0,
        isBusy = isBusy,
        testTag = "withdraw_submit_button",
        onClick = { onSubmit(agentNo, storeNo, amount) }
    )
}

@Composable
private fun DepositContent(
    isBusy: Boolean,
    onSubmit: (Double, String) -> Unit,
) {
    var amountText by remember { mutableStateOf("1000") }
    var agentLabel by remember { mutableStateOf("M-Pesa Agent • Westlands") }
    val amount = amountText.toDoubleOrNull() ?: 0.0

    Text("Top Up M-Pesa Wallet", style = MaterialTheme.typography.headlineSmall)
    Text(
        text = "Deposits automatically clear any active Fuliza balance first.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("500", "1000", "2500", "5000").forEach { quick ->
            SuggestionChip(onClick = { amountText = quick }, label = { Text("KES $quick") })
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = { Text("Deposit Amount (KES)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deposit_amount_input")
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = agentLabel,
        onValueChange = { agentLabel = it },
        label = { Text("Source / Agent Description") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    SubmitActionButton(
        label = if (amount > 0) "Deposit ${formatKes(amount)}" else "Deposit",
        enabled = !isBusy && amount >= 10.0,
        isBusy = isBusy,
        testTag = "deposit_submit_button",
        onClick = { onSubmit(amount, agentLabel) }
    )
}

@Composable
private fun SavingsContent(
    initialToSavings: Boolean,
    profile: AccountProfile,
    isBusy: Boolean,
    onSubmit: (Boolean, Double) -> Unit,
) {
    var toSavings by remember { mutableStateOf(initialToSavings) }
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull() ?: 0.0

    Text("Savings Vault (M-Shwari Lock)", style = MaterialTheme.typography.headlineSmall)
    Text(
        text = "Wallet: ${formatKes(profile.mpesaBalance, profile.hideBalances)} • Savings: ${formatKes(profile.savingsBalance, profile.hideBalances)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(
            selected = toSavings,
            onClick = { toSavings = true },
            label = { Text("Move to Savings") }
        )
        FilterChip(
            selected = !toSavings,
            onClick = { toSavings = false },
            label = { Text("Withdraw to Wallet") }
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = { Text("Amount (KES)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    SubmitActionButton(
        label = if (toSavings) "Lock in Savings" else "Move to M-Pesa",
        enabled = !isBusy && amount >= 10.0,
        isBusy = isBusy,
        testTag = "savings_submit_button",
        onClick = { onSubmit(toSavings, amount) }
    )
}

@Composable
private fun BuyBundleContent(
    offer: BundleOffer,
    profile: AccountProfile,
    isBusy: Boolean,
    onSubmit: (BundleOffer, Boolean) -> Unit,
) {
    var payWithMpesa by remember { mutableStateOf(true) }

    Text(offer.name, style = MaterialTheme.typography.headlineSmall)
    Text(
        text = "${offer.subtitle} • ${offer.validityLabel}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Includes", style = MaterialTheme.typography.labelMedium)
                val parts = buildList {
                    if (offer.dataMb > 0) add(formatDataMb(offer.dataMb))
                    if (offer.minutes > 0) add("${offer.minutes} Mins")
                    if (offer.smsCount > 0) add("${offer.smsCount} SMS")
                }.joinToString(" • ")
                Text(parts, style = MaterialTheme.typography.titleSmall, color = SafaEmerald)
            }
            Text(
                text = formatKes(offer.priceKes),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
    Text("Select Payment Method", style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(
            selected = payWithMpesa,
            onClick = { payWithMpesa = true },
            label = { Text("M-Pesa (${formatKes(profile.mpesaBalance, profile.hideBalances)})") }
        )
        FilterChip(
            selected = !payWithMpesa,
            onClick = { payWithMpesa = false },
            label = { Text("Airtime (${formatKes(profile.airtimeBalance, profile.hideBalances)})") }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    SubmitActionButton(
        label = "Activate for ${formatKes(offer.priceKes)}",
        enabled = !isBusy,
        isBusy = isBusy,
        testTag = "confirm_buy_bundle_button",
        onClick = { onSubmit(offer, payWithMpesa) }
    )
}

@Composable
private fun FulizaOptionRow(
    profile: AccountProfile,
    allowFuliza: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val availableFuliza = kotlin.math.max(0.0, profile.fulizaLimit - profile.fulizaUsed)
    Spacer(modifier = Modifier.height(10.dp))
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!allowFuliza) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Cover shortfall with Fuliza", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Available limit: ${formatKes(availableFuliza, profile.hideBalances)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = allowFuliza, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun FeePreviewBanner(amount: Double, fee: Double) {
    Spacer(modifier = Modifier.height(10.dp))
    Surface(
        color = SafaGold.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Est. Fee: ${formatKes(fee)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Total: ${formatKes(amount + fee)}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SubmitActionButton(
    label: String,
    enabled: Boolean,
    isBusy: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = SafaEmerald,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag(testTag)
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun AddSavedPayeeDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, payeeType: String, identifier: String, accountRef: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var payeeType by remember { mutableStateOf("PHONE") }
    var identifier by remember { mutableStateOf("") }
    var accountRef by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Quick Favorite") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("PHONE" to "Phone", "PAYBILL" to "Paybill", "TILL" to "Till").forEach { (key, label) ->
                        FilterChip(
                            selected = payeeType == key,
                            onClick = { payeeType = key },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g., Mama Mboga)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = {
                        Text(
                            when (payeeType) {
                                "PAYBILL" -> "Paybill Number"
                                "TILL" -> "Till Number"
                                else -> "Phone Number"
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (payeeType == "PAYBILL") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = accountRef,
                        onValueChange = { accountRef = it },
                        label = { Text("Account Number (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, payeeType, identifier, accountRef.ifBlank { null }) },
                enabled = !isBusy && name.isNotBlank() && identifier.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SafaEmerald, contentColor = Color.White)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
