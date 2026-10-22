package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Call(
    val id: String = "",
    val channelId: String = "",
    @get:PropertyName("isGroup") @set:PropertyName("isGroup") var isGroup: Boolean = false,
    val title: String = "",
    val initiatorId: String = "",
    val initiatorName: String = "",
    val participantIds: List<String> = emptyList(),
    val activeParticipantIds: List<String> = emptyList(),
    val status: String = "ringing",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val endedAt: Timestamp? = null
)
