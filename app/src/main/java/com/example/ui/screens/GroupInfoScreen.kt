package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PersonAdd
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
import com.example.data.model.Group
import com.example.data.model.User
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserRepository
import com.example.ui.components.UserAvatar
import com.example.ui.theme.TealPrimary
import com.example.util.LocalStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    groupId: String,
    currentUserId: String,
    chatRepository: ChatRepository,
    userRepository: UserRepository,
    onBackClicked: () -> Unit,
    onGroupLeft: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    val group by chatRepository.observeGroup(groupId).collectAsState(initial = null)
    val allUsers by userRepository.observeAllUsers().collectAsState(initial = emptyList())
    val userMap = remember(allUsers) { allUsers.associateBy { it.userId } }

    var showAddMemberDialog by remember { mutableStateOf(false) }

    BackHandler { onBackClicked() }

    val isAdmin = remember(group, currentUserId) {
        group?.adminIds?.contains(currentUserId) == true || group?.creatorId == currentUserId
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.groupInfoTitle, fontWeight = FontWeight.Bold) },
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
        if (group == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealPrimary)
            }
        } else {
            val grp = group!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        UserAvatar(
                            name = grp.name,
                            photoUrl = grp.photoUrl,
                            size = 80.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = grp.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (grp.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = grp.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.membersCount(grp.memberIds.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                        if (isAdmin) {
                            TextButton(onClick = { showAddMemberDialog = true }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp), tint = TealPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("সদস্য যোগ করুন", color = TealPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(grp.memberIds) { memberUid ->
                    val user = userMap[memberUid]
                    val isMemberAdmin = grp.adminIds.contains(memberUid) || grp.creatorId == memberUid
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                name = user?.displayName ?: "User",
                                photoUrl = user?.photoUrl,
                                size = 42.dp,
                                isOnline = user?.isOnline
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (memberUid == currentUserId) "${user?.displayName ?: "আমি"} (আপনি)" else user?.displayName ?: "ব্যবহারকারী",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (isMemberAdmin) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = strings.adminBadge,
                                                color = TealPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "@${user?.username ?: memberUid.take(6)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Remove member option if current user is admin and target is not creator
                            if (isAdmin && memberUid != currentUserId && memberUid != grp.creatorId) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            chatRepository.removeMemberFromGroup(groupId, memberUid)
                                            Toast.makeText(context, "সদস্যকে অপসারণ করা হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = strings.removeMember, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                // Leave group button (if not default public group)
                if (!grp.isDefaultPublic && grp.id != ChatRepository.PUBLIC_GROUP_ID) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    chatRepository.leaveGroup(groupId)
                                    onGroupLeft()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("leave_group_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.leaveGroup, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showAddMemberDialog && group != null) {
        val currentMemberIds = group!!.memberIds
        val nonMembers = allUsers.filter { !currentMemberIds.contains(it.userId) }

        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("নতুন সদস্য যোগ করুন") },
            text = {
                if (nonMembers.isEmpty()) {
                    Text("সবাই ইতোমধ্যে এই গ্রুপে যুক্ত আছেন।")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(nonMembers) { candidate ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UserAvatar(candidate.displayName, candidate.photoUrl, size = 36.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(candidate.displayName, fontWeight = FontWeight.SemiBold)
                                        Text("@${candidate.username}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            chatRepository.addMemberToGroup(groupId, candidate.userId)
                                            Toast.makeText(context, "${candidate.displayName} কে যুক্ত করা হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                                ) {
                                    Text("যোগ করুন", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
