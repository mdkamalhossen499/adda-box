package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Call
import com.example.data.repository.CallRepository
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TealPrimary
import com.example.util.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(
    currentUserId: String,
    callRepository: CallRepository,
    onCallBackClicked: (Call) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val calls by callRepository.observeCallHistory().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.callHistoryTitle,
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
        if (calls.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "এখনো কোনো কলের ইতিহাস নেই। বন্ধু বা গ্রুপে অডিও কল শুরু করুন!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(calls, key = { it.id }) { call ->
                    val isOutgoing = call.initiatorId == currentUserId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCallBackClicked(call) }
                            .padding(vertical = 6.dp),
                        color = Color.Transparent
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (call.isGroup) Icons.Default.Group else Icons.Default.Call,
                                        contentDescription = null,
                                        tint = TealPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = call.title.ifEmpty { if (call.isGroup) "গ্রুপ কল" else call.initiatorName },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isOutgoing) Icons.Default.CallMade else Icons.Default.CallReceived,
                                        contentDescription = null,
                                        tint = if (isOutgoing) TealPrimary else OnlineGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (call.createdAt != null) formatTimestamp(call.createdAt.toDate()) else "সম্প্রতি",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onCallBackClicked(call) },
                                modifier = Modifier.testTag("callback_button_${call.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call Back",
                                    tint = TealPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
