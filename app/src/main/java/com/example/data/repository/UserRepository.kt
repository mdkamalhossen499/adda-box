package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.User
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore) {

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

    fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val docRef = db.collection("users").document(uid)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject(User::class.java))
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun getUserProfile(userId: String): User? {
        val docRef = db.collection("users").document(userId)
        return try {
            val snapshot = docRef.get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            null
        }
    }

    fun observeAllUsers(): Flow<List<User>> = callbackFlow {
        val colRef = db.collection("users")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, colRef.path)
                return@addSnapshotListener
            }
            val users = snapshot?.toObjects(User::class.java) ?: emptyList()
            trySend(users)
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveUserProfile(
        displayName: String,
        username: String,
        bio: String,
        photoUrl: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val snapshot = docRef.get().await()
            val cleanUsername = username.trim().replace("@", "").ifEmpty { "user_${uid.take(6)}" }
            val cleanName = displayName.trim().ifEmpty { "User" }
            if (snapshot.exists()) {
                val updates = mapOf(
                    "displayName" to cleanName,
                    "username" to cleanUsername,
                    "bio" to bio,
                    "photoUrl" to photoUrl,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val data = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "username" to cleanUsername,
                    "bio" to bio,
                    "photoUrl" to photoUrl,
                    "isOnline" to true,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "blockedUserIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(data).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePresence(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("users").document(uid)
        try {
            val updates = mapOf(
                "isOnline" to isOnline,
                "lastSeen" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    suspend fun blockUser(targetUid: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            docRef.update("blockedUserIds", FieldValue.arrayUnion(targetUid), "updatedAt", FieldValue.serverTimestamp()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(targetUid: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            docRef.update("blockedUserIds", FieldValue.arrayRemove(targetUid), "updatedAt", FieldValue.serverTimestamp()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }
}
