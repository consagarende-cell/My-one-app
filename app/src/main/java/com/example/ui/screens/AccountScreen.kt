package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.AccountProfile
import com.example.data.SafaCatalog
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.SafaEmerald
import com.example.ui.theme.SafaRed

@Composable
fun AccountScreen(
    profile: AccountProfile,
    userEmail: String?,
    photoUrl: String?,
    hasAppPin: Boolean,
    isBusy: Boolean,
    onUpdateProfile: (displayName: String, phoneNumber: String, onDone: () -> Unit) -> Unit,
    onToggleHideBalances: (Boolean) -> Unit,
    onSetAppPin: (String) -> Unit,
    onClearAppPin: () -> Unit,
    onLockAppNow: () -> Unit,
    onUssdShortcut: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Profile Identity Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(SafaEmerald.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "Profile avatar",
                                tint = SafaEmerald,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile.displayName, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = profile.phoneNumber,
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                            color = SafaEmerald
                        )
                        if (!userEmail.isNullOrBlank()) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit line details",
                            tint = SafaEmerald
                        )
                    }
                }
            }
        }

        // 2. SIM, Security PIN & Privacy Settings
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SimCard,
                            contentDescription = "SIM Status",
                            tint = SafaEmerald
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SIM & Tariff Plan", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${profile.planName} • Active 5G eSIM",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleHideBalances(!profile.hideBalances) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.VisibilityOff,
                                contentDescription = "Hide balances",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Privacy Mask", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Hide wallet & airtime balances on screen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = profile.hideBalances,
                            onCheckedChange = onToggleHideBalances
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPinDialog = true }
                            .testTag("app_pin_settings_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "SafaOne PIN Lock",
                                tint = SafaEmerald
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("SafaOne 4-Digit PIN", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = if (hasAppPin) "PIN enabled • Tap to change or remove" else "Set a quick 4-digit unlock PIN",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (hasAppPin) {
                            TextButton(
                                onClick = onLockAppNow,
                                modifier = Modifier.testTag("lock_app_now_button")
                            ) {
                                Text("Lock Now", color = SafaEmerald)
                            }
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Configure PIN",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. USSD Quick Self-Care Directory
        item {
            SectionHeaderRow(title = "USSD Quick Self-Service")
        }

        items(SafaCatalog.ussdShortcuts, key = { it.code }) { shortcut ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUssdShortcut(shortcut.targetAction) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = SafaEmerald.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Dialpad,
                                contentDescription = shortcut.code,
                                tint = SafaEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = shortcut.code,
                                style = MaterialTheme.typography.labelSmall,
                                color = SafaEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(shortcut.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = shortcut.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Open ${shortcut.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. Sign Out / Switch Account
        item {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SafaRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = "Sign out"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out / Switch Account", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            initialName = profile.displayName,
            initialPhone = profile.phoneNumber,
            isBusy = isBusy,
            onDismiss = { showEditDialog = false },
            onSave = { name, phone ->
                onUpdateProfile(name, phone) { showEditDialog = false }
            }
        )
    }

    if (showPinDialog) {
        SetAppPinDialog(
            hasExistingPin = hasAppPin,
            onDismiss = { showPinDialog = false },
            onSavePin = { pin ->
                onSetAppPin(pin)
                showPinDialog = false
            },
            onRemovePin = {
                onClearAppPin()
                showPinDialog = false
            }
        )
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialPhone: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit SafaOne Line Details") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone) },
                enabled = !isBusy && name.trim().isNotEmpty() && phone.trim().length >= 7,
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

@Composable
private fun SetAppPinDialog(
    hasExistingPin: Boolean,
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit,
    onRemovePin: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val pinValid = pin.length == 4 && pin.all { it.isDigit() } && pin == confirmPin

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasExistingPin) "Update SafaOne PIN" else "Set 4-Digit SafaOne PIN") },
        text = {
            Column {
                Text(
                    text = "Use a 4-digit PIN to quickly lock and unlock your SafaOne wallet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
                    label = { Text("New 4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) confirmPin = it },
                    label = { Text("Confirm 4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (hasExistingPin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onRemovePin) {
                        Text("Remove PIN Lock", color = SafaRed)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSavePin(pin) },
                enabled = pinValid,
                colors = ButtonDefaults.buttonColors(containerColor = SafaEmerald, contentColor = Color.White)
            ) {
                Text("Save PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
