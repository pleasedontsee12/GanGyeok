package com.example.gangyeok.ui.screens.auth.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gangyeok.ui.home.CleaningQuestCard
import com.google.firebase.auth.FirebaseAuth

@Composable
fun QuestScreen(modifier: Modifier = Modifier, onCameraClick: () -> Unit) {
    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("청소 퀘스트", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
        CleaningQuestCard(questName = "거실 바닥 청소", assignee = FirebaseAuth.getInstance().currentUser?.displayName ?: "나", dDay = "D-2", onCameraClick = onCameraClick)
    }
}