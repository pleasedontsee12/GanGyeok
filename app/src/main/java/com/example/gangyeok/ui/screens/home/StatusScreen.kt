package com.example.gangyeok.ui.screens.auth.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gangyeok.model.UserStatus
import com.example.gangyeok.ui.home.MyStatusCard
import com.example.gangyeok.ui.home.RoommateCard

@Composable
fun StatusScreen(modifier: Modifier = Modifier, myStatus: UserStatus, onStatusChange: (UserStatus) -> Unit, roommates: List<com.example.gangyeok.model.User>) {
    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("나와 룸메이트 현황", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
        MyStatusCard(currentStatus = myStatus, onStatusChange = onStatusChange)
        Spacer(modifier = Modifier.height(20.dp))
        if (roommates.isEmpty()) Text("아직 룸메이트가 없습니다.", color = Color.Gray)
        else roommates.forEach { RoommateCard(name = it.name, status = it.status); Spacer(modifier = Modifier.height(12.dp)) }
    }
}