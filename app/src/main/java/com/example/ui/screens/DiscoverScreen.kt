package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AccountProfile
import com.example.data.BongaReward
import com.example.data.BundleCategory
import com.example.data.BundleOffer
import com.example.data.FibrePlan
import com.example.data.SafaCatalog
import com.example.ui.components.formatDataMb
import com.example.ui.components.formatKes
import com.example.ui.sheets.MpesaSheet
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.SafaDeepPine
import com.example.ui.theme.SafaEmerald
import com.example.ui.theme.SafaGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    profile: AccountProfile,
    selectedSubTab: Int,
    onSubTabSelected: (Int) -> Unit,
    isBusy: Boolean,
    onOpenBundleSheet: (MpesaSheet.BuyBundle) -> Unit,
    onRedeemBonga: (BongaReward) -> Unit,
    onPayFibre: (FibrePlan) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf(BundleCategory.ALL) }

    val filteredBundles = remember(selectedCategory) {
        if (selectedCategory == BundleCategory.ALL) SafaCatalog.bundleOffers
        else SafaCatalog.bundleOffers.filter { it.category == selectedCategory.name }
    }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { onSubTabSelected(0) },
                text = { Text("Bundles") },
                modifier = Modifier.testTag("discover_bundles_tab")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { onSubTabSelected(1) },
                text = { Text("Bonga") },
                modifier = Modifier.testTag("discover_bonga_tab")
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { onSubTabSelected(2) },
                text = { Text("Home Fibre") },
                modifier = Modifier.testTag("discover_fibre_tab")
            )
        }

        when (selectedSubTab) {
            0 -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BundleCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                }
                items(filteredBundles, key = { it.id }) { offer ->
                    BundleStoreCard(
                        offer = offer,
                        onBuyClick = { onOpenBundleSheet(MpesaSheet.BuyBundle(offer)) }
                    )
                }
            }

            1 -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = SafaDeepPine),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val tier = when {
                                    profile.bongaPoints >= 2000L -> "GOLD TIER"
                                    profile.bongaPoints >= 800L -> "SILVER TIER"
                                    else -> "BRONZE TIER"
                                }
                                Surface(color = SafaGold, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = tier,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F1400),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "${profile.bongaPoints} pts",
                                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMono),
                                    color = Color.White
                                )
                                Text(
                                    text = "Earn Bonga Points automatically with every M-Pesa payment and bundle purchase.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Image(
                                painter = painterResource(id = R.drawable.img_bonga_rewards),
                                contentDescription = "Bonga Points Rewards",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }
                    }
                }

                items(SafaCatalog.bongaRewards, key = { it.id }) { reward ->
                    val canAfford = profile.bongaPoints >= reward.pointsCost
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SafaGold.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Stars,
                                    contentDescription = reward.title,
                                    tint = Color(0xFFD97706)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(reward.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = reward.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${reward.pointsCost} Bonga Points",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SafaEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onRedeemBonga(reward) },
                                enabled = !isBusy && canAfford,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SafaEmerald,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("redeem_${reward.id}_button")
                            ) {
                                Text("Redeem")
                            }
                        }
                    }
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(145.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_fibre_banner),
                                    contentDescription = "SafaOne Home Fibre",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                            )
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Router,
                                            contentDescription = null,
                                            tint = Color(0xFF6EE7B7)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "SafaOne Home Fibre • ${profile.fibrePlanName}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (profile.fibreActive) "Connected • ${profile.fibreExpiryLabel}" else "Subscription Inactive",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = SafaEmerald
                                    )
                                    Text(
                                        text = "Monthly Fee: ${formatKes(profile.fibreMonthlyFee)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Fibre connected",
                                    tint = SafaEmerald
                                )
                            }
                        }
                    }
                }

                items(SafaCatalog.fibrePlans, key = { it.id }) { plan ->
                    val isCurrent = profile.fibrePlanName == plan.name
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(plan.name, style = MaterialTheme.typography.titleMedium)
                                    if (isCurrent) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = SafaEmerald.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "CURRENT",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SafaEmerald,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = plan.perks,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${formatKes(plan.monthlyFee)} / month",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                                    color = SafaEmerald
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            if (isCurrent) {
                                OutlinedButton(
                                    onClick = { onPayFibre(plan) },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Renew")
                                }
                            } else {
                                Button(
                                    onClick = { onPayFibre(plan) },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SafaEmerald,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Switch")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BundleStoreCard(
    offer: BundleOffer,
    onBuyClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(offer.name, style = MaterialTheme.typography.titleMedium)
                    if (offer.tag != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = SafaGold.copy(alpha = 0.22f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = offer.tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = offer.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                val specs = buildList {
                    if (offer.dataMb > 0) add(formatDataMb(offer.dataMb))
                    if (offer.minutes > 0) add("${offer.minutes} Mins")
                    if (offer.smsCount > 0) add("${offer.smsCount} SMS")
                    add(offer.validityLabel)
                }.joinToString(" • ")
                Text(
                    text = specs,
                    style = MaterialTheme.typography.labelMedium,
                    color = SafaEmerald
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onBuyClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafaEmerald,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("buy_${offer.id}_button")
            ) {
                Text(
                    text = "KES ${offer.priceKes.toInt()}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono)
                )
            }
        }
    }
}
