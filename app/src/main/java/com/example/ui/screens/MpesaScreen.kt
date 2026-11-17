package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Atm
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AccountProfile
import com.example.data.SavedPayee
import com.example.data.TransactionFilter
import com.example.data.WalletTransaction
import com.example.ui.components.QuickActionTile
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.TransactionRow
import com.example.ui.components.formatKes
import com.example.ui.sheets.MpesaSheet
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.SafaDeepPine
import com.example.ui.theme.SafaEmerald
import com.example.ui.theme.SafaForest
import com.example.ui.theme.SafaGold
import com.example.ui.theme.SafaMintLight
import com.example.ui.theme.SafaTeal
import kotlin.math.max

@Composable
fun MpesaScreen(
    profile: AccountProfile,
    transactions: List<WalletTransaction>,
    savedPayees: List<SavedPayee>,
    onToggleHideBalances: () -> Unit,
    onOpenSheet: (MpesaSheet) -> Unit,
    onOpenAddPayee: () -> Unit,
    onDeletePayee: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFilter by remember { mutableStateOf(TransactionFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTx = remember(transactions, selectedFilter, searchQuery) {
        transactions.filter { tx ->
            val matchesFilter = when (selectedFilter) {
                TransactionFilter.ALL -> true
                TransactionFilter.SENT -> tx.type == "SEND_MONEY" || tx.type == "WITHDRAW"
                TransactionFilter.PAYMENTS -> tx.type == "PAY_BILL" || tx.type == "BUY_GOODS" || tx.type == "FIBRE_PAY"
                TransactionFilter.BUNDLES -> tx.type == "BUY_BUNDLE" || tx.type == "BUY_AIRTIME" || tx.type == "BONGA_REDEEM"
                TransactionFilter.SAVINGS -> tx.type == "DEPOSIT" || tx.type == "SAVINGS_IN" || tx.type == "SAVINGS_OUT"
            }
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                tx.title.lowercase().contains(q) ||
                tx.counterparty.lowercase().contains(q) ||
                tx.receiptCode.lowercase().contains(q)
            matchesFilter && matchesQuery
        }
    }

    val fulizaAvailable = max(0.0, profile.fulizaLimit - profile.fulizaUsed)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. M-Pesa Wallet Hero Card + Fuliza + Savings Vault
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SafaDeepPine, SafaForest, SafaEmerald)
                        )
                    )
                    .padding(18.dp)
                    .testTag("mpesa_balance_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "M-PESA WALLET BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatKes(profile.mpesaBalance, profile.hideBalances),
                                style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMono),
                                color = Color.White
                            )
                        }
                        IconButton(onClick = onToggleHideBalances) {
                            Icon(
                                imageVector = if (profile.hideBalances) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Toggle wallet balance privacy",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Savings Lock Card
                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenSheet(MpesaSheet.SavingsTransfer(true)) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Savings Vault",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatKes(profile.savingsBalance, profile.hideBalances),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                                    color = Color.White
                                )
                            }
                        }

                        // Deposit Quick Action
                        Surface(
                            color = SafaGold,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(0.75f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenSheet(MpesaSheet.DepositCash) }
                                .testTag("mpesa_deposit_button")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "+ Top Up",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF1F1400),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Deposit Cash",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1F1400).copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Fuliza Credit Limit Meter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Fuliza Available: ${formatKes(fulizaAvailable, profile.hideBalances)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "Limit ${formatKes(profile.fulizaLimit, profile.hideBalances)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val fulizaRatio = if (profile.fulizaLimit > 0) {
                        (fulizaAvailable / profile.fulizaLimit).toFloat().coerceIn(0f, 1f)
                    } else 0f
                    LinearProgressIndicator(
                        progress = { fulizaRatio },
                        color = SafaGold,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }

        // 2. M-Pesa Core Services Grid
        item {
            SectionHeaderRow(title = "Transact")
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        QuickActionTile(
                            icon = Icons.AutoMirrored.Rounded.Send,
                            label = "Send Money",
                            tint = SafaEmerald,
                            containerColor = SafaMintLight,
                            testTag = "mpesa_send_money_tile",
                            onClick = { onOpenSheet(MpesaSheet.SendMoney()) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Rounded.ReceiptLong,
                            label = "Pay Bill",
                            tint = SafaTeal,
                            containerColor = Color(0xFFCCFBF1),
                            testTag = "mpesa_pay_bill_tile",
                            onClick = { onOpenSheet(MpesaSheet.PayBill()) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Rounded.ShoppingBag,
                            label = "Buy Goods",
                            tint = Color(0xFFB45309),
                            containerColor = Color(0xFFFEF3C7),
                            testTag = "mpesa_buy_goods_tile",
                            onClick = { onOpenSheet(MpesaSheet.BuyGoods()) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        QuickActionTile(
                            icon = Icons.Rounded.Atm,
                            label = "Withdraw",
                            tint = SafaEmerald,
                            containerColor = SafaMintLight,
                            testTag = "mpesa_withdraw_tile",
                            onClick = { onOpenSheet(MpesaSheet.WithdrawCash) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Rounded.PhoneAndroid,
                            label = "Buy Airtime",
                            tint = SafaTeal,
                            containerColor = Color(0xFFCCFBF1),
                            testTag = "mpesa_airtime_tile",
                            onClick = { onOpenSheet(MpesaSheet.BuyAirtime) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Rounded.Savings,
                            label = "Savings",
                            tint = Color(0xFFB45309),
                            containerColor = Color(0xFFFEF3C7),
                            testTag = "mpesa_savings_tile",
                            onClick = { onOpenSheet(MpesaSheet.SavingsTransfer(true)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Saved Favorites (Contacts, Paybills, Tills)
        item {
            SectionHeaderRow(
                title = "Saved Favorites",
                actionLabel = "+ Add",
                onActionClick = onOpenAddPayee
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (savedPayees.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAddPayee)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add favorite",
                            tint = SafaEmerald
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Save frequent contacts, Paybills, or Tills for 1-tap payments.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(savedPayees, key = { it.id }) { payee ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .width(176.dp)
                                .clickable {
                                    when (payee.payeeType) {
                                        "PAYBILL" -> onOpenSheet(
                                            MpesaSheet.PayBill(
                                                prefillName = payee.name,
                                                prefillPaybill = payee.identifier,
                                                prefillAccount = payee.accountRef.orEmpty()
                                            )
                                        )
                                        "TILL" -> onOpenSheet(
                                            MpesaSheet.BuyGoods(
                                                prefillName = payee.name,
                                                prefillTill = payee.identifier
                                            )
                                        )
                                        else -> onOpenSheet(
                                            MpesaSheet.SendMoney(
                                                prefillName = payee.name,
                                                prefillPhone = payee.identifier
                                            )
                                        )
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = SafaEmerald.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = payee.payeeType,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SafaEmerald,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeletePayee(payee.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove ${payee.name}",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = payee.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = payee.identifier,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. M-Pesa Statement Header + Search + Filters
        item {
            SectionHeaderRow(title = "M-Pesa Statement")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search name, receipt code, or till...") },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = "Search statement")
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mpesa_statement_search")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) }
                    )
                }
            }
        }

        if (filteredTx.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No matching M-Pesa statement entries found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(filteredTx, key = { it.id }) { tx ->
                TransactionRow(tx = tx, hideBalances = profile.hideBalances)
            }
        }
    }
}
