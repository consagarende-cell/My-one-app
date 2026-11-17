package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AccountProfile
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.SafaDeepPine
import com.example.ui.theme.SafaEmerald
import com.example.ui.theme.SafaForest
import com.example.ui.theme.SafaGold
import kotlin.math.max

@Composable
fun MpesaBalanceCard(
    profile: AccountProfile,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    onToggleVisibility: () -> Unit,
    onSendMoneyClick: () -> Unit,
    onDepositClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fulizaAvailable = max(0.0, profile.fulizaLimit - profile.fulizaUsed)

    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onCardClick)
            .testTag("dashboard_mpesa_balance_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(SafaDeepPine, SafaForest, SafaEmerald)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // Header row: Badge + Refresh + Privacy Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = "M-Pesa Balance",
                                tint = Color(0xFF6EE7B7),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "M-PESA ACCOUNT BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDone,
                                    contentDescription = "Synced with Firestore",
                                    tint = Color(0xFF6EE7B7),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Live • ${profile.phoneNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRefreshClick,
                            enabled = !isRefreshing,
                            modifier = Modifier.testTag("mpesa_card_refresh_button")
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Refresh M-Pesa balance from Firestore",
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(
                            onClick = onToggleVisibility,
                            modifier = Modifier.testTag("mpesa_card_visibility_button")
                        ) {
                            Icon(
                                imageVector = if (profile.hideBalances) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (profile.hideBalances) "Show balance" else "Hide balance",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Balance Readout
                Text(
                    text = formatKes(profile.mpesaBalance, profile.hideBalances),
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMono),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("mpesa_card_balance_value")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Fuliza & Savings Vault chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = "Fuliza Available",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.76f)
                            )
                            Text(
                                text = formatKes(fulizaAvailable, profile.hideBalances),
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono),
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = "Savings Vault",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.76f)
                            )
                            Text(
                                text = formatKes(profile.savingsBalance, profile.hideBalances),
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono),
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Card Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onSendMoneyClick,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = Color.White,
                            contentColor = SafaDeepPine
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mpesa_card_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Money", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = onDepositClick,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = SafaGold,
                            contentColor = Color(0xFF1F1400)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mpesa_card_deposit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Top Up", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
