package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhoto: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val replyToMessageId: String = "",
    val replyToText: String = "",
    val replyToSenderName: String = "",
    val seenBy: List<String> = emptyList(),
    val recipientIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)
