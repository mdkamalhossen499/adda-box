package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.repository.UserRepository
import com.example.ui.components.UserAvatar
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TealPrimary
import com.example.util.LocalStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentUserId: String,
    userRepository: UserRepository,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val currentUser by userRepository.observeCurrentUser().collectAsState(initial = null)
    var showEditProfileDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.tabProfile, fontWeight = FontWeight.Bold, color = TealPrimary) },
                actions = {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("open_settings_button")) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TealPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (currentUser == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealPrimary)
            }
        } else {
            val user = currentUser!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                UserAvatar(
                    name = user.displayName,
                    photoUrl = user.photoUrl,
                    size = 96.dp,
                    isOnline = user.isOnline
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TealPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Online status chip
                Surface(
                    color = OnlineGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(OnlineGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.online,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnlineGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Unique User ID Card with Copy button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "অনন্য ইউজার আইডি (Unique User ID)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = user.userId,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(user.userId))
                                Toast.makeText(context, strings.userIdCopied, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("copy_own_user_id_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy ID",
                                tint = TealPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bio Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "বায়ো (Bio)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = user.bio.ifEmpty { "কোনো বায়ো যোগ করা হয়নি।" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Edit Profile Button
                Button(
                    onClick = { showEditProfileDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("edit_profile_button"),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.editProfile, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Settings shortcut button
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("settings_shortcut_button"),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp), tint = TealPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.settingsTitle, color = TealPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Edit Profile Modal
    if (showEditProfileDialog && currentUser != null) {
        val user = currentUser!!
        var editDisplayName by remember { mutableStateOf(user.displayName) }
        var editBio by remember { mutableStateOf(user.bio) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text(strings.editProfile) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editDisplayName,
                        onValueChange = { editDisplayName = it },
                        label = { Text(strings.displayNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text(strings.bioLabel) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            userRepository.saveUserProfile(
                                displayName = editDisplayName.trim(),
                                username = user.username,
                                bio = editBio.trim(),
                                photoUrl = user.photoUrl
                            )
                            showEditProfileDialog = false
                            Toast.makeText(context, "প্রোফাইল আপডেট হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
