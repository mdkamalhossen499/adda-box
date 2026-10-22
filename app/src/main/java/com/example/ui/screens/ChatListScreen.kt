package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Chat
import com.example.data.model.Group
import com.example.data.model.User
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserRepository
import com.example.ui.components.UserAvatar
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.util.LocalStrings
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    currentUserId: String,
    chatRepository: ChatRepository,
    userRepository: UserRepository,
    onOpenGroupChat: (Group) -> Unit,
    onOpenDirectChat: (chatId: String, otherUser: User) -> Unit,
    onCreateGroupClicked: () -> Unit,
    onJoinCallClicked: (callId: String, channelTitle: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val groups by chatRepository.observeUserGroups().collectAsState(initial = emptyList())
    val directChats by chatRepository.observeUserDirectChats().collectAsState(initial = emptyList())
    val allUsers by userRepository.observeAllUsers().collectAsState(initial = emptyList())

    // Ensure default public group exists
    LaunchedEffect(Unit) {
        chatRepository.ensureDefaultPublicGroup()
    }

    // Map other users for direct chats
    val userMap = remember(allUsers) { allUsers.associateBy { it.userId } }

    val filteredGroups = remember(groups, searchQuery) {
        if (searchQuery.isBlank()) groups
        else groups.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val filteredDirectChats = remember(directChats, searchQuery, userMap) {
        if (searchQuery.isBlank()) directChats
        else directChats.filter { chat ->
            val otherUid = chat.participantIds.firstOrNull { it != currentUserId }
            val otherUser = userMap[otherUid]
            otherUser?.displayName?.contains(searchQuery, ignoreCase = true) == true ||
                    otherUser?.username?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(strings.searchPlaceholder) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_search_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = TealContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "আড্ডা",
                                    color = TealPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        },
                        modifier = Modifier.testTag("toggle_chat_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TealPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateGroupClicked,
                icon = { Icon(Icons.Default.Add, contentDescription = "New Group") },
                text = { Text(strings.newGroup, fontWeight = FontWeight.Bold) },
                containerColor = TealPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_group_fab")
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Public group "সবাই" pinned at top if exists
            val defaultGroup = groups.firstOrNull { it.id == ChatRepository.PUBLIC_GROUP_ID || it.isDefaultPublic }
            if (defaultGroup != null) {
                item {
                    PublicGroupBanner(
                        group = defaultGroup,
                        onClick = { onOpenGroupChat(defaultGroup) },
                        onJoinCall = {
                            if (defaultGroup.activeCallId.isNotBlank()) {
                                onJoinCallClicked(defaultGroup.activeCallId, defaultGroup.name)
                            }
                        }
                    )
                }
            }

            // Other groups
            val otherGroups = filteredGroups.filter { it.id != ChatRepository.PUBLIC_GROUP_ID && !it.isDefaultPublic }
            if (otherGroups.isNotEmpty()) {
                item {
                    Text(
                        text = "গ্রুপসমূহ (Groups)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                    )
                }
                items(otherGroups, key = { "grp_${it.id}" }) { group ->
                    GroupChatItem(
                        group = group,
                        onClick = { onOpenGroupChat(group) },
                        onJoinCall = {
                            if (group.activeCallId.isNotBlank()) {
                                onJoinCallClicked(group.activeCallId, group.name)
                            }
                        }
                    )
                }
            }

            // Direct 1-to-1 chats
            if (filteredDirectChats.isNotEmpty()) {
                item {
                    Text(
                        text = "ব্যক্তিগত আড্ডা (Direct Messages)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                    )
                }
                items(filteredDirectChats, key = { "dm_${it.id}" }) { chat ->
                    val otherUid = chat.participantIds.firstOrNull { it != currentUserId } ?: ""
                    val otherUser = userMap[otherUid] ?: User(displayName = "ব্যবহারকারী", username = otherUid.take(6))
                    DirectChatItem(
                        chat = chat,
                        otherUser = otherUser,
                        onClick = { onOpenDirectChat(chat.id, otherUser) }
                    )
                }
            }

            if (otherGroups.isEmpty() && filteredDirectChats.isEmpty() && defaultGroup == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.emptyChats,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PublicGroupBanner(
    group: Group,
    onClick: () -> Unit,
    onJoinCall: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
            .testTag("public_group_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(TealPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Public Group",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = group.description.ifEmpty { strings.publicGroupDesc },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Live active call badge if call ongoing in "সবাই"
            if (group.activeCallId.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = OnlineGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Active Call",
                                tint = OnlineGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.activeCallBanner,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnlineGreen
                            )
                        }
                        Button(
                            onClick = onJoinCall,
                            colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = strings.joinCall,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupChatItem(
    group: Group,
    onClick: () -> Unit,
    onJoinCall: () -> Unit
) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    name = group.name,
                    photoUrl = group.photoUrl,
                    size = 50.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (group.lastMessageTime != null) {
                            Text(
                                text = formatTimestamp(group.lastMessageTime.toDate()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (group.lastMessageSenderName.isNotBlank()) "${group.lastMessageSenderName}: ${group.lastMessage}" else group.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (group.activeCallId.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(OnlineGreen.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = OnlineGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.activeCallBanner, style = MaterialTheme.typography.labelSmall, color = OnlineGreen)
                    }
                    TextButton(onClick = onJoinCall, contentPadding = PaddingValues(0.dp)) {
                        Text(strings.joinCall, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = OnlineGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun DirectChatItem(
    chat: Chat,
    otherUser: User,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
                name = otherUser.displayName,
                photoUrl = otherUser.photoUrl,
                size = 50.dp,
                isOnline = otherUser.isOnline
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = otherUser.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (chat.lastMessageTime != null) {
                        Text(
                            text = formatTimestamp(chat.lastMessageTime.toDate()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = chat.lastMessage.ifEmpty { "@${otherUser.username}" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun formatTimestamp(date: Date): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(date)
}
