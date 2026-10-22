package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.data.model.Call
import com.example.data.model.Group
import com.example.data.model.User
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserRepository
import com.example.ui.components.IncomingCallDialog
import com.example.ui.screens.*
import com.example.ui.theme.TealPrimary
import com.example.util.LocalStrings
import kotlinx.coroutines.launch

enum class MainTab {
    CHATS,
    PEOPLE,
    CALLS,
    PROFILE
}

sealed class Screen {
    data class Main(val tab: MainTab = MainTab.CHATS) : Screen()
    data class Conversation(
        val chatId: String,
        val title: String,
        val subtitle: String,
        val photoUrl: String,
        val isGroup: Boolean,
        val recipientIds: List<String>
    ) : Screen()
    object CreateGroup : Screen()
    data class GroupInfo(val groupId: String) : Screen()
    object Settings : Screen()
    data class ActiveCall(val callId: String) : Screen()
}

@Composable
fun AppNavigation(
    currentUserId: String,
    userRepository: UserRepository,
    chatRepository: ChatRepository,
    callRepository: CallRepository,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main(MainTab.CHATS)) }
    val screenStack = remember { mutableStateListOf<Screen>() }

    fun navigateTo(screen: Screen) {
        screenStack.add(currentScreen)
        currentScreen = screen
    }

    fun popBack() {
        if (screenStack.isNotEmpty()) {
            currentScreen = screenStack.removeLast()
        } else {
            currentScreen = Screen.Main(MainTab.CHATS)
        }
    }

    // Monitor incoming calls in real-time
    val incomingCalls by callRepository.observeIncomingCalls().collectAsState(initial = emptyList())
    val activeIncomingCall = incomingCalls.firstOrNull()

    // Update user online status on launch and exit
    DisposableEffect(currentUserId) {
        scope.launch { userRepository.updatePresence(true) }
        onDispose {
            scope.launch { userRepository.updatePresence(false) }
        }
    }

    Scaffold(
        bottomBar = {
            if (currentScreen is Screen.Main) {
                val currentTab = (currentScreen as Screen.Main).tab
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.CHATS,
                        onClick = { currentScreen = Screen.Main(MainTab.CHATS) },
                        icon = { Icon(Icons.Default.ChatBubble, contentDescription = strings.tabChats) },
                        label = { Text(strings.tabChats, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TealPrimary,
                            selectedTextColor = TealPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("tab_chats")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.PEOPLE,
                        onClick = { currentScreen = Screen.Main(MainTab.PEOPLE) },
                        icon = { Icon(Icons.Default.People, contentDescription = strings.tabPeople) },
                        label = { Text(strings.tabPeople, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TealPrimary,
                            selectedTextColor = TealPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("tab_people")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.CALLS,
                        onClick = { currentScreen = Screen.Main(MainTab.CALLS) },
                        icon = { Icon(Icons.Default.Phone, contentDescription = strings.tabCalls) },
                        label = { Text(strings.tabCalls, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TealPrimary,
                            selectedTextColor = TealPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("tab_calls")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.PROFILE,
                        onClick = { currentScreen = Screen.Main(MainTab.PROFILE) },
                        icon = { Icon(Icons.Default.Person, contentDescription = strings.tabProfile) },
                        label = { Text(strings.tabProfile, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TealPrimary,
                            selectedTextColor = TealPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("tab_profile")
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val screen = currentScreen) {
                is Screen.Main -> {
                    when (screen.tab) {
                        MainTab.CHATS -> {
                            ChatListScreen(
                                currentUserId = currentUserId,
                                chatRepository = chatRepository,
                                userRepository = userRepository,
                                onOpenGroupChat = { group ->
                                    navigateTo(
                                        Screen.Conversation(
                                            chatId = group.id,
                                            title = group.name,
                                            subtitle = "${group.memberIds.size} জন সদস্য",
                                            photoUrl = group.photoUrl,
                                            isGroup = true,
                                            recipientIds = group.memberIds
                                        )
                                    )
                                },
                                onOpenDirectChat = { chatId, otherUser ->
                                    navigateTo(
                                        Screen.Conversation(
                                            chatId = chatId,
                                            title = otherUser.displayName,
                                            subtitle = if (otherUser.isOnline) strings.online else strings.offline,
                                            photoUrl = otherUser.photoUrl,
                                            isGroup = false,
                                            recipientIds = listOf(otherUser.userId, currentUserId)
                                        )
                                    )
                                },
                                onCreateGroupClicked = {
                                    navigateTo(Screen.CreateGroup)
                                },
                                onJoinCallClicked = { callId, _ ->
                                    scope.launch {
                                        callRepository.joinCall(callId)
                                        navigateTo(Screen.ActiveCall(callId))
                                    }
                                }
                            )
                        }

                        MainTab.PEOPLE -> {
                            PeopleDirectoryScreen(
                                currentUserId = currentUserId,
                                userRepository = userRepository,
                                chatRepository = chatRepository,
                                onStartDirectMessage = { chatId, otherUser ->
                                    navigateTo(
                                        Screen.Conversation(
                                            chatId = chatId,
                                            title = otherUser.displayName,
                                            subtitle = if (otherUser.isOnline) strings.online else strings.offline,
                                            photoUrl = otherUser.photoUrl,
                                            isGroup = false,
                                            recipientIds = listOf(otherUser.userId, currentUserId)
                                        )
                                    )
                                },
                                onStartDirectCall = { targetUser ->
                                    scope.launch {
                                        val result = callRepository.startCall(
                                            channelId = "",
                                            isGroup = false,
                                            title = targetUser.displayName,
                                            participantIds = listOf(targetUser.userId, currentUserId)
                                        )
                                        if (result.isSuccess) {
                                            navigateTo(Screen.ActiveCall(result.getOrThrow()))
                                        }
                                    }
                                }
                            )
                        }

                        MainTab.CALLS -> {
                            CallHistoryScreen(
                                currentUserId = currentUserId,
                                callRepository = callRepository,
                                onCallBackClicked = { call ->
                                    scope.launch {
                                        val result = callRepository.startCall(
                                            channelId = call.channelId,
                                            isGroup = call.isGroup,
                                            title = call.title,
                                            participantIds = call.participantIds
                                        )
                                        if (result.isSuccess) {
                                            navigateTo(Screen.ActiveCall(result.getOrThrow()))
                                        }
                                    }
                                }
                            )
                        }

                        MainTab.PROFILE -> {
                            ProfileScreen(
                                currentUserId = currentUserId,
                                userRepository = userRepository,
                                onOpenSettings = { navigateTo(Screen.Settings) }
                            )
                        }
                    }
                }

                is Screen.Conversation -> {
                    ChatConversationScreen(
                        chatId = screen.chatId,
                        title = screen.title,
                        subtitle = screen.subtitle,
                        photoUrl = screen.photoUrl,
                        isGroup = screen.isGroup,
                        recipientIds = screen.recipientIds,
                        currentUserId = currentUserId,
                        chatRepository = chatRepository,
                        onBackClicked = { popBack() },
                        onStartCallClicked = {
                            scope.launch {
                                val result = callRepository.startCall(
                                    channelId = screen.chatId,
                                    isGroup = screen.isGroup,
                                    title = screen.title,
                                    participantIds = screen.recipientIds
                                )
                                if (result.isSuccess) {
                                    navigateTo(Screen.ActiveCall(result.getOrThrow()))
                                }
                            }
                        },
                        onOpenGroupInfoClicked = {
                            if (screen.isGroup) {
                                navigateTo(Screen.GroupInfo(screen.chatId))
                            }
                        }
                    )
                }

                is Screen.CreateGroup -> {
                    CreateGroupScreen(
                        currentUserId = currentUserId,
                        chatRepository = chatRepository,
                        userRepository = userRepository,
                        onGroupCreated = { newGroupId ->
                            popBack()
                        },
                        onBackClicked = { popBack() }
                    )
                }

                is Screen.GroupInfo -> {
                    GroupInfoScreen(
                        groupId = screen.groupId,
                        currentUserId = currentUserId,
                        chatRepository = chatRepository,
                        userRepository = userRepository,
                        onBackClicked = { popBack() },
                        onGroupLeft = {
                            screenStack.clear()
                            currentScreen = Screen.Main(MainTab.CHATS)
                        }
                    )
                }

                is Screen.Settings -> {
                    SettingsScreen(
                        currentUserId = currentUserId,
                        userRepository = userRepository,
                        onBackClicked = { popBack() },
                        onLogoutCompleted = {
                            onLogout()
                        }
                    )
                }

                is Screen.ActiveCall -> {
                    ActiveCallScreen(
                        callId = screen.callId,
                        currentUserId = currentUserId,
                        callRepository = callRepository,
                        userRepository = userRepository,
                        onEndCall = { popBack() }
                    )
                }
            }

            // Real-time Incoming Call Dialog
            if (activeIncomingCall != null && currentScreen !is Screen.ActiveCall) {
                IncomingCallDialog(
                    call = activeIncomingCall,
                    onAccept = {
                        scope.launch {
                            callRepository.answerCall(activeIncomingCall.id)
                            navigateTo(Screen.ActiveCall(activeIncomingCall.id))
                        }
                    },
                    onDecline = {
                        scope.launch {
                            callRepository.leaveCall(activeIncomingCall.id, activeIncomingCall.channelId)
                        }
                    }
                )
            }
        }
    }
}
