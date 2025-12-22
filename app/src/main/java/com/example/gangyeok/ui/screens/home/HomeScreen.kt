package com.example.gangyeok.ui.screens.auth.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gangyeok.model.UserStatus
import com.example.gangyeok.ui.home.CleaningQuestCard
import com.example.gangyeok.ui.home.LogItem
import com.example.gangyeok.ui.home.RoommateCard
import com.example.gangyeok.ui.home.StatusSelector
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onCameraClick: () -> Unit,
    myName: String,
    myStatusMessage: String,
    onStatusMessageChange: (String) -> Unit,
    myStatus: UserStatus,
    onStatusChange: (UserStatus) -> Unit,
    isLoading: Boolean,
    roommates: List<com.example.gangyeok.model.User>,
    houseId: String,
    houseLogs: List<com.example.gangyeok.model.HouseLog>
) {
    if (isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    var localStatusMessage by remember { mutableStateOf(myStatusMessage) }
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    LaunchedEffect(myStatusMessage) { localStatusMessage = myStatusMessage }

    if (showDialog) {
        ChangeStatusMessageDialog(localStatusMessage, { showDialog = false }) { onStatusMessageChange(it); showDialog = false }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = {
                db.collection("houses").document(houseId).get().addOnSuccessListener {
                    Toast.makeText(context, "초대 코드: ${it.getString("inviteCode")}", Toast.LENGTH_LONG).show()
                }
            }) { Text("📩 초대 코드 확인", fontSize = 12.sp, color = Color.Gray) }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(64.dp), shape = CircleShape, color = Color.LightGray) { Icon(Icons.Default.Person, "Profile", Modifier.padding(8.dp)) }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(myName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(localStatusMessage, color = Color.Gray, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                StatusSelector(myStatus, onStatusChange)
            }
            IconButton({ showDialog = true }) { Icon(Icons.Default.Edit, "Edit", tint = Color.Gray) }
        }

        Spacer(Modifier.height(20.dp))
        Text("우리 집 상태 요약", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (roommates.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("아직 룸메이트가 없어요!", Modifier.padding(16.dp), color = Color(0xFF1565C0), textAlign = TextAlign.Center)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { roommates.forEach { RoommateCard(it.name, it.status) } }
        }

        Spacer(Modifier.height(24.dp))
        CleaningQuestCard("거실 바닥 청소", myName, "D-2", modifier = Modifier, onCameraClick)

        Spacer(Modifier.height(24.dp))

        // 로그 섹션 (HomeComponents에 있는 UI 사용)
        Column(Modifier.fillMaxWidth()) {
            Text("하우스 로그 & 채팅", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            if (houseLogs.isEmpty()) Text("기록이 없어요.", color = Color.Gray)
            else houseLogs.take(3).forEach { LogItem(it); Spacer(Modifier.height(10.dp)) }
        }

        Spacer(Modifier.height(50.dp))
    }
}

@Composable
fun ChangeStatusMessageDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("메시지 변경") }, text = { OutlinedTextField(text, { text = it }) }, confirmButton = { Button({ onConfirm(text) }) { Text("확인") } }, dismissButton = { Button(onDismiss) { Text("취소") } })
}