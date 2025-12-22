package com.example.gangyeok.ui.screens.auth.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gangyeok.ui.home.LogItem


@Composable
fun HouseLogScreen(
    modifier: Modifier = Modifier,
    logs: List<com.example.gangyeok.model.HouseLog>,
    onSendMessage: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    Column(modifier = modifier.fillMaxSize()) {
        Text("우리 집 로그 & 채팅", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), reverseLayout = true) {
            items(logs.size) { index -> LogItem(log = logs[index]); Spacer(modifier = Modifier.height(12.dp)) }
        }
        Surface(shadowElevation = 8.dp, color = Color.White) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(text, { text = it }, placeholder = { Text("메시지...") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { if(text.isNotBlank()) { onSendMessage(text); text = "" } }))
                Spacer(Modifier.width(8.dp))
                IconButton({ if(text.isNotBlank()) { onSendMessage(text); text = "" } }, Modifier.background(Color(0xFF1565C0), CircleShape)) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White) }

            }
        }
    }
}
