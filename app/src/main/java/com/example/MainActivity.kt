package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.AccountProfile
import com.example.data.SafaRepository
import com.example.data.UiState
import com.example.ui.SafaViewModel
import com.example.ui.components.ReceiptConfirmationDialog
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MpesaScreen
import com.example.ui.sheets.AddSavedPayeeDialog
import com.example.ui.sheets.MpesaActionBottomSheet
import com.example.ui.sheets.MpesaSheet
import com.example.ui.theme.SafaDeepPine
import com.example.ui.theme.SafaEmerald
import com.example.ui.theme.SafaForest
import com.example.ui.theme.SafaOneTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafaOneTheme {
                AppNavigation()
            }
        }
    }
}

private enum class SafaTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
) {
    HOME("Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    MPESA("M-Pesa", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_mpesa"),
    DISCOVER("Discover", Icons.Rounded.Explore, Icons.Outlined.Explore, "nav_discover"),
    ACCOUNT("Account", Icons.Rounded.Person, Icons.Outlined.Person, "nav_account"),
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var preferredPhoneNumber by rememberSaveable { mutableStateOf("") }
    val user = currentUser

    if (user == null) {
        AuthScreen(
            preferredPhoneNumber = preferredPhoneNumber,
            onPhoneNumberChanged = { preferredPhoneNumber = it },
            onAuthSuccess = {}
        )
    } else {
        val uid = user.uid
        key(uid) {
            SafaMainShell(
                currentUserId = uid,
                userDisplayName = user.displayName,
                userEmail = user.email,
                userPhotoUrl = user.photoUrl?.toString(),
                preferredPhoneNumber = preferredPhoneNumber.ifBlank { null }
            )
        }
    }
}

@Composable
private fun SafaMainShell(
    currentUserId: String,
    userDisplayName: String?,
    userEmail: String?,
    userPhotoUrl: String?,
    preferredPhoneNumber: String?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val pinPrefs = remember(currentUserId) {
        context.getSharedPreferences("safaone_pin_$currentUserId", Context.MODE_PRIVATE)
    }
    var savedPin by remember(currentUserId) { mutableStateOf(pinPrefs.getString("app_pin", null)) }
    var isPinLocked by rememberSaveable(currentUserId) { mutableStateOf(savedPin != null) }

    val viewModel: SafaViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SafaViewModel(
                    repository = SafaRepository(db),
                    currentUserId = currentUserId,
                    initialDisplayName = userDisplayName,
                    initialPhoneNumber = preferredPhoneNumber
                )
            }
        }
    )

    if (isPinLocked && savedPin != null) {
        PinUnlockScreen(
            displayName = userDisplayName ?: "SafaOne Member",
            expectedPin = savedPin.orEmpty(),
            onUnlocked = { isPinLocked = false },
            onSignOut = { signOut(credentialManager, scope) }
        )
        return
    }

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val bundlesState by viewModel.activeBundlesState.collectAsStateWithLifecycle()
    val payeesState by viewModel.savedPayeesState.collectAsStateWithLifecycle()
    val initError by viewModel.initError.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val isRefreshingBalance by viewModel.isRefreshingBalance.collectAsStateWithLifecycle()
    val receipt by viewModel.receipt.collectAsStateWithLifecycle()

    var currentTabOrdinal by rememberSaveable { mutableIntStateOf(0) }
    var discoverSubTab by rememberSaveable { mutableIntStateOf(0) }
    var activeSheet by remember { mutableStateOf<MpesaSheet?>(null) }
    var showAddPayeeDialog by remember { mutableStateOf(false) }

    val currentTab = SafaTab.entries.getOrElse(currentTabOrdinal) { SafaTab.HOME }

    if (currentTab != SafaTab.HOME) {
        BackHandler { currentTabOrdinal = SafaTab.HOME.ordinal }
    }

    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()
    val activeBundles = (bundlesState as? UiState.Success)?.data.orEmpty()
    val savedPayees = (payeesState as? UiState.Success)?.data.orEmpty()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        SafaTab.entries.forEachIndexed { index, tab ->
                            val selected = currentTabOrdinal == index
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTabOrdinal = index },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SafaTab.entries.forEachIndexed { index, tab ->
                            val selected = currentTabOrdinal == index
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTabOrdinal = index },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)
                    ) {
                        when (val pState = profileState) {
                            is UiState.Loading -> {
                                LoadingAccountView()
                            }

                            is UiState.Error -> {
                                ErrorAccountView(
                                    message = pState.message,
                                    onRetry = { viewModel.initializeAccount() }
                                )
                            }

                            is UiState.Success -> {
                                val profile = pState.data
                                if (profile == null) {
                                    if (initError != null) {
                                        ErrorAccountView(
                                            message = initError.orEmpty(),
                                            onRetry = { viewModel.initializeAccount() }
                                        )
                                    } else {
                                        LoadingAccountView()
                                    }
                                } else {
                                    SafaTabContent(
                                        tab = currentTab,
                                        profile = profile,
                                        transactions = transactions,
                                        activeBundles = activeBundles,
                                        savedPayees = savedPayees,
                                        discoverSubTab = discoverSubTab,
                                        hasAppPin = savedPin != null,
                                        isBusy = isBusy,
                                        isRefreshingBalance = isRefreshingBalance,
                                        userEmail = userEmail,
                                        userPhotoUrl = userPhotoUrl,
                                        viewModel = viewModel,
                                        onTabChange = { currentTabOrdinal = it.ordinal },
                                        onDiscoverSubTabChange = { discoverSubTab = it },
                                        onOpenSheet = { activeSheet = it },
                                        onOpenAddPayee = { showAddPayeeDialog = true },
                                        onSetAppPin = { newPin ->
                                            pinPrefs.edit().putString("app_pin", newPin).apply()
                                            savedPin = newPin
                                        },
                                        onClearAppPin = {
                                            pinPrefs.edit().remove("app_pin").apply()
                                            savedPin = null
                                        },
                                        onLockAppNow = { isPinLocked = true },
                                        onSignOut = { signOut(credentialManager, scope) }
                                    )

                                    activeSheet?.let { sheet ->
                                        MpesaActionBottomSheet(
                                            sheet = sheet,
                                            profile = profile,
                                            savedPayees = savedPayees,
                                            isBusy = isBusy,
                                            onDismiss = { activeSheet = null },
                                            onSendMoney = { name, phone, amount, note, fuliza ->
                                                viewModel.sendMoney(profile, name, phone, amount, note, fuliza) {
                                                    activeSheet = null
                                                }
                                            },
                                            onPayBill = { biz, paybill, acc, amount, fuliza ->
                                                viewModel.payBill(profile, biz, paybill, acc, amount, fuliza) {
                                                    activeSheet = null
                                                }
                                            },
                                            onBuyGoods = { merchant, till, amount, fuliza ->
                                                viewModel.buyGoods(profile, merchant, till, amount, fuliza) {
                                                    activeSheet = null
                                                }
                                            },
                                            onBuyAirtime = { phone, isSelf, amount, fuliza ->
                                                viewModel.buyAirtime(profile, phone, isSelf, amount, fuliza) {
                                                    activeSheet = null
                                                }
                                            },
                                            onWithdrawCash = { agent, store, amount ->
                                                viewModel.withdrawCash(profile, agent, store, amount) {
                                                    activeSheet = null
                                                }
                                            },
                                            onDepositCash = { amount, label ->
                                                viewModel.depositCash(profile, amount, label) {
                                                    activeSheet = null
                                                }
                                            },
                                            onTransferSavings = { toSavings, amount ->
                                                viewModel.transferSavings(profile, toSavings, amount) {
                                                    activeSheet = null
                                                }
                                            },
                                            onBuyBundle = { offer, payWithMpesa ->
                                                viewModel.buyBundle(profile, offer, payWithMpesa) {
                                                    activeSheet = null
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPayeeDialog) {
        AddSavedPayeeDialog(
            isBusy = isBusy,
            onDismiss = { showAddPayeeDialog = false },
            onSave = { name, type, id, acc ->
                viewModel.addSavedPayee(name, type, id, acc) {
                    showAddPayeeDialog = false
                }
            }
        )
    }

    receipt?.let { activeReceipt ->
        ReceiptConfirmationDialog(
            receipt = activeReceipt,
            onDismiss = { viewModel.dismissReceipt() }
        )
    }
}

@Composable
private fun PinUnlockScreen(
    displayName: String,
    expectedPin: String,
    onUnlocked: () -> Unit,
    onSignOut: () -> Unit,
) {
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SafaDeepPine, SafaForest)))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SafaEmerald.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Locked",
                        tint = SafaEmerald,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Welcome back, ${displayName.substringBefore(" ")}",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Enter your 4-digit SafaOne PIN to unlock",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { value ->
                        if (value.length <= 4 && value.all(Char::isDigit)) {
                            enteredPin = value
                            pinError = null
                            if (value.length == 4) {
                                if (value == expectedPin) {
                                    onUnlocked()
                                } else {
                                    pinError = "Incorrect PIN. Try again."
                                }
                            }
                        }
                    },
                    label = { Text("4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_unlock_input")
                )
                if (pinError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pinError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (enteredPin == expectedPin) {
                            onUnlocked()
                        } else {
                            pinError = "Incorrect PIN. Try again."
                        }
                    },
                    enabled = enteredPin.length == 4,
                    colors = ButtonDefaults.buttonColors(containerColor = SafaEmerald, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pin_unlock_button")
                ) {
                    Text("Unlock SafaOne")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onSignOut) {
                    Text("Switch Google Account")
                }
            }
        }
    }
}

@Composable
private fun SafaTabContent(
    tab: SafaTab,
    profile: AccountProfile,
    transactions: List<com.example.data.WalletTransaction>,
    activeBundles: List<com.example.data.ActiveBundle>,
    savedPayees: List<com.example.data.SavedPayee>,
    discoverSubTab: Int,
    hasAppPin: Boolean,
    isBusy: Boolean,
    isRefreshingBalance: Boolean,
    userEmail: String?,
    userPhotoUrl: String?,
    viewModel: SafaViewModel,
    onTabChange: (SafaTab) -> Unit,
    onDiscoverSubTabChange: (Int) -> Unit,
    onOpenSheet: (MpesaSheet) -> Unit,
    onOpenAddPayee: () -> Unit,
    onSetAppPin: (String) -> Unit,
    onClearAppPin: () -> Unit,
    onLockAppNow: () -> Unit,
    onSignOut: () -> Unit,
) {
    when (tab) {
        SafaTab.HOME -> HomeScreen(
            profile = profile,
            transactions = transactions,
            activeBundles = activeBundles,
            isRefreshingBalance = isRefreshingBalance,
            onRefreshBalance = { viewModel.refreshMpesaBalance() },
            onToggleHideBalances = { viewModel.toggleHideBalances(!profile.hideBalances) },
            onOpenSheet = onOpenSheet,
            onNavigateToMpesa = { onTabChange(SafaTab.MPESA) },
            onNavigateToDiscover = { subTab ->
                onDiscoverSubTabChange(subTab)
                onTabChange(SafaTab.DISCOVER)
            }
        )

        SafaTab.MPESA -> MpesaScreen(
            profile = profile,
            transactions = transactions,
            savedPayees = savedPayees,
            onToggleHideBalances = { viewModel.toggleHideBalances(!profile.hideBalances) },
            onOpenSheet = onOpenSheet,
            onOpenAddPayee = onOpenAddPayee,
            onDeletePayee = { viewModel.deleteSavedPayee(it) }
        )

        SafaTab.DISCOVER -> DiscoverScreen(
            profile = profile,
            selectedSubTab = discoverSubTab,
            onSubTabSelected = onDiscoverSubTabChange,
            isBusy = isBusy,
            onOpenBundleSheet = onOpenSheet,
            onRedeemBonga = { viewModel.redeemBonga(profile, it) },
            onPayFibre = { viewModel.payFibre(profile, it) }
        )

        SafaTab.ACCOUNT -> AccountScreen(
            profile = profile,
            userEmail = userEmail,
            photoUrl = userPhotoUrl,
            hasAppPin = hasAppPin,
            isBusy = isBusy,
            onUpdateProfile = { name, phone, done -> viewModel.updateProfileDetails(name, phone, done) },
            onToggleHideBalances = { viewModel.toggleHideBalances(it) },
            onSetAppPin = onSetAppPin,
            onClearAppPin = onClearAppPin,
            onLockAppNow = onLockAppNow,
            onUssdShortcut = { target ->
                when (target) {
                    "HOME" -> onTabChange(SafaTab.HOME)
                    "MPESA" -> onTabChange(SafaTab.MPESA)
                    "DISCOVER" -> {
                        onDiscoverSubTabChange(0)
                        onTabChange(SafaTab.DISCOVER)
                    }
                    "BONGA" -> {
                        onDiscoverSubTabChange(1)
                        onTabChange(SafaTab.DISCOVER)
                    }
                    "FIBRE" -> {
                        onDiscoverSubTabChange(2)
                        onTabChange(SafaTab.DISCOVER)
                    }
                }
            },
            onSignOut = onSignOut
        )
    }
}

@Composable
private fun LoadingAccountView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = SafaEmerald)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Loading your SafaOne account...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorAccountView(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Couldn't Load Account",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = SafaEmerald, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Retry")
        }
    }
}
