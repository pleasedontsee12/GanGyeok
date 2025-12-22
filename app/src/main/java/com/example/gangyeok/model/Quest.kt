package com.example.gangyeok.model

enum class QuestStatus {
    PENDING,
    WAITING_FOR_APPROVAL,
    VERIFIED
}

// 퀘스트 데이터 모델
data class Quest(
    val id: String = "",
    val title: String = "",
    val assigneeId: String = "",     // 당번 UID
    val assigneeName: String = "",   // 당번 이름
    val status: QuestStatus = QuestStatus.PENDING,
    val proofImageUrl: String? = null, // 인증샷 URL
    val approvers: List<String> = emptyList(), // 승인한 룸메이트 UIDs
    val createdAt: Long = 0L,
    val completedAt: Long = 0L
)