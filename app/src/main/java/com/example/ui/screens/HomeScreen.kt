package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Atm
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AccountProfile
import com.example.data.ActiveBundle
import com.example.data.BundleOffer
import com.example.data.SafaCatalog
import com.example.data.WalletTransaction
import com.example.ui.components.MpesaBalanceCard
import com.example.ui.components.QuickActionTile
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.TransactionRow
import com.example.ui.components.formatDataMb
import com.example.ui.components.formatKes
import com.example.ui.sheets.MpesaSheet
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.SafaDeepPine
import com.example.ui.theme.SafaEmerald
import com.example.ui.theme.SafaEmeraldDark
import com.example.ui.theme.SafaForest
import com.example.ui.theme.SafaGold
import com.example.ui.theme.SafaGoldContainer
import com.example.ui.theme.SafaMintLight
import com.example.ui.theme.SafaTeal
import com.example.ui.theme.SafaTealContainer
import kotlin.math.max

@Composable
fun HomeScreen(
    profile: AccountProfile,
    transactions: List<WalletTransaction>,
    activeBundles: List<ActiveBundle>,
    isRefreshingBalance: Boolean,
    onRefreshBalance: () -> Unit,
    onToggleHideBalances: () -> Unit,
    onOpenSheet: (MpesaSheet) -> Unit,
    onNavigateToMpesa: () -> Unit,
    onNavigateToDiscover: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tunukiwaOffers = SafaCatalog.bundleOffers.filter { it.category == "TUNUKIWA" || it.tag != null }
    val fulizaAvailable = max(0.0, profile.fulizaLimit - profile.fulizaUsed)
    val bongaTier = when {
        profile.bongaPoints >= 2000L -> "GOLD"
        profile.bongaPoints >= 800L -> "SILVER"
        else -> "BRONZE"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Greeting & Active Mobile Line Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Jambo, ${profile.displayName.substringBefore(" ")}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = SafaEmerald.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${profile.phoneNumber} • 5G",
                                style = MaterialTheme.typography.labelSmall,
                                color = SafaEmerald,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = profile.planName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = onToggleHideBalances,
                    modifier = Modifier.testTag("home_toggle_balance_visibility")
                ) {
                    Icon(
                        imageVector = if (profile.hideBalances) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (profile.hideBalances) "Show balances" else "Hide balances",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Dedicated Firestore M-Pesa Account Balance Card
        item {
            MpesaBalanceCard(
                profile = profile,
                isRefreshing = isRefreshingBalance,
                onRefreshClick = onRefreshBalance,
                onToggleVisibility = onToggleHideBalances,
                onSendMoneyClick = { onOpenSheet(MpesaSheet.SendMoney()) },
                onDepositClick = { onOpenSheet(MpesaSheet.DepositCash) },
                onCardClick = onNavigateToMpesa
            )
        }

        // 2. Quick-Access Dashboard Bento Cards: Airtime, Data Bundles, M-Pesa Services, Bonga Rewards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Airtime & Data Bundles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick-Access Card 1: Airtime
                    DashboardQuickAccessCard(
                        title = "Airtime",
                        primaryValue = formatKes(profile.airtimeBalance, profile.hideBalances),
                        subtitle = "PrePay Line • Ready",
                        ctaLabel = "Top Up",
                        icon = Icons.Rounded.PhoneAndroid,
                        gradientColors = listOf(SafaDeepPine, SafaForest),
                        accentColor = Color(0xFF6EE7B7),
                        badgeBackground = Color.White.copy(alpha = 0.16f),
                        testTag = "dashboard_card_airtime",
                        onClick = { onOpenSheet(MpesaSheet.BuyAirtime) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    // Quick-Access Card 2: Data Bundles
                    val dataProgress = (profile.dataBalanceMb.toFloat() / 10240f).coerceIn(0.06f, 1f)
                    DashboardQuickAccessCard(
                        title = "Data Bundles",
                        primaryValue = formatDataMb(profile.dataBalanceMb, profile.hideBalances),
                        subtitle = if (profile.hideBalances) "••• Mins • ••• SMS" else "${profile.minutesBalance}m • ${profile.smsBalance} SMS",
                        ctaLabel = "Buy Bundles",
                        icon = Icons.Rounded.DataUsage,
                        gradientColors = listOf(Color(0xFF0A4F48), SafaTeal),
                        accentColor = SafaTealContainer,
                        badgeBackground = Color.White.copy(alpha = 0.18f),
                        progress = dataProgress,
                        testTag = "dashboard_card_data_bundles",
                        onClick = { onNavigateToDiscover(0) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // Row 2: M-Pesa Services & Bonga Rewards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick-Access Card 3: M-Pesa Services
                    DashboardQuickAccessCard(
                        title = "M-Pesa Services",
                        primaryValue = formatKes(profile.mpesaBalance, profile.hideBalances),
                        subtitle = "Fuliza ${formatKes(fulizaAvailable, profile.hideBalances)}",
                        ctaLabel = "Transact",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        gradientColors = listOf(SafaEmeraldDark, SafaEmerald),
                        accentColor = SafaMintLight,
                        badgeBackground = Color.White.copy(alpha = 0.20f),
                        testTag = "dashboard_card_mpesa",
                        onClick = onNavigateToMpesa,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    // Quick-Access Card 4: Bonga Rewards
                    DashboardQuickAccessCard(
                        title = "Bonga Rewards",
                        primaryValue = if (profile.hideBalances) "•••• pts" else "${profile.bongaPoints} pts",
                        subtitle = "$bongaTier Tier • Earn & Redeem",
                        ctaLabel = "Redeem",
                        icon = Icons.Rounded.Stars,
                        gradientColors = listOf(Color(0xFF78350F), Color(0xFFD97706)),
                        accentColor = SafaGoldContainer,
                        badgeBackground = Color.White.copy(alpha = 0.20f),
                        testTag = "dashboard_card_bonga",
                        onClick = { onNavigateToDiscover(1) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }

        // 3. M-Pesa & Line Quick Actions Grid
        item {
            SectionHeaderRow(title = "M-Pesa & Line Services")
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
                            testTag = "home_send_money_tile",
                            onClick = { onOpenSheet(MpesaSheet.SendMoney()) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Rounded.ReceiptLong,
                            label = "Pay Bill",
                            tint = SafaTeal,
                            containerColor = SafaTealContainer,
                            testTag = "home_pay_bill_tile",
                            onClick = { onOpenSheet(MpesaSheet.PayBill()) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Rounded.ShoppingBag,
                            label = "Buy Goods",
                            tint = Color(0xFFB45309),
                            containerColor = SafaGoldContainer,
                            testTag = "home_buy_goods_tile",
                            onClick = { onOpenSheet(MpesaSheet.BuyGoods()) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Rounded.Atm,
                            label = "Withdraw",
                            tint = SafaEmerald,
                            containerColor = SafaMintLight,
                            testTag = "home_withdraw_tile",
                            onClick = { onOpenSheet(MpesaSheet.WithdrawCash) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        QuickActionTile(
                            icon = Icons.Rounded.Wifi,
                            label = "Bundles",
                            tint = SafaTeal,
                            containerColor = SafaTealContainer,
                            testTag = "home_bundles_tile",
                            onClick = { onNavigateToDiscover(0) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Rounded.PhoneAndroid,
                            label = "Buy Airtime",
                            tint = SafaEmerald,
                            containerColor = SafaMintLight,
                            testTag = "home_airtime_tile",
                            onClick = { onOpenSheet(MpesaSheet.BuyAirtime) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Rounded.Savings,
                            label = "Savings",
                            tint = Color(0xFFB45309),
                            containerColor = SafaGoldContainer,
                            testTag = "home_savings_tile",
                            onClick = { onOpenSheet(MpesaSheet.SavingsTransfer(true)) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Rounded.Router,
                            label = "Home Fibre",
                            tint = SafaTeal,
                            containerColor = SafaTealContainer,
                            testTag = "home_fibre_tile",
                            onClick = { onNavigateToDiscover(2) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. Tunukiwa "Just For You" Deals Carousel
        item {
            SectionHeaderRow(
                title = "Tunukiwa • Just For You",
                actionLabel = "All Bundles",
                onActionClick = { onNavigateToDiscover(0) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tunukiwaOffers, key = { it.id }) { offer ->
                    TunukiwaOfferCard(
                        offer = offer,
                        onBuyClick = { onOpenSheet(MpesaSheet.BuyBundle(offer)) }
                    )
                }
            }
        }

        // 5. Active Bundles Summary
        if (activeBundles.isNotEmpty()) {
            item {
                SectionHeaderRow(
                    title = "Active Plans (${activeBundles.size})",
                    actionLabel = "Add More",
                    onActionClick = { onNavigateToDiscover(0) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(activeBundles.take(5), key = { it.id }) { bundle ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.width(210.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Surface(
                                    color = SafaEmerald.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = bundle.validityLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SafaEmerald,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = bundle.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val summary = buildList {
                                    if (bundle.dataMb > 0) add(formatDataMb(bundle.dataMb))
                                    if (bundle.minutes > 0) add("${bundle.minutes} mins")
                                    if (bundle.smsCount > 0) add("${bundle.smsCount} SMS")
                                }.joinToString(" • ")
                                Text(
                                    text = summary.ifEmpty { "Active Pack" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Recent M-Pesa & Line Activity
        item {
            SectionHeaderRow(
                title = "Recent Activity",
                actionLabel = "Full Statement",
                onActionClick = onNavigateToMpesa
            )
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No transactions yet. Try topping up airtime, sending money, or buying a Tunukiwa bundle!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(transactions.take(4), key = { it.id }) { tx ->
                TransactionRow(tx = tx, hideBalances = profile.hideBalances)
            }
        }
    }
}

@Composable
private fun DashboardQuickAccessCard(
    title: String,
    primaryValue: String,
    subtitle: String,
    ctaLabel: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    accentColor: Color,
    badgeBackground: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(colors = gradientColors))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(16.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Open $title",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = primaryValue,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.76f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = accentColor,
                    trackColor = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Surface(
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = ctaLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun TunukiwaOfferCard(
    offer: BundleOffer,
    onBuyClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.width(248.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_promo),
                    contentDescription = offer.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(12.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Surface(
                        color = SafaGold,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = offer.tag ?: "TUNUKIWA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F1400),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = offer.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${offer.validityLabel} • ${offer.subtitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatKes(offer.priceKes),
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                        color = SafaEmerald,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = onBuyClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SafaEmerald, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Buy", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
