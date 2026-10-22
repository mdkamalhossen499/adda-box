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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.User
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserRepository
import com.example.ui.components.UserAvatar
import com.example.ui.theme.TealPrimary
import com.example.util.LocalStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    currentUserId: String,
    chatRepository: ChatRepository,
    userRepository: UserRepository,
    onGroupCreated: (groupId: String) -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    val selectedMemberIds = remember { mutableStateListOf<String>() }
    var isCreating by remember { mutableStateOf(false) }

    val allUsers by userRepository.observeAllUsers().collectAsState(initial = emptyList())
    val eligibleUsers = remember(allUsers, currentUserId) {
        allUsers.filter { it.userId != currentUserId }
    }

    BackHandler { onBackClicked() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.createGroupTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = Color.Transparent
            ) {
                Button(
                    onClick = {
                        if (groupName.isBlank()) {
                            Toast.makeText(context, "গ্রুপের নাম লিখুন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isCreating = true
                        scope.launch {
                            val result = chatRepository.createGroup(
                                name = groupName.trim(),
                                description = groupDescription.trim(),
                                photoUrl = "",
                                selectedMemberIds = selectedMemberIds.toList()
                            )
                            isCreating = false
                            if (result.isSuccess) {
                                onGroupCreated(result.getOrThrow())
                            } else {
                                Toast.makeText(context, "গ্রুপ তৈরি ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = groupName.isNotBlank() && !isCreating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_create_group_button"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(strings.createGroupTitle, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text(strings.groupNameLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_group_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = groupDescription,
                    onValueChange = { groupDescription = it },
                    label = { Text(strings.groupDescLabel) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_group_desc_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "${strings.selectMembers} (${selectedMemberIds.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            items(eligibleUsers, key = { it.userId }) { user ->
                val isSelected = selectedMemberIds.contains(user.userId)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isSelected) selectedMemberIds.remove(user.userId)
                            else selectedMemberIds.add(user.userId)
                        }
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            name = user.displayName,
                            photoUrl = user.photoUrl,
                            size = 42.dp,
                            isOnline = user.isOnline
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "@${user.username}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) selectedMemberIds.add(user.userId)
                                else selectedMemberIds.remove(user.userId)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                        )
                    }
                }
            }
        }
    }
}
