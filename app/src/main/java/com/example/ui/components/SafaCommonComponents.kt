package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.WalletTransaction
import com.example.ui.ActionReceipt
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.SafaEmerald
import com.example.ui.theme.SafaGold
import com.example.ui.theme.SafaRed
import com.example.ui.theme.SafaTeal
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

fun formatKes(amount: Double, hidden: Boolean = false): String {
    if (hidden) return "KES •••••"
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "KES ${nf.format(amount)}"
}

fun formatDataMb(mb: Long, hidden: Boolean = false): String {
    if (hidden) return "••• GB"
    return if (mb >= 1024L) {
        val gb = mb / 1024.0
        String.format(Locale.US, "%.2f GB", gb)
    } else {
        "$mb MB"
    }
}

fun formatTimestamp(ts: Timestamp?): String {
    val date = (ts ?: Timestamp.now()).toDate()
    val fmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    return fmt.format(date)
}

@Composable
fun QuickActionTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    containerColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TransactionRow(
    tx: WalletTransaction,
    hideBalances: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val (icon, tint, badgeBg) = when (tx.type) {
        "DEPOSIT", "SAVINGS_OUT" -> Triple(
            Icons.AutoMirrored.Rounded.CallReceived,
            SafaEmerald,
            SafaEmerald.copy(alpha = 0.14f)
        )
        "PAY_BILL" -> Triple(
            Icons.Rounded.ReceiptLong,
            SafaTeal,
            SafaTeal.copy(alpha = 0.14f)
        )
        "BUY_GOODS" -> Triple(
            Icons.Rounded.ShoppingBag,
            SafaGold,
            SafaGold.copy(alpha = 0.16f)
        )
        "BUY_BUNDLE" -> Triple(
            Icons.Rounded.Wifi,
            SafaEmerald,
            SafaEmerald.copy(alpha = 0.14f)
        )
        "BUY_AIRTIME" -> Triple(
            Icons.Rounded.PhoneAndroid,
            SafaTeal,
            SafaTeal.copy(alpha = 0.14f)
        )
        "BONGA_REDEEM" -> Triple(
            Icons.Rounded.Stars,
            SafaGold,
            SafaGold.copy(alpha = 0.16f)
        )
        "SAVINGS_IN" -> Triple(
            Icons.Rounded.Savings,
            SafaEmerald,
            SafaEmerald.copy(alpha = 0.14f)
        )
        "FIBRE_PAY" -> Triple(
            Icons.Rounded.Router,
            SafaTeal,
            SafaTeal.copy(alpha = 0.14f)
        )
        else -> Triple(
            Icons.AutoMirrored.Rounded.CallMade,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    }

    val amountPrefix = when (tx.direction) {
        "CREDIT" -> "+"
        "DEBIT" -> "-"
        else -> ""
    }
    val amountText = when {
        hideBalances -> "••••"
        tx.source == "BONGA" -> "${tx.amount.toLong()} pts"
        else -> "$amountPrefix${formatKes(tx.amount)}"
    }
    val amountColor = when (tx.direction) {
        "CREDIT" -> SafaEmerald
        "DEBIT" -> MaterialTheme.colorScheme.onSurface
        else -> SafaGold
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tx.title,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${tx.counterparty} • ${formatTimestamp(tx.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = amountText,
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                        color = amountColor
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse receipt" else "Expand receipt",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Receipt Code",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = tx.receiptCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = SafaEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Source & Fee",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${tx.source} • Fee ${formatKes(tx.fee, hideBalances)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Balance After",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (tx.source == "BONGA") "${tx.balanceAfter.toLong()} pts" else formatKes(tx.balanceAfter, hideBalances),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (!tx.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Note: ${tx.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptConfirmationDialog(
    receipt: ActionReceipt,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (receipt.isError) SafaRed.copy(alpha = 0.14f)
                        else SafaEmerald.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (receipt.isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                    contentDescription = receipt.title,
                    tint = if (receipt.isError) SafaRed else SafaEmerald,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = receipt.title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!receipt.isError && receipt.receiptCode != "DONE") {
                    Surface(
                        color = SafaEmerald.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "REF: ${receipt.receiptCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SafaEmerald,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text(
                    text = receipt.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (receipt.isError) SafaRed else SafaEmerald,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receipt_done_button")
            ) {
                Text(if (receipt.isError) "Got It" else "Done")
            }
        }
    )
}

@Composable
fun SectionHeaderRow(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null && onActionClick != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = SafaEmerald,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = actionLabel,
                    tint = SafaEmerald,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
