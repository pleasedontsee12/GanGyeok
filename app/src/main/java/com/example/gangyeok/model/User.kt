package com.example.gangyeok.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val houseId: String = "",
    val status: UserStatus = UserStatus.AVAILABLE,
    val statusMessage: String = "",
    val fcmToken: String = "",
    val lastPing: Long = 0L,
    val profileImage: String? = null
)

enum class UserStatus {
    AVAILABLE,
    BUSY,
    OUT
}