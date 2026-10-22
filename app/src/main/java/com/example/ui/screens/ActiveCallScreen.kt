package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Call
import com.example.data.model.User
import com.example.data.repository.CallRepository
import com.example.data.repository.UserRepository
import com.example.ui.components.UserAvatar
import com.example.ui.theme.CallRed
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.util.LocalStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ActiveCallScreen(
    callId: String,
    currentUserId: String,
    callRepository: CallRepository,
    userRepository: UserRepository,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var callDurationSeconds by remember { mutableIntStateOf(0) }

    val call by callRepository.observeCall(callId).collectAsState(initial = null)
    val allUsers by userRepository.observeAllUsers().collectAsState(initial = emptyList())
    val userMap = remember(allUsers) { allUsers.associateBy { it.userId } }

    // Timer for call duration
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            callDurationSeconds++
        }
    }

    // Auto exit if call is ended
    LaunchedEffect(call?.status) {
        if (call != null && call?.status == "ended") {
            onEndCall()
        }
    }

    BackHandler {
        // Leaving the call
        scope.launch {
            callRepository.leaveCall(callId, call?.channelId ?: "")
            onEndCall()
        }
    }

    val activeParticipantUids = call?.activeParticipantIds ?: listOf(currentUserId)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Call Title & Type
            Text(
                text = call?.title ?: strings.activeCallTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Duration format mm:ss
            val minutes = callDurationSeconds / 60
            val seconds = callDurationSeconds % 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Grid of participant avatars with speaking highlight ring
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (activeParticipantUids.size > 2) 2 else 1),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(activeParticipantUids) { uid ->
                    val user = userMap[uid]
                    val isCurrentSpeaker = remember { mutableStateOf(uid == currentUserId && !isMuted) }

                    // Alternate speaking animation for lively conversation vibe
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.08f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(if (isCurrentSpeaker.value) pulseScale else 1f)
                                    .border(
                                        width = if (isCurrentSpeaker.value) 3.dp else 0.dp,
                                        color = if (isCurrentSpeaker.value) OnlineGreen else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .padding(6.dp)
                            ) {
                                UserAvatar(
                                    name = user?.displayName ?: "User",
                                    photoUrl = user?.photoUrl,
                                    size = 84.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (uid == currentUserId) "${user?.displayName ?: "আমি"} (আপনি)" else user?.displayName ?: "ব্যবহারকারী",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uid == currentUserId && isMuted) {
                                    Icon(
                                        imageVector = Icons.Default.MicOff,
                                        contentDescription = "Muted",
                                        tint = CallRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("মিউট", color = CallRed, fontSize = 12.sp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = "Speaking",
                                        tint = OnlineGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.speaking, color = OnlineGreen, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Controls Bar (Mute, Speaker, End Call)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute / Unmute
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) CallRed else Color(0xFF334155))
                        .testTag("toggle_mute_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isMuted) strings.unmute else strings.mute,
                        tint = Color.White
                    )
                }

                // End Call Button
                IconButton(
                    onClick = {
                        scope.launch {
                            callRepository.leaveCall(callId, call?.channelId ?: "")
                            onEndCall()
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(CallRed)
                        .testTag("end_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = strings.endCall,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Speaker Toggle
                IconButton(
                    onClick = { isSpeakerOn = !isSpeakerOn },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isSpeakerOn) TealPrimary else Color(0xFF334155))
                        .testTag("toggle_speaker_button")
                ) {
                    Icon(
                        imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        contentDescription = strings.speaker,
                        tint = Color.White
                    )
                }
            }
        }
    }
}
