package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Chat
import com.example.data.model.Group
import com.example.data.model.Message
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google")
    }

    companion object {
        const val PUBLIC_GROUP_ID = "default_group_shobai"
    }

    // Ensure the default public group "সবাই" exists and current user is in memberIds
    suspend fun ensureDefaultPublicGroup(): Result<Group> {
        val uid = requireUserId()
        val docRef = db.collection("groups").document(PUBLIC_GROUP_ID)
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val groupData = mapOf(
                    "id" to PUBLIC_GROUP_ID,
                    "name" to "সবাই (পাবলিক আড্ডা)",
                    "description" to "আড্ডার সকল সদস্যের উন্মুক্ত গ্রুপ",
                    "photoUrl" to "",
                    "creatorId" to uid,
                    "adminIds" to listOf(uid),
                    "memberIds" to listOf(uid),
                    "isDefaultPublic" to true,
                    "lastMessage" to "আড্ডায় সবাইকে স্বাগতম!",
                    "lastMessageSenderName" to "সিস্টেম",
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "activeCallId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(groupData).await()
                val created = docRef.get().await().toObject(Group::class.java)!!
                Result.success(created)
            } else {
                val group = snapshot.toObject(Group::class.java)!!
                if (!group.memberIds.contains(uid)) {
                    docRef.update(
                        "memberIds", FieldValue.arrayUnion(uid),
                        "updatedAt", FieldValue.serverTimestamp()
                    ).await()
                }
                Result.success(group)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // Observe all groups that current user belongs to or that are public
    fun observeUserGroups(): Flow<List<Group>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val colRef = db.collection("groups")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, colRef.path)
                return@addSnapshotListener
            }
            val groups = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(Group::class.java)
                } catch (e: Exception) {
                    println("DESERIALIZATION FAILED FOR DOC: ${doc.id}: ${e.message}")
                    e.printStackTrace()
                    null
                }
            } ?: emptyList()
            val filtered = groups.filter { it.isDefaultPublic || it.memberIds.contains(uid) }
            trySend(filtered)
        }
        awaitClose { registration.remove() }
    }

    fun observeGroup(groupId: String): Flow<Group?> = callbackFlow {
        val docRef = db.collection("groups").document(groupId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(Group::class.java))
        }
        awaitClose { registration.remove() }
    }

    suspend fun createGroup(
        name: String,
        description: String,
        photoUrl: String,
        selectedMemberIds: List<String>
    ): Result<String> {
        val uid = requireUserId()
        val groupId = "grp_${UUID.randomUUID().toString().take(12)}"
        val docRef = db.collection("groups").document(groupId)
        val allMembers = (selectedMemberIds + uid).distinct()
        return try {
            val groupData = mapOf(
                "id" to groupId,
                "name" to name.trim().ifEmpty { "নতুন গ্রুপ" },
                "description" to description.trim(),
                "photoUrl" to photoUrl,
                "creatorId" to uid,
                "adminIds" to listOf(uid),
                "memberIds" to allMembers,
                "isDefaultPublic" to false,
                "lastMessage" to "গ্রুপ তৈরি করা হয়েছে",
                "lastMessageSenderName" to (auth.currentUser?.displayName ?: "User"),
                "lastMessageTime" to FieldValue.serverTimestamp(),
                "activeCallId" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(groupData).await()
            Result.success(groupId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addMemberToGroup(groupId: String, targetUid: String): Result<Unit> {
        val docRef = db.collection("groups").document(groupId)
        return try {
            docRef.update(
                "memberIds", FieldValue.arrayUnion(targetUid),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeMemberFromGroup(groupId: String, targetUid: String): Result<Unit> {
        val docRef = db.collection("groups").document(groupId)
        return try {
            docRef.update(
                "memberIds", FieldValue.arrayRemove(targetUid),
                "adminIds", FieldValue.arrayRemove(targetUid),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun leaveGroup(groupId: String): Result<Unit> {
        val uid = requireUserId()
        return removeMemberFromGroup(groupId, uid)
    }

    // Direct 1-to-1 chats
    fun observeUserDirectChats(): Flow<List<Chat>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val colRef = db.collection("chats")
        val registration = colRef.whereArrayContains("participantIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, colRef.path)
                    return@addSnapshotListener
                }
                val chats = snapshot?.toObjects(Chat::class.java) ?: emptyList()
                trySend(chats)
            }
        awaitClose { registration.remove() }
    }

    suspend fun getOrCreateDirectChat(otherUid: String): Result<String> {
        val uid = requireUserId()
        val sortedUids = listOf(uid, otherUid).sorted()
        val chatId = "dm_${sortedUids[0]}_${sortedUids[1]}"
        val docRef = db.collection("chats").document(chatId)
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val data = mapOf(
                    "id" to chatId,
                    "participantIds" to sortedUids,
                    "lastMessage" to "",
                    "lastMessageSenderId" to "",
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(data).await()
            }
            Result.success(chatId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // Messages
    fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val colRef = db.collection("messages")
        val registration = colRef.whereEqualTo("chatId", chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, colRef.path)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                // Sort ascending by creation time
                val sorted = messages.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                trySend(sorted)
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(
        chatId: String,
        text: String,
        imageUrl: String = "",
        replyTo: Message? = null,
        recipientIds: List<String>
    ): Result<String> {
        val uid = requireUserId()
        val messageId = "msg_${UUID.randomUUID().toString().take(12)}"
        val docRef = db.collection("messages").document(messageId)
        val senderDisplayName = auth.currentUser?.displayName ?: "User"
        val senderPhoto = auth.currentUser?.photoUrl?.toString() ?: ""
        val allRecipients = (recipientIds + uid).distinct()

        return try {
            val data = mapOf(
                "id" to messageId,
                "chatId" to chatId,
                "senderId" to uid,
                "senderName" to senderDisplayName,
                "senderPhoto" to senderPhoto,
                "text" to text,
                "imageUrl" to imageUrl,
                "replyToMessageId" to (replyTo?.id ?: ""),
                "replyToText" to (replyTo?.text?.take(200) ?: ""),
                "replyToSenderName" to (replyTo?.senderName ?: ""),
                "seenBy" to listOf(uid),
                "recipientIds" to allRecipients,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()

            // Update parent group or direct chat lastMessage snippet
            if (chatId.startsWith("grp_") || chatId == PUBLIC_GROUP_ID) {
                db.collection("groups").document(chatId).update(
                    "lastMessage", text.ifEmpty { "[ছবি]" },
                    "lastMessageSenderName", senderDisplayName,
                    "lastMessageTime", FieldValue.serverTimestamp(),
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            } else if (chatId.startsWith("dm_")) {
                db.collection("chats").document(chatId).update(
                    "lastMessage", text.ifEmpty { "[ছবি]" },
                    "lastMessageSenderId", uid,
                    "lastMessageTime", FieldValue.serverTimestamp(),
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            }

            Result.success(messageId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markMessageSeen(messageId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val docRef = db.collection("messages").document(messageId)
        return try {
            docRef.update("seenBy", FieldValue.arrayUnion(uid)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> {
        val docRef = db.collection("messages").document(messageId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
