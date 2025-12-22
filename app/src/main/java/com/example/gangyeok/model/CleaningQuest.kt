package com.example.gangyeok.model

data class CleaningQuest(
    val id: String = "",
    val assigneeId: String = "",
    val dueDate: Long = 0,
    val isCompleted: Boolean = false,
    val proofImageUrl: String? = null
)