package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.repository.UserRepository
import com.example.ui.theme.TealPrimary
import com.example.util.AppLanguage
import com.example.util.AppLanguageState
import com.example.util.LocalStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUserId: String,
    userRepository: UserRepository,
    onBackClicked: () -> Unit,
    onLogoutCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val currentUser by userRepository.observeCurrentUser().collectAsState(initial = null)
    val allUsers by userRepository.observeAllUsers().collectAsState(initial = emptyList())
    val userMap = remember(allUsers) { allUsers.associateBy { it.userId } }

    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showBlockedUsersDialog by remember { mutableStateOf(false) }

    BackHandler { onBackClicked() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Language Selection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(strings.languageSetting, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (AppLanguageState.currentLanguage == AppLanguage.BANGLA) "বাংলা (ডিফল্ট)" else "English",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = AppLanguageState.currentLanguage == AppLanguage.BANGLA,
                        onCheckedChange = { isBangla ->
                            AppLanguageState.currentLanguage = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = TealPrimary)
                    )
                }
            }

            // Dark Mode Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(strings.darkModeSetting, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (AppLanguageState.isDarkMode) "সক্রিয় (On)" else "নিষ্ক্রিয় (Off)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = AppLanguageState.isDarkMode,
                        onCheckedChange = { AppLanguageState.isDarkMode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = TealPrimary)
                    )
                }
            }

            // Blocked Users list button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { showBlockedUsersDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(strings.blockedUsers, fontWeight = FontWeight.SemiBold)
                            val count = currentUser?.blockedUserIds?.size ?: 0
                            Text(
                                text = "$count জন ব্যবহারকারী",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = { showLogoutConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.logout, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text(strings.logout) },
            text = { Text(strings.logoutConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        signOutUser(context, credentialManager, scope) {
                            onLogoutCompleted()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.logout, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Blocked Users Modal
    if (showBlockedUsersDialog) {
        val blockedIds = currentUser?.blockedUserIds ?: emptyList()
        AlertDialog(
            onDismissRequest = { showBlockedUsersDialog = false },
            title = { Text(strings.blockedUsers) },
            text = {
                if (blockedIds.isEmpty()) {
                    Text("কোনো ব্লক করা ব্যবহারকারী নেই।")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(blockedIds) { targetUid ->
                            val user = userMap[targetUid]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(user?.displayName ?: "User", fontWeight = FontWeight.SemiBold)
                                    Text("@${user?.username ?: targetUid.take(6)}", fontSize = 11.sp, color = Color.Gray)
                                }
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            userRepository.unblockUser(targetUid)
                                            Toast.makeText(context, "আনব্লক করা হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Text(strings.unblockBtn, color = TealPrimary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBlockedUsersDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
