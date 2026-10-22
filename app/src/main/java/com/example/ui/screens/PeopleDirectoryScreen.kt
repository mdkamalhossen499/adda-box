package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserRepository
import com.example.ui.components.UserAvatar
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TealPrimary
import com.example.util.LocalStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleDirectoryScreen(
    currentUserId: String,
    userRepository: UserRepository,
    chatRepository: ChatRepository,
    onStartDirectMessage: (chatId: String, otherUser: User) -> Unit,
    onStartDirectCall: (targetUser: User) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedUserForDialog by remember { mutableStateOf<User?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }

    val allUsers by userRepository.observeAllUsers().collectAsState(initial = emptyList())
    val currentUserProfile by userRepository.observeCurrentUser().collectAsState(initial = null)

    val blockedIds = remember(currentUserProfile) {
        currentUserProfile?.blockedUserIds ?: emptyList()
    }

    val filteredUsers = remember(allUsers, searchQuery, currentUserId) {
        allUsers.filter { it.userId != currentUserId }
            .filter { user ->
                if (searchQuery.isBlank()) true
                else user.displayName.contains(searchQuery, ignoreCase = true) ||
                        user.username.contains(searchQuery, ignoreCase = true)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.peopleDirectoryTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
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
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = TealPrimary)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("people_search_input"),
                shape = RoundedCornerShape(24.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filteredUsers, key = { it.userId }) { user ->
                    val isBlocked = blockedIds.contains(user.userId)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedUserForDialog = user }
                            .testTag("user_item_${user.username}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                name = user.displayName,
                                photoUrl = user.photoUrl,
                                size = 48.dp,
                                isOnline = user.isOnline
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isBlocked) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "ব্লকড",
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "@${user.username}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TealPrimary
                                )
                                if (user.bio.isNotBlank()) {
                                    Text(
                                        text = user.bio,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Quick Message Action button
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val result = chatRepository.getOrCreateDirectChat(user.userId)
                                        if (result.isSuccess) {
                                            onStartDirectMessage(result.getOrThrow(), user)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Message",
                                    tint = TealPrimary
                                )
                            }
                        }
                    }
                }

                if (filteredUsers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "কোনো ব্যবহারকারী পাওয়া যায়নি",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // User Profile Action Modal (Message, Call, Block, Report)
    if (selectedUserForDialog != null) {
        val targetUser = selectedUserForDialog!!
        val isBlocked = blockedIds.contains(targetUser.userId)

        AlertDialog(
            onDismissRequest = { selectedUserForDialog = null },
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserAvatar(
                        name = targetUser.displayName,
                        photoUrl = targetUser.photoUrl,
                        size = 80.dp,
                        isOnline = targetUser.isOnline
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = targetUser.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${targetUser.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TealPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // User ID with copy button
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            clipboardManager.setText(AnnotatedString(targetUser.userId))
                            Toast.makeText(context, strings.userIdCopied, Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ID: ${targetUser.userId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(14.dp),
                                tint = TealPrimary
                            )
                        }
                    }

                    if (targetUser.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = targetUser.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action: Message
                    Button(
                        onClick = {
                            val userToChat = targetUser
                            selectedUserForDialog = null
                            scope.launch {
                                val result = chatRepository.getOrCreateDirectChat(userToChat.userId)
                                if (result.isSuccess) {
                                    onStartDirectMessage(result.getOrThrow(), userToChat)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.messageBtn, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action: Audio Call
                    OutlinedButton(
                        onClick = {
                            val userToCall = targetUser
                            selectedUserForDialog = null
                            onStartDirectCall(userToCall)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp), tint = TealPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.callBtn, color = TealPrimary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary: Block & Report
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    if (isBlocked) {
                                        userRepository.unblockUser(targetUser.userId)
                                    } else {
                                        userRepository.blockUser(targetUser.userId)
                                        Toast.makeText(context, strings.userBlocked, Toast.LENGTH_SHORT).show()
                                    }
                                    selectedUserForDialog = null
                                }
                            }
                        ) {
                            Text(
                                text = if (isBlocked) strings.unblockBtn else strings.blockBtn,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        TextButton(
                            onClick = {
                                showReportDialog = true
                            }
                        ) {
                            Text(strings.reportBtn, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedUserForDialog = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showReportDialog && selectedUserForDialog != null) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text(strings.reportBtn) },
            text = { Text("ব্যবহারকারীকে অনুপযুক্ত আচরণ বা স্প্যামের জন্য রিপোর্ট করতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        showReportDialog = false
                        selectedUserForDialog = null
                        Toast.makeText(context, strings.userReported, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.reportBtn, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
