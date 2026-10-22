package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.User
import com.example.data.repository.UserRepository
import com.example.ui.components.UserAvatar
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.util.LocalStrings
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    userRepository: UserRepository,
    onAuthSuccess: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentUserAuth by remember { mutableStateOf(Firebase.auth.currentUser) }
    var existingProfile by remember { mutableStateOf<User?>(null) }
    var isCheckingProfile by remember { mutableStateOf(false) }

    // If already signed in via Firebase, check if profile exists
    LaunchedEffect(currentUserAuth) {
        val user = currentUserAuth
        if (user != null) {
            isCheckingProfile = true
            val profile = userRepository.getUserProfile(user.uid)
            existingProfile = profile
            isCheckingProfile = false
            if (profile != null && profile.username.isNotBlank()) {
                onAuthSuccess(profile)
            }
        }
    }

    if (currentUserAuth != null && (existingProfile == null || existingProfile?.username.isNullOrBlank()) && !isCheckingProfile) {
        // User is signed in with Google, needs initial Profile Onboarding
        ProfileOnboardingView(
            authUid = currentUserAuth!!.uid,
            initialName = currentUserAuth!!.displayName ?: "",
            initialPhoto = currentUserAuth!!.photoUrl?.toString() ?: "",
            userRepository = userRepository,
            onProfileSaved = { savedUser ->
                onAuthSuccess(savedUser)
            }
        )
    } else {
        // Landing / Welcome Sign in screen
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Logo Badge
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(TealPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "আ",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = strings.signInTitle,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = strings.signInSubtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (isCheckingProfile || isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = TealPrimary
                    )
                } else {
                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            signInWithGoogle(
                                context = context,
                                credentialManager = credentialManager,
                                scope = scope,
                                onSuccess = {
                                    isLoading = false
                                    currentUserAuth = Firebase.auth.currentUser
                                },
                                onError = { err ->
                                    isLoading = false
                                    errorMessage = err
                                },
                                onCancel = {
                                    isLoading = false
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = strings.signInWithGoogle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileOnboardingView(
    authUid: String,
    initialName: String,
    initialPhoto: String,
    userRepository: UserRepository,
    onProfileSaved: (User) -> Unit
) {
    val strings = LocalStrings.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var displayName by remember { mutableStateOf(initialName.ifEmpty { "নতুন বন্ধু" }) }
    var username by remember { mutableStateOf("user_${authUid.take(6)}") }
    var bio by remember { mutableStateOf("আড্ডায় যুক্ত হয়েছি!") }
    var selectedPhotoUrl by remember { mutableStateOf(initialPhoto) }
    var isSaving by remember { mutableStateOf(false) }

    // Preset avatars
    val presetAvatars = listOf(
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
        "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = strings.setupProfileTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = strings.setupProfileSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Avatar preview
        UserAvatar(
            name = displayName,
            photoUrl = selectedPhotoUrl,
            size = 96.dp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = strings.chooseAvatar,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(presetAvatars) { avatarUrl ->
                val isSelected = selectedPhotoUrl == avatarUrl
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) TealPrimary else Color.LightGray,
                            shape = CircleShape
                        )
                        .clickable { selectedPhotoUrl = avatarUrl }
                ) {
                    UserAvatar(name = "User", photoUrl = avatarUrl, size = 52.dp)
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Unique User ID with Copy Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ইউজার আইডি (User ID):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = authUid,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(authUid))
                        Toast.makeText(context, strings.userIdCopied, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("copy_user_id_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy User ID",
                        tint = TealPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Name input
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text(strings.displayNameLabel) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("display_name_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Username input
        OutlinedTextField(
            value = username,
            onValueChange = { username = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' } },
            label = { Text(strings.usernameLabel) },
            prefix = { Text("@") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("username_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Bio input
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text(strings.bioLabel) },
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bio_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                isSaving = true
                scope.launch {
                    val result = userRepository.saveUserProfile(
                        displayName = displayName,
                        username = username,
                        bio = bio,
                        photoUrl = selectedPhotoUrl
                    )
                    isSaving = false
                    if (result.isSuccess) {
                        val saved = userRepository.getUserProfile(authUid)
                        if (saved != null) {
                            onProfileSaved(saved)
                        }
                    } else {
                        Toast.makeText(context, "সংরক্ষণ ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = displayName.isNotBlank() && username.isNotBlank() && !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("complete_onboarding_button"),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = strings.completeSetup,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

fun signInWithGoogle(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onError("Google Client ID not found in resources")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onSuccess()
            } else {
                onError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled: ${e.message}", e)
            onCancel()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onError(e.localizedMessage ?: "Sign-in failed")
        }
    }
}

fun signOutUser(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onComplete: () -> Unit
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Error clearing credentials", e)
        } finally {
            onComplete()
        }
    }
}
