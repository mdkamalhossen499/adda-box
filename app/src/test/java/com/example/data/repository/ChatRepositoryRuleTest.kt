package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createGroup_authenticatedUser_createsSuccessfully() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = ChatRepository(firestore)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createGroup("Adda Test Group", "Test Description", "", emptyList())
    }
    assertTrue(result.isSuccess)
    val groupId = result.getOrThrow()
    assertTrue(groupId.isNotEmpty())
  }

  @Test
  fun observeUserGroups_authenticatedUser_returnsGroup() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = ChatRepository(firestore)
    val groupId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createGroup("Public Hangout", "Test", "", emptyList()).getOrThrow()
    }

    val groups = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeUserGroups().first { list -> list.any { it.id == groupId } }
    }
    assertTrue(groups.any { it.id == groupId })
  }

  @Test
  fun unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = ChatRepository(firestore)

    var threw = false
    try {
      repository.createGroup("Unauthorized", "", "", emptyList())
    } catch (e: IllegalStateException) {
      threw = true
    }
    assertTrue(threw)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
