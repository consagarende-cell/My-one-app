package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class AccountProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val phoneNumber: String = "0712 345 678",
    val planName: String = "SafaOne PrePay 5G",
    val airtimeBalance: Double = 0.0,
    val mpesaBalance: Double = 0.0,
    val savingsBalance: Double = 0.0,
    val fulizaLimit: Double = 5000.0,
    val fulizaUsed: Double = 0.0,
    val dataBalanceMb: Long = 0L,
    val minutesBalance: Long = 0L,
    val smsBalance: Long = 0L,
    val bongaPoints: Long = 0L,
    val fibrePlanName: String = "Gold 60 Mbps",
    val fibreMonthlyFee: Double = 6299.0,
    val fibreActive: Boolean = true,
    val fibreExpiryLabel: String = "Renews in 18 days",
    val hideBalances: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val receiptCode: String = "",
    val type: String = "SEND_MONEY",
    val title: String = "",
    val counterparty: String = "",
    val amount: Double = 0.0,
    val fee: Double = 0.0,
    val direction: String = "DEBIT",
    val source: String = "MPESA",
    val balanceAfter: Double = 0.0,
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ActiveBundle(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "DATA",
    val dataMb: Long = 0L,
    val minutes: Long = 0L,
    val smsCount: Long = 0L,
    val validityLabel: String = "",
    val priceKes: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SavedPayee(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val payeeType: String = "PHONE",
    val identifier: String = "",
    val accountRef: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class TransactionFilter(val label: String) {
    ALL("All"),
    SENT("Sent"),
    PAYMENTS("Paybill & Till"),
    BUNDLES("Bundles & Airtime"),
    SAVINGS("Savings & Top-Up")
}

enum class BundleCategory(val label: String) {
    ALL("All"),
    TUNUKIWA("Tunukiwa"),
    DATA("Data"),
    ALL_IN_ONE("All-in-One"),
    VOICE("Minutes"),
    SMS("SMS")
}

data class BundleOffer(
    val id: String,
    val name: String,
    val subtitle: String,
    val category: String,
    val dataMb: Long,
    val minutes: Long,
    val smsCount: Long,
    val validityLabel: String,
    val priceKes: Double,
    val tag: String? = null,
)

data class BongaReward(
    val id: String,
    val title: String,
    val subtitle: String,
    val pointsCost: Long,
    val dataMb: Long = 0L,
    val airtimeKes: Double = 0.0,
    val mpesaCashKes: Double = 0.0,
)

data class FibrePlan(
    val id: String,
    val name: String,
    val speedMbps: Int,
    val monthlyFee: Double,
    val perks: String,
)

data class UssdShortcut(
    val code: String,
    val title: String,
    val description: String,
    val targetAction: String,
)

object SafaCatalog {
    val bundleOffers: List<BundleOffer> = listOf(
        BundleOffer(
            id = "tunukiwa_2gb_day",
            name = "Tunukiwa 2GB Daily",
            subtitle = "Special personal offer • High-speed 5G",
            category = "TUNUKIWA",
            dataMb = 2048L,
            minutes = 0L,
            smsCount = 0L,
            validityLabel = "Valid 24 hrs",
            priceKes = 50.0,
            tag = "HOT DEAL"
        ),
        BundleOffer(
            id = "tunukiwa_combo_20",
            name = "Tunukiwa 500MB + 25 Mins",
            subtitle = "Instant daily mini combo",
            category = "TUNUKIWA",
            dataMb = 500L,
            minutes = 25L,
            smsCount = 20L,
            validityLabel = "Valid 24 hrs",
            priceKes = 25.0,
            tag = "JUST FOR YOU"
        ),
        BundleOffer(
            id = "data_5gb_week",
            name = "5GB Weekly Data",
            subtitle = "Rollover enabled + Free WhatsApp",
            category = "DATA",
            dataMb = 5120L,
            minutes = 0L,
            smsCount = 0L,
            validityLabel = "Valid 7 days",
            priceKes = 250.0
        ),
        BundleOffer(
            id = "data_15gb_month",
            name = "15GB Monthly Streamer",
            subtitle = "No expiry rollover on active plan",
            category = "DATA",
            dataMb = 15360L,
            minutes = 0L,
            smsCount = 0L,
            validityLabel = "Valid 30 days",
            priceKes = 1000.0,
            tag = "POPULAR"
        ),
        BundleOffer(
            id = "all_in_one_500",
            name = "All-in-One 500",
            subtitle = "3GB Data + 250 Mins + 500 SMS",
            category = "ALL_IN_ONE",
            dataMb = 3072L,
            minutes = 250L,
            smsCount = 500L,
            validityLabel = "Valid 30 days",
            priceKes = 500.0,
            tag = "BEST VALUE"
        ),
        BundleOffer(
            id = "all_in_one_1000",
            name = "Go Monthly 1000",
            subtitle = "8GB Data + 500 Mins + 1000 SMS",
            category = "ALL_IN_ONE",
            dataMb = 8192L,
            minutes = 500L,
            smsCount = 1000L,
            validityLabel = "Valid 30 days",
            priceKes = 1000.0
        ),
        BundleOffer(
            id = "voice_150_week",
            name = "150 Mins All-Net",
            subtitle = "Call any local network",
            category = "VOICE",
            dataMb = 0L,
            minutes = 150L,
            smsCount = 50L,
            validityLabel = "Valid 7 days",
            priceKes = 100.0
        ),
        BundleOffer(
            id = "sms_1000_month",
            name = "1,000 Monthly SMS",
            subtitle = "Unlimited-feel SMS across networks",
            category = "SMS",
            dataMb = 0L,
            minutes = 0L,
            smsCount = 1000L,
            validityLabel = "Valid 30 days",
            priceKes = 50.0
        )
    )

    val bongaRewards: List<BongaReward> = listOf(
        BongaReward(
            id = "bonga_500mb",
            title = "500MB Bonga Data",
            subtitle = "Instant 24-hour high-speed data",
            pointsCost = 150L,
            dataMb = 500L
        ),
        BongaReward(
            id = "bonga_2gb",
            title = "2GB Bonga Data Pack",
            subtitle = "7-day super bundle",
            pointsCost = 450L,
            dataMb = 2048L
        ),
        BongaReward(
            id = "bonga_airtime_100",
            title = "KES 100 Airtime",
            subtitle = "Credited straight to your line",
            pointsCost = 350L,
            airtimeKes = 100.0
        ),
        BongaReward(
            id = "bonga_cash_250",
            title = "KES 250 M-Pesa Cashback",
            subtitle = "Instant Lipa na Bonga wallet credit",
            pointsCost = 800L,
            mpesaCashKes = 250.0
        )
    )

    val fibrePlans: List<FibrePlan> = listOf(
        FibrePlan("bronze", "Bronze 15 Mbps", 15, 2999.0, "Ideal for browsing & HD streaming"),
        FibrePlan("silver", "Silver 30 Mbps", 30, 4100.0, "Smooth remote work & multi-device Wi-Fi"),
        FibrePlan("gold", "Gold 60 Mbps", 60, 6299.0, "4K streaming, gaming & smart home"),
        FibrePlan("diamond", "Diamond 100 Mbps", 100, 12499.0, "Ultra-fast gigabit-class family fibre")
    )

    val ussdShortcuts: List<UssdShortcut> = listOf(
        UssdShortcut("*144#", "Check Line Balances", "View airtime, data, voice & SMS", "HOME"),
        UssdShortcut("*544#", "Tunukiwa & Data Deals", "Browse personalized bundle offers", "DISCOVER"),
        UssdShortcut("*234#", "M-Pesa Self-Service", "Send money, Paybill, Till & Fuliza", "MPESA"),
        UssdShortcut("*126#", "Bonga Points Menu", "Redeem loyalty points for data or cash", "BONGA"),
        UssdShortcut("*400#", "Home Fibre & 5G", "Renew or upgrade broadband plan", "FIBRE")
    )
}
