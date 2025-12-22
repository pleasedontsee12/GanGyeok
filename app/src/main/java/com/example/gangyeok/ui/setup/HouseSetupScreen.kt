package com.example.gangyeok.ui.setup

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gangyeok.model.House
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

@Composable
fun HouseSetupScreen(
    onHouseJoined: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var houseName by remember { mutableStateOf("") }
    var inviteCodeInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("환영합니다! 👋", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("아직 소속된 하우스가 없네요.", color = Color.Gray)
        Spacer(modifier = Modifier.height(40.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🏠 새 하우스 만들기", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = houseName,
                    onValueChange = { houseName = it },
                    label = { Text("우리 집 이름 (예: 301호)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (houseName.isNotBlank() && currentUser != null) {
                            isLoading = true
                            val newInviteCode = UUID.randomUUID().toString().substring(0, 6).uppercase()
                            val houseId = UUID.randomUUID().toString()

                            val newHouse = House(
                                id = houseId,
                                name = houseName,
                                inviteCode = newInviteCode,
                                managerId = currentUser.uid,
                                members = listOf(currentUser.uid)
                            )

                            db.collection("houses").document(houseId).set(newHouse)
                                .addOnSuccessListener {
                                    db.collection("users").document(currentUser.uid)
                                        .update("houseId", houseId)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            Toast.makeText(context, "하우스 생성 완료! 코드: $newInviteCode", Toast.LENGTH_LONG).show()
                                            onHouseJoined()
                                        }
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("이 이름으로 시작하기")
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
        Text("또는", color = Color.Gray)
        Spacer(modifier = Modifier.height(30.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📩 초대 코드로 입장하기", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = inviteCodeInput,
                    onValueChange = { inviteCodeInput = it.uppercase() },
                    label = { Text("초대 코드 6자리") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (inviteCodeInput.isNotBlank() && currentUser != null) {
                            isLoading = true
                            db.collection("houses")
                                .whereEqualTo("inviteCode", inviteCodeInput)
                                .get()
                                .addOnSuccessListener { documents ->
                                    if (documents.isEmpty) {
                                        isLoading = false
                                        Toast.makeText(context, "잘못된 초대 코드입니다.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val house = documents.documents[0]
                                        val houseId = house.id

                                        db.collection("houses").document(houseId)
                                            .update("members", FieldValue.arrayUnion(currentUser.uid))
                                            .addOnSuccessListener {
                                                db.collection("users").document(currentUser.uid)
                                                    .update("houseId", houseId)
                                                    .addOnSuccessListener {
                                                        isLoading = false
                                                        Toast.makeText(context, "${house.getString("name")}에 입장했습니다!", Toast.LENGTH_SHORT).show()
                                                        onHouseJoined()
                                                    }
                                            }
                                    }
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Text("입장하기")
                }
            }
        }
    }
}