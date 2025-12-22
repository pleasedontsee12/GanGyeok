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
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(onNavigateToSignUp: () -> Unit) {
    val context = LocalContext.current; val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("로그인", fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(32.dp))
        OutlinedTextField(email, { email = it }, label = { Text("이메일") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Spacer(Modifier.height(16.dp))
        OutlinedTextField(password, { password = it }, label = { Text("비밀번호") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true); Spacer(Modifier.height(24.dp))
        if (isLoading) CircularProgressIndicator() else {
            Button({ if(email.isNotBlank()) { isLoading = true; auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { isLoading = false; if(it.isSuccessful) Toast.makeText(context, "환영합니다!", Toast.LENGTH_SHORT).show() } } }, Modifier.fillMaxWidth()) { Text("로그인하기") }
            Spacer(Modifier.height(8.dp)); Button(onNavigateToSignUp, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Black)) { Text("회원가입") }
        }
    }
}