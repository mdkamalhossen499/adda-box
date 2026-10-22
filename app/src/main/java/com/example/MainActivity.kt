package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.AddaTheme
import com.example.util.AppLanguageState
import com.example.util.LocalStrings
import com.example.util.Strings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      CompositionLocalProvider(LocalStrings provides Strings(AppLanguageState.currentLanguage)) {
        AddaTheme(darkTheme = AppLanguageState.isDarkMode) {
          Surface(modifier = Modifier.fillMaxSize()) {
            val context = LocalContext.current
            val dbId = context.getString(R.string.firestore_database_id)
            val firestore = remember { FirebaseFirestore.getInstance(dbId) }
            val userRepository = remember { UserRepository(firestore) }
            val chatRepository = remember { ChatRepository(firestore) }
            val callRepository = remember { CallRepository(firestore) }

            var authUser by remember { mutableStateOf(Firebase.auth.currentUser) }

            DisposableEffect(Unit) {
              val listener = FirebaseAuth.AuthStateListener { auth ->
                authUser = auth.currentUser
              }
              Firebase.auth.addAuthStateListener(listener)
              onDispose {
                Firebase.auth.removeAuthStateListener(listener)
              }
            }

            if (authUser == null) {
              AuthScreen(
                userRepository = userRepository,
                onAuthSuccess = { user ->
                  authUser = Firebase.auth.currentUser
                }
              )
            } else {
              AppNavigation(
                currentUserId = authUser!!.uid,
                userRepository = userRepository,
                chatRepository = chatRepository,
                callRepository = callRepository,
                onLogout = {
                  authUser = null
                }
              )
            }
          }
        }
      }
    }
  }
}
