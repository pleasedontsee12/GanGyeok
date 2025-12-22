package com.example.gangyeok.model

data class House(
    val id: String = "",           // 하우스 고유 ID
    val name: String = "",         // 우리 집 이름
    val inviteCode: String = "",   // 입장 코드
    val managerId: String = "",    // 방장(집 만든 사람) ID
    val members: List<String> = emptyList() // 구성원들의 uid 리스트
)