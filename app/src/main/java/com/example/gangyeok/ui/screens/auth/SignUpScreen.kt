package com.example.gangyeok.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gangyeok.model.UserStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SignUpScreen(onNavigateToLogin: () -> Unit) {
    val context = LocalContext.current; val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("회원가입", fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(32.dp))
        OutlinedTextField(email, { email = it }, label = { Text("이메일") }, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(16.dp))
        OutlinedTextField(name, { name = it }, label = { Text("이름") }, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(16.dp))
        OutlinedTextField(password, { password = it }, label = { Text("비밀번호") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation()); Spacer(Modifier.height(24.dp))
        if (isLoading) CircularProgressIndicator() else {
            Button({ if(email.isNotBlank()) { isLoading = true; auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    val uid = task.result?.user?.uid ?: ""; val db = FirebaseFirestore.getInstance()
                    task.result?.user?.updateProfile(userProfileChangeRequest { displayName = name })
                    db.collection("users").document(uid).set(com.example.gangyeok.model.User(uid=uid, name=name, email=email, status=UserStatus.AVAILABLE, statusMessage="안녕하세요!")).addOnCompleteListener { isLoading = false; if(it.isSuccessful) Toast.makeText(context, "가입 완료!", Toast.LENGTH_SHORT).show() }
                } else isLoading = false
            } } }, Modifier.fillMaxWidth()) { Text("가입하기") }
            Spacer(Modifier.height(8.dp)); Button(onNavigateToLogin, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Black)) { Text("로그인으로 돌아가기") }
        }
    }
}