package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Group
import com.example.data.model.Message
import com.example.data.model.User
import com.example.data.repository.ChatRepository
import com.example.ui.components.UserAvatar
import com.example.ui.theme.*
import com.example.util.LocalStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    chatId: String,
    title: String,
    subtitle: String,
    photoUrl: String,
    isGroup: Boolean,
    recipientIds: List<String>,
    currentUserId: String,
    chatRepository: ChatRepository,
    onBackClicked: () -> Unit,
    onStartCallClicked: () -> Unit,
    onOpenGroupInfoClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var selectedMessageForAction by remember { mutableStateOf<Message?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isTypingSimulator by remember { mutableStateOf(false) }

    // Real-time messages stream
    val messages by chatRepository.observeMessages(chatId).collectAsState(initial = emptyList())

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Auto mark received messages as seen
    LaunchedEffect(messages) {
        messages.filter { it.senderId != currentUserId && !it.seenBy.contains(currentUserId) }
            .forEach { unreadMsg ->
                chatRepository.markMessageSeen(unreadMsg.id)
            }
    }

    // Zero-permission photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                chatRepository.sendMessage(
                    chatId = chatId,
                    text = inputText.trim(),
                    imageUrl = uri.toString(),
                    replyTo = replyingToMessage,
                    recipientIds = recipientIds
                )
                inputText = ""
                replyingToMessage = null
            }
        }
    }

    BackHandler { onBackClicked() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isGroup && onOpenGroupInfoClicked != null) {
                                    onOpenGroupInfoClicked()
                                }
                            }
                    ) {
                        UserAvatar(
                            name = title,
                            photoUrl = photoUrl,
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isTypingSimulator) strings.typing else subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isTypingSimulator) TealLight else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onStartCallClicked,
                        modifier = Modifier.testTag("start_audio_call_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Audio Call",
                            tint = TealPrimary
                        )
                    }
                    if (isGroup && onOpenGroupInfoClicked != null) {
                        IconButton(onClick = onOpenGroupInfoClicked, modifier = Modifier.testTag("group_info_header_button")) {
                            Icon(Icons.Default.Info, contentDescription = "Group Info", tint = TealPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .imePadding()
            ) {
                // Reply quote banner
                if (replyingToMessage != null) {
                    val msg = replyingToMessage!!
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(32.dp)
                                    .background(TealPrimary, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = msg.senderName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary
                                )
                                Text(
                                    text = msg.text.ifEmpty { "[ছবি]" },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { replyingToMessage = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel reply", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Input row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo attachment button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("attach_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Attach Image",
                            tint = TealPrimary
                        )
                    }

                    // Message input field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            // Brief simulated typing signal
                            if (it.length == 1) {
                                isTypingSimulator = true
                                scope.launch {
                                    delay(2000)
                                    isTypingSimulator = false
                                }
                            }
                        },
                        placeholder = { Text(strings.typeMessage) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Send Button
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val textToSend = inputText.trim()
                                val replySnapshot = replyingToMessage
                                inputText = ""
                                replyingToMessage = null
                                scope.launch {
                                    chatRepository.sendMessage(
                                        chatId = chatId,
                                        text = textToSend,
                                        imageUrl = "",
                                        replyTo = replySnapshot,
                                        recipientIds = recipientIds
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("send_message_button"),
                        containerColor = TealPrimary,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = strings.send,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val isMine = message.senderId == currentUserId
                    MessageBubble(
                        message = message,
                        isMine = isMine,
                        isGroup = isGroup,
                        onLongClick = {
                            selectedMessageForAction = message
                        }
                    )
                }
            }
        }
    }

    // Message options bottom sheet / dialog (Reply or Delete)
    if (selectedMessageForAction != null) {
        val msg = selectedMessageForAction!!
        val isMine = msg.senderId == currentUserId
        AlertDialog(
            onDismissRequest = { selectedMessageForAction = null },
            title = { Text(msg.senderName) },
            text = {
                Column {
                    Text(text = msg.text.ifEmpty { "[ছবি]" })
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        replyingToMessage = msg
                        selectedMessageForAction = null
                    }
                ) {
                    Text(strings.reply, fontWeight = FontWeight.Bold, color = TealPrimary)
                }
            },
            dismissButton = {
                if (isMine) {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = true
                        }
                    ) {
                        Text(strings.delete, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

    if (showDeleteConfirmDialog && selectedMessageForAction != null) {
        val msgToDelete = selectedMessageForAction!!
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                selectedMessageForAction = null
            },
            title = { Text(strings.delete) },
            text = { Text(strings.deleteMessageConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        selectedMessageForAction = null
                        scope.launch {
                            chatRepository.deleteMessage(msgToDelete.id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        selectedMessageForAction = null
                    }
                ) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isMine: Boolean,
    isGroup: Boolean,
    onLongClick: () -> Unit
) {
    val bubbleColor = if (isMine) TealPrimary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isMine) 16.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 16.dp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        // Group sender label
        if (isGroup && !isMine && message.senderName.isNotBlank()) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = TealPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }

        Surface(
            color = bubbleColor,
            shape = bubbleShape,
            modifier = Modifier
                .widthIn(max = 290.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Quoted reply content if any
                if (message.replyToText.isNotBlank()) {
                    Surface(
                        color = if (isMine) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(modifier = Modifier.padding(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .background(if (isMine) Color.White else TealPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = message.replyToSenderName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMine) Color.White else TealPrimary
                                )
                                Text(
                                    text = message.replyToText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Attached image if any
                if (message.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Attached image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Message Text
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                }

                // Timestamp and Seen status checkmark
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.createdAt != null) {
                        Text(
                            text = formatTimestamp(message.createdAt.toDate()),
                            fontSize = 10.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                    if (isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val isSeen = message.seenBy.size > 1
                        Icon(
                            imageVector = if (isSeen) Icons.Default.DoneAll else Icons.Default.Check,
                            contentDescription = if (isSeen) "Seen" else "Sent",
                            tint = if (isSeen) Color(0xFF67E8F9) else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
