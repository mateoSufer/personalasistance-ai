package com.kova.app.domain.model

data class UserProfile(
    val name: String = "",
    val goal: String = "",
    val distractionApps: List<String> = listOf(
        "com.zhiliaoapp.musically",
        "com.instagram.android",
        "com.twitter.android",
        "com.facebook.katana",
        "com.snapchat.android",
        "com.google.android.youtube"
    )
)