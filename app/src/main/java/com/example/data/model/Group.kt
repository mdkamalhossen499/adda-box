package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val photoUrl: String = "",
    val creatorId: String = "",
    val adminIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    @get:PropertyName("isDefaultPublic") @set:PropertyName("isDefaultPublic") var isDefaultPublic: Boolean = false,
    val lastMessage: String = "",
    val lastMessageSenderName: String = "",
    val lastMessageTime: Timestamp? = null,
    val activeCallId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
