package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppLanguage {
    BANGLA,
    ENGLISH
}

class Strings(val language: AppLanguage) {
    val appName: String get() = if (language == AppLanguage.BANGLA) "আড্ডা" else "Adda"
    val appTagline: String get() = if (language == AppLanguage.BANGLA) "সবাই একসাথে, সবসময় আনন্দে" else "Connect, Chat & Call with Friends"

    // Tabs
    val tabChats: String get() = if (language == AppLanguage.BANGLA) "চ্যাট" else "Chats"
    val tabPeople: String get() = if (language == AppLanguage.BANGLA) "মানুষ" else "People"
    val tabCalls: String get() = if (language == AppLanguage.BANGLA) "কল" else "Calls"
    val tabProfile: String get() = if (language == AppLanguage.BANGLA) "প্রোফাইল" else "Profile"

    // Auth & Onboarding
    val signInTitle: String get() = if (language == AppLanguage.BANGLA) "আড্ডায় স্বাগতম" else "Welcome to Adda"
    val signInSubtitle: String get() = if (language == AppLanguage.BANGLA) "বন্ধুদের সাথে কথা বলুন, গ্রুপে আড্ডা দিন এবং কথা বলুন প্রাণ খুলে" else "Join groups, talk, and make voice calls with friends"
    val signInWithGoogle: String get() = if (language == AppLanguage.BANGLA) "গুগল দিয়ে সাইন ইন করুন" else "Sign in with Google"
    val setupProfileTitle: String get() = if (language == AppLanguage.BANGLA) "আপনার প্রোফাইল সাজান" else "Setup Your Profile"
    val setupProfileSubtitle: String get() = if (language == AppLanguage.BANGLA) "আপনার নাম ও পছন্দসই ইউজারনেম দিন" else "Set your display name and unique username"
    val displayNameLabel: String get() = if (language == AppLanguage.BANGLA) "নাম (Display Name)" else "Display Name"
    val usernameLabel: String get() = if (language == AppLanguage.BANGLA) "ইউজারনেম (Username)" else "Username"
    val bioLabel: String get() = if (language == AppLanguage.BANGLA) "বায়ো (Bio)" else "Bio"
    val chooseAvatar: String get() = if (language == AppLanguage.BANGLA) "ছবি নির্বাচন করুন" else "Choose Avatar"
    val completeSetup: String get() = if (language == AppLanguage.BANGLA) "শুরু করুন" else "Get Started"
    val userIdCopied: String get() = if (language == AppLanguage.BANGLA) "ইউজার আইডি কপি করা হয়েছে!" else "User ID copied!"

    // Chats
    val searchPlaceholder: String get() = if (language == AppLanguage.BANGLA) "খুঁজুন..." else "Search..."
    val newGroup: String get() = if (language == AppLanguage.BANGLA) "নতুন গ্রুপ" else "New Group"
    val publicGroupTitle: String get() = if (language == AppLanguage.BANGLA) "সবাই (পাবলিক আড্ডা)" else "Everyone (Public Adda)"
    val publicGroupDesc: String get() = if (language == AppLanguage.BANGLA) "সকল নিবন্ধিত সদস্যের সাধারণ গ্রুপ" else "Default group for all registered users"
    val emptyChats: String get() = if (language == AppLanguage.BANGLA) "কোনো চ্যাট নেই। নতুন গ্রুপ খুলুন বা মানুষদের সাথে বার্তা শুরু করুন!" else "No chats yet. Create a group or start messaging people!"
    val typing: String get() = if (language == AppLanguage.BANGLA) "টাইপ করছেন..." else "typing..."
    val online: String get() = if (language == AppLanguage.BANGLA) "অনলাইন" else "Online"
    val offline: String get() = if (language == AppLanguage.BANGLA) "অফলাইন" else "Offline"
    val reply: String get() = if (language == AppLanguage.BANGLA) "উত্তর দিন" else "Reply"
    val delete: String get() = if (language == AppLanguage.BANGLA) "মুছে ফেলুন" else "Delete"
    val deleteMessageConfirm: String get() = if (language == AppLanguage.BANGLA) "বার্তাটি মুছে ফেলতে চান?" else "Delete this message?"
    val typeMessage: String get() = if (language == AppLanguage.BANGLA) "বার্তা লিখুন..." else "Type a message..."
    val send: String get() = if (language == AppLanguage.BANGLA) "পাঠান" else "Send"
    val activeCallBanner: String get() = if (language == AppLanguage.BANGLA) "গ্রুপ অডিও কল চলছে!" else "Group audio call is active!"
    val joinCall: String get() = if (language == AppLanguage.BANGLA) "কলে যোগ দিন" else "Join Call"

    // People
    val peopleDirectoryTitle: String get() = if (language == AppLanguage.BANGLA) "সকল মানুষ" else "People Directory"
    val messageBtn: String get() = if (language == AppLanguage.BANGLA) "বার্তা" else "Message"
    val callBtn: String get() = if (language == AppLanguage.BANGLA) "অডিও কল" else "Audio Call"
    val blockBtn: String get() = if (language == AppLanguage.BANGLA) "ব্লক করুন" else "Block"
    val unblockBtn: String get() = if (language == AppLanguage.BANGLA) "আনব্লক করুন" else "Unblock"
    val reportBtn: String get() = if (language == AppLanguage.BANGLA) "রিপোর্ট করুন" else "Report"
    val userReported: String get() = if (language == AppLanguage.BANGLA) "ব্যবহারকারীকে রিপোর্ট করা হয়েছে।" else "User has been reported."
    val userBlocked: String get() = if (language == AppLanguage.BANGLA) "ব্যবহারকারীকে ব্লক করা হয়েছে।" else "User has been blocked."

    // Group Management
    val createGroupTitle: String get() = if (language == AppLanguage.BANGLA) "নতুন গ্রুপ তৈরি" else "Create New Group"
    val groupNameLabel: String get() = if (language == AppLanguage.BANGLA) "গ্রুপের নাম" else "Group Name"
    val groupDescLabel: String get() = if (language == AppLanguage.BANGLA) "গ্রুপের বিবরণ" else "Group Description"
    val selectMembers: String get() = if (language == AppLanguage.BANGLA) "সদস্য নির্বাচন করুন" else "Select Members"
    val groupInfoTitle: String get() = if (language == AppLanguage.BANGLA) "গ্রুপের তথ্য" else "Group Info"
    val membersCount: (Int) -> String = { count ->
        if (language == AppLanguage.BANGLA) "$count জন সদস্য" else "$count members"
    }
    val adminBadge: String get() = if (language == AppLanguage.BANGLA) "অ্যাডমিন" else "Admin"
    val removeMember: String get() = if (language == AppLanguage.BANGLA) "সরিয়ে দিন" else "Remove"
    val leaveGroup: String get() = if (language == AppLanguage.BANGLA) "গ্রুপ ত্যাগ করুন" else "Leave Group"

    // Calls
    val callHistoryTitle: String get() = if (language == AppLanguage.BANGLA) "কলের ইতিহাস" else "Call History"
    val activeCallTitle: String get() = if (language == AppLanguage.BANGLA) "চলমান অডিও আড্ডা" else "Active Voice Call"
    val mute: String get() = if (language == AppLanguage.BANGLA) "মিউট" else "Mute"
    val unmute: String get() = if (language == AppLanguage.BANGLA) "আনমিউট" else "Unmute"
    val speaker: String get() = if (language == AppLanguage.BANGLA) "স্পিকার" else "Speaker"
    val endCall: String get() = if (language == AppLanguage.BANGLA) "কল শেষ করুন" else "End Call"
    val incomingCall: String get() = if (language == AppLanguage.BANGLA) "ইনকামিং অডিও কল..." else "Incoming audio call..."
    val acceptCall: String get() = if (language == AppLanguage.BANGLA) "গ্রহণ করুন" else "Accept"
    val declineCall: String get() = if (language == AppLanguage.BANGLA) "প্রত্যাখ্যান" else "Decline"
    val speaking: String get() = if (language == AppLanguage.BANGLA) "কথা বলছেন" else "Speaking"

    // Settings
    val settingsTitle: String get() = if (language == AppLanguage.BANGLA) "সেটিংস" else "Settings"
    val editProfile: String get() = if (language == AppLanguage.BANGLA) "প্রোফাইল সম্পাদনা" else "Edit Profile"
    val languageSetting: String get() = if (language == AppLanguage.BANGLA) "ভাষা (Language)" else "Language (ভাষা)"
    val currentLangLabel: String get() = if (language == AppLanguage.BANGLA) "বাংলা" else "English"
    val darkModeSetting: String get() = if (language == AppLanguage.BANGLA) "ডার্ক মোড" else "Dark Mode"
    val blockedUsers: String get() = if (language == AppLanguage.BANGLA) "ব্লক করা তালিকা" else "Blocked Users"
    val logout: String get() = if (language == AppLanguage.BANGLA) "লগ আউট" else "Logout"
    val logoutConfirm: String get() = if (language == AppLanguage.BANGLA) "আপনি কি নিশ্চিত যে লগ আউট করতে চান?" else "Are you sure you want to log out?"
    val cancel: String get() = if (language == AppLanguage.BANGLA) "বাতিল" else "Cancel"
    val save: String get() = if (language == AppLanguage.BANGLA) "সংরক্ষণ করুন" else "Save"
}

object AppLanguageState {
    var currentLanguage by mutableStateOf(AppLanguage.BANGLA)
    var isDarkMode by mutableStateOf(false)
}

val LocalStrings = compositionLocalOf { Strings(AppLanguage.BANGLA) }
