package com.example.gangyeok.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class HouseLog(
    val id: String = "",
    val type: String = "SYSTEM",
    val content: String = "",
    val authorName: String = "",
    val authorId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null
) {
    constructor() : this("", "SYSTEM", "", "", "", 0L, null)
}

enum class LogType {
    SYSTEM,
    CHAT,
    SOS
}

data class Comment(
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)