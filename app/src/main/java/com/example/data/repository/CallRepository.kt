package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Call
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CallRepository(private val db: FirebaseFirestore) {

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

    fun observeIncomingCalls(): Flow<List<Call>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val colRef = db.collection("calls")
        val registration = colRef.whereArrayContains("participantIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, colRef.path)
                    return@addSnapshotListener
                }
                val calls = snapshot?.toObjects(Call::class.java) ?: emptyList()
                val incomingRinging = calls.filter {
                    it.status == "ringing" && it.initiatorId != uid
                }
                trySend(incomingRinging)
            }
        awaitClose { registration.remove() }
    }

    fun observeCall(callId: String): Flow<Call?> = callbackFlow {
        val docRef = db.collection("calls").document(callId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(Call::class.java))
        }
        awaitClose { registration.remove() }
    }

    fun observeCallHistory(): Flow<List<Call>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val colRef = db.collection("calls")
        val registration = colRef.whereArrayContains("participantIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, colRef.path)
                    return@addSnapshotListener
                }
                val calls = snapshot?.toObjects(Call::class.java) ?: emptyList()
                val sorted = calls.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                trySend(sorted)
            }
        awaitClose { registration.remove() }
    }

    suspend fun startCall(
        channelId: String,
        isGroup: Boolean,
        title: String,
        participantIds: List<String>
    ): Result<String> {
        val uid = requireUserId()
        val callId = "call_${UUID.randomUUID().toString().take(12)}"
        val docRef = db.collection("calls").document(callId)
        val initiatorName = auth.currentUser?.displayName ?: "User"
        val allParticipants = (participantIds + uid).distinct()

        return try {
            val data = mapOf(
                "id" to callId,
                "channelId" to channelId,
                "isGroup" to isGroup,
                "title" to title.ifEmpty { "Audio Call" },
                "initiatorId" to uid,
                "initiatorName" to initiatorName,
                "participantIds" to allParticipants,
                "activeParticipantIds" to listOf(uid),
                "status" to "ringing",
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()

            // If it's a group call, update activeCallId on the group
            if (isGroup && channelId.isNotEmpty()) {
                db.collection("groups").document(channelId).update(
                    "activeCallId", callId,
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            }

            Result.success(callId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun answerCall(callId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("calls").document(callId)
        return try {
            docRef.update(
                "status", "active",
                "activeParticipantIds", FieldValue.arrayUnion(uid)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinCall(callId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("calls").document(callId)
        return try {
            docRef.update(
                "status", "active",
                "activeParticipantIds", FieldValue.arrayUnion(uid)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun leaveCall(callId: String, channelId: String = ""): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("calls").document(callId)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val call = snapshot.toObject(Call::class.java)
                val remaining = (call?.activeParticipantIds ?: emptyList()).filter { it != uid }
                if (remaining.isEmpty() || call?.initiatorId == uid) {
                    // Call ends if empty or initiator leaves
                    docRef.update(
                        "status", "ended",
                        "activeParticipantIds", emptyList<String>(),
                        "endedAt", FieldValue.serverTimestamp()
                    ).await()
                    if (channelId.isNotEmpty()) {
                        db.collection("groups").document(channelId).update(
                            "activeCallId", "",
                            "updatedAt", FieldValue.serverTimestamp()
                        ).await()
                    }
                } else {
                    docRef.update(
                        "activeParticipantIds", FieldValue.arrayRemove(uid)
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }
}
