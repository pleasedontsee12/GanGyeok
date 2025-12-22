package com.example.gangyeok

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gangyeok.model.HouseLog
import com.example.gangyeok.model.User
import com.example.gangyeok.model.UserStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.*

enum class QuestStatus {
    PENDING,
    WAITING_FOR_APPROVAL,
    VERIFIED
}

data class Quest(
    val id: String = "",
    val title: String = "",
    val assigneeId: String = "",
    val assigneeName: String = "",
    val status: QuestStatus = QuestStatus.PENDING,
    val proofImageUrl: String? = null,
    val approvers: List<String> = emptyList(),
    val isRepeating: Boolean = false,
    val createdAt: Long = 0L,
    val completedAt: Long = 0L
)

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "홈")
    object Quest : Screen("quest", "미션")
    object HouseLog: Screen("houseLog", "수다방")
    object Settings : Screen("settings", "설정")
}

// 색상 팔레트
data class AppColors(
    val background: Color,
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val primary: Color = Color(0xFF1565C0),
    val myChatBubble: Color = Color(0xFFFFEB3B),
    val iconTint: Color = Color.Gray
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GangyeokApp() }
    }
}

@Composable
fun GangyeokApp() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var firebaseUser by remember { mutableStateOf(auth.currentUser) }
    var userProfile by remember { mutableStateOf<User?>(null) }
    var roommates by remember { mutableStateOf<List<User>>(emptyList()) }
    var houseLogs by remember { mutableStateOf<List<HouseLog>>(emptyList()) }

    var activeQuests by remember { mutableStateOf<List<Quest>>(emptyList()) }

    var houseName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }

    var isUploading by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var isDarkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }

    val appColors = if (isDarkMode) {
        AppColors(Color(0xFF121212), Color(0xFF1E1E1E), Color.White, Color.LightGray, Color(0xFFFBC02D), Color.LightGray)
    } else {
        AppColors(Color(0xFFF5F5F5), Color.White, Color.Black, Color.Gray, Color(0xFFFFEB3B), Color.Gray)
    }

    // Auth & Data Listeners
    DisposableEffect(Unit) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            firebaseUser = firebaseAuth.currentUser
            if (firebaseAuth.currentUser == null) {
                userProfile = null; roommates = emptyList(); houseLogs = emptyList(); activeQuests = emptyList()
            }
        }
        auth.addAuthStateListener(authStateListener)
        onDispose { auth.removeAuthStateListener(authStateListener) }
    }

    DisposableEffect(firebaseUser) {
        val user = firebaseUser
        if (user != null) {
            val registration = db.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val newUser = snapshot.toObject(User::class.java)
                        if (userProfile != null && newUser != null && newUser.lastPing > userProfile!!.lastPing) {
                            vibratePhone(context)
                            Toast.makeText(context, "🤫 룸메이트가 콕! 찔렀습니다.", Toast.LENGTH_LONG).show()
                        }
                        userProfile = newUser
                    }
                }
            onDispose { registration.remove() }
        } else { onDispose { } }
    }

    DisposableEffect(userProfile) {
        val houseId = userProfile?.houseId
        if (!houseId.isNullOrBlank()) {
            val houseReg = db.collection("houses").document(houseId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        houseName = snapshot.getString("name") ?: "우리 집"
                        inviteCode = snapshot.getString("inviteCode") ?: ""
                    }
                }
            val roomReg = db.collection("users").whereEqualTo("houseId", houseId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                        roommates = list.filter { it.uid != auth.currentUser?.uid }
                    }
                }
            val logReg = db.collection("houses").document(houseId).collection("logs")
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(50)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        houseLogs = snapshot.documents.mapNotNull { it.toObject(HouseLog::class.java) }
                    }
                }
            val questReg = db.collection("houses").document(houseId).collection("quests")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val allQuests = snapshot.documents.mapNotNull { it.toObject(Quest::class.java) }
                        activeQuests = allQuests.filter { it.status != QuestStatus.VERIFIED }
                    }
                }

            onDispose { roomReg.remove(); logReg.remove(); houseReg.remove(); questReg.remove() }
        } else { onDispose { } }
    }

    fun addLog(content: String, type: String = "SYSTEM", imageUrl: String? = null) {
        val user = userProfile ?: return
        if (user.houseId.isBlank()) return
        val newLog = HouseLog(UUID.randomUUID().toString(), type, content, user.name, user.uid, System.currentTimeMillis(), imageUrl)
        db.collection("houses").document(user.houseId).collection("logs").document(newLog.id).set(newLog)
    }

    fun sendSilentPing(targetUid: String, lastPingTime: Long) {
        val currentTime = System.currentTimeMillis()
        val cooldown = 5000L // 5초

        if (currentTime - lastPingTime < cooldown) {
            Toast.makeText(context, "잠시 후에 다시 찔러주세요! ⏳", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(targetUid).update("lastPing", currentTime)
            .addOnSuccessListener { Toast.makeText(context, "🤫 콕! 찔렀습니다.", Toast.LENGTH_SHORT).show() }
    }

    fun addNewQuest(title: String, assigneeUid: String, assigneeName: String, isRepeating: Boolean) {
        val user = userProfile ?: return
        val houseId = user.houseId

        val newQuest = Quest(
            id = UUID.randomUUID().toString(),
            title = title,
            assigneeId = assigneeUid,
            assigneeName = assigneeName,
            status = QuestStatus.PENDING,
            isRepeating = isRepeating,
            createdAt = System.currentTimeMillis()
        )
        db.collection("houses").document(houseId).collection("quests").document(newQuest.id).set(newQuest)
        val repeatText = if(isRepeating) "(반복)" else "(1회성)"
        addLog("${user.name}님이 새 미션 [$title]$repeatText 을 등록했습니다! 담당: $assigneeName", "SYSTEM")
        Toast.makeText(context, "미션 등록 완료!", Toast.LENGTH_SHORT).show()
    }

    fun getNextAssignee(currentAssigneeId: String): Pair<String, String>? {
        val allMembers = (roommates + (userProfile!!)).sortedBy { it.name }
        if (allMembers.isEmpty()) return null
        val currentIndex = allMembers.indexOfFirst { it.uid == currentAssigneeId }
        val nextIndex = if (currentIndex == -1 || currentIndex == allMembers.lastIndex) 0 else currentIndex + 1
        val nextMember = allMembers[nextIndex]
        return Pair(nextMember.uid, nextMember.name)
    }

    fun completeQuest(questId: String) {
        val user = userProfile ?: return
        scope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    db.collection("houses").document(user.houseId).collection("quests").document(questId)
                        .update(mapOf("status" to QuestStatus.WAITING_FOR_APPROVAL, "completedAt" to System.currentTimeMillis()))

                    val questTitle = activeQuests.find { it.id == questId }?.title ?: "미션"
                    addLog("${user.name}님이 [$questTitle] 완료! 검사해주세요 ✨", "QUEST")
                    Toast.makeText(context, "완료 요청을 보냈습니다!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun approveQuest(quest: Quest) {
        val user = userProfile ?: return
        if (quest.approvers.contains(user.uid)) {
            Toast.makeText(context, "이미 승인하셨습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val totalMembers = roommates.size + 1
        val requiredApprovals = if (totalMembers <= 2) 1 else (totalMembers / 2) + 1
        val newApprovers = quest.approvers + user.uid

        if (newApprovers.size >= requiredApprovals) {
            db.collection("houses").document(user.houseId).collection("quests").document(quest.id)
                .update(mapOf("status" to QuestStatus.VERIFIED, "approvers" to newApprovers))
                .addOnSuccessListener {
                    addLog("${quest.assigneeName}님의 [${quest.title}] 최종 승인 완료! (${newApprovers.size}/$requiredApprovals) 🎉", "SYSTEM")
                    if (quest.isRepeating) {
                        val next = getNextAssignee(quest.assigneeId)
                        if (next != null) {
                            val (nextUid, nextName) = next
                            val nextQuest = Quest(
                                id = UUID.randomUUID().toString(),
                                title = quest.title,
                                assigneeId = nextUid,
                                assigneeName = nextName,
                                status = QuestStatus.PENDING,
                                isRepeating = true,
                                createdAt = System.currentTimeMillis()
                            )
                            db.collection("houses").document(user.houseId).collection("quests").document(nextQuest.id).set(nextQuest)
                            addLog("🔄 반복 미션: 다음 [${quest.title}] 당번은 $nextName 님입니다!", "SYSTEM")
                        }
                    }
                }
        } else {
            db.collection("houses").document(user.houseId).collection("quests").document(quest.id).update("approvers", newApprovers)
            val currentCount = newApprovers.size
            Toast.makeText(context, "승인 확인! ($currentCount/$requiredApprovals 명)", Toast.LENGTH_SHORT).show()
            addLog("${user.name}님이 [${quest.title}] 청소를 확인했습니다. ($currentCount/$requiredApprovals)", "SYSTEM")
        }
    }

    fun updateProfileImage(uri: Uri) {
        val user = userProfile ?: return
        isUploading = true
        scope.launch(Dispatchers.IO) {
            val base64 = uriToBase64(context, uri)
            withContext(Dispatchers.Main) {
                db.collection("users").document(user.uid).update("profileImage", base64)
                isUploading = false
            }
        }
    }

    fun leaveHouse() {
        val user = userProfile ?: return
        if (user.houseId.isNotBlank()) {
            val batch = db.batch()
            batch.update(db.collection("users").document(user.uid), "houseId", "")
            batch.update(db.collection("houses").document(user.houseId), "members", FieldValue.arrayRemove(user.uid))
            batch.commit().addOnSuccessListener { Toast.makeText(context, "하우스를 떠났습니다.", Toast.LENGTH_SHORT).show() }
        }
    }

    if (firebaseUser == null) {
        var authState by remember { mutableStateOf("LOGIN") }

        when (authState) {
            "LOGIN" -> {
                LoginScreen(
                    onNavigateToSignUp = { authState = "SIGNUP" },
                    onNavigateToFindAccount = { authState = "FIND_ACCOUNT" }
                )
            }
            "SIGNUP" -> {
                SignUpScreen(
                    onNavigateToLogin = { authState = "LOGIN" },
                    onSignUpSuccess = { authState = "LOGIN" }
                )
            }
            "FIND_ACCOUNT" -> {
                FindAccountScreen(
                    onNavigateToLogin = { authState = "LOGIN" }
                )
            }
        }
    } else {
        if (userProfile == null) {
            Box(Modifier.fillMaxSize().background(appColors.background), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            if (userProfile!!.houseId.isBlank()) {
                HouseSetupScreen(onHouseJoined = { })
            } else {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
                val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) updateProfileImage(it) }

                Scaffold(
                    containerColor = appColors.background,
                    bottomBar = { BottomNavigationBar(currentScreen, isDarkMode, appColors) { currentScreen = it } }
                ) { innerPadding ->
                    if (isUploading) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.5f)), Alignment.Center) { CircularProgressIndicator(color = Color.White) }
                    } else {
                        val totalMembers = roommates.size + 1
                        val approvalThreshold = if (totalMembers <= 2) 1 else (totalMembers / 2) + 1

                        when (currentScreen) {
                            is Screen.Home -> HomeScreen(
                                modifier = Modifier.padding(innerPadding),
                                myName = userProfile!!.name,
                                myStatusMessage = userProfile!!.statusMessage,
                                myProfileImage = userProfile!!.profileImage,
                                onStatusMessageChange = { msg -> db.collection("users").document(firebaseUser!!.uid).update("statusMessage", msg) },
                                myStatus = userProfile!!.status,
                                onStatusChange = { status ->
                                    db.collection("users").document(firebaseUser!!.uid).update("status", status)
                                },
                                onProfileClick = { galleryLauncher.launch("image/*") },
                                isLoading = false,
                                roommates = roommates,
                                houseName = houseName,
                                inviteCode = inviteCode,
                                houseLogs = houseLogs,
                                activeQuests = activeQuests,
                                currentUserId = userProfile!!.uid,
                                approvalThreshold = approvalThreshold,
                                onApproveQuest = { q -> approveQuest(q) },
                                onCompleteQuest = { qId -> completeQuest(qId) },
                                onPing = { targetUid, lastPing -> sendSilentPing(targetUid, lastPing) },
                                onUpdateHouseName = { name -> if(userProfile!!.houseId.isNotBlank()) db.collection("houses").document(userProfile!!.houseId).update("name", name) },
                                appColors = appColors
                            )
                            is Screen.Quest -> QuestScreen(
                                modifier = Modifier.padding(innerPadding),
                                activeQuests = activeQuests,
                                currentUserId = userProfile!!.uid,
                                allMembers = listOf(userProfile!!) + roommates,
                                approvalThreshold = approvalThreshold,
                                onCompleteQuest = { qId -> completeQuest(qId) },
                                onApproveClick = { q -> approveQuest(q) },
                                onAddNewQuest = { title, uid, name, repeat -> addNewQuest(title, uid, name, repeat) },
                                colors = appColors
                            )
                            is Screen.HouseLog -> HouseLogScreen(Modifier.padding(innerPadding), houseLogs, userProfile!!.uid, { msg -> addLog(msg, "CHAT") }, appColors)
                            is Screen.Settings -> SettingsScreen(
                                Modifier.padding(innerPadding),
                                isDarkMode,
                                { newMode -> isDarkMode = newMode; prefs.edit().putBoolean("dark_mode", newMode).apply() },
                                { auth.signOut() },
                                { leaveHouse() },
                                appColors
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit,
    onNavigateToFindAccount: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5F5)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "간격", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1565C0))
            Spacer(Modifier.height(16.dp))
            Text(text = "우리 집 룸메이트 라이프", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(48.dp))

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = email, onValueChange = { email = it }, label = { Text("이메일") },
                        leadingIcon = { Icon(Icons.Default.Email, null) }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF1565C0), focusedLabelColor = Color(0xFF1565C0))
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password, onValueChange = { password = it }, label = { Text("비밀번호") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) }, modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(), singleLine = true, shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF1565C0), focusedLabelColor = Color(0xFF1565C0))
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF1565C0))
            } else {
                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            isLoading = true
                            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                                if (!it.isSuccessful) { isLoading = false; Toast.makeText(context, "로그인 실패: 정보를 확인해주세요.", Toast.LENGTH_SHORT).show() }
                            }
                        } else { Toast.makeText(context, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show() }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) { Text("로그인하기", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onNavigateToFindAccount) {
                        Text("비밀번호 찾기", color = Color.Gray)
                    }
                    Text("|", color = Color.LightGray, modifier = Modifier.padding(horizontal = 8.dp))
                    TextButton(onClick = onNavigateToSignUp) {
                        Text("회원가입", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


@Composable
fun SignUpScreen(onNavigateToLogin: () -> Unit, onSignUpSuccess: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5F5)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Text(text = "회원가입", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
            Spacer(Modifier.height(8.dp))
            Text("함께하는 즐거움, 간격에서 시작하세요!", color = Color.Gray, fontSize = 14.sp)
            Spacer(Modifier.height(32.dp))

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = email, onValueChange = { email = it }, label = { Text("이메일") },
                        leadingIcon = { Icon(Icons.Default.Email, null) }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF1565C0))
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name, onValueChange = { name = it }, label = { Text("이름 (닉네임)") },
                        leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF1565C0))
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password, onValueChange = { password = it }, label = { Text("비밀번호 (6자리 이상)") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) }, modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF1565C0))
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF1565C0))
            } else {
                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank() && name.isNotBlank()) {
                            isLoading = true
                            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val uid = task.result?.user?.uid ?: ""
                                    val db = FirebaseFirestore.getInstance()
                                    task.result?.user?.updateProfile(userProfileChangeRequest { displayName = name })
                                    val newUser = hashMapOf("uid" to uid, "name" to name, "email" to email, "status" to "AVAILABLE", "statusMessage" to "반가워요!", "houseId" to "", "lastPing" to 0L)
                                    db.collection("users").document(uid).set(newUser).addOnCompleteListener { isLoading = false; onSignUpSuccess() }
                                } else { isLoading = false; Toast.makeText(context, "오류: ${task.exception?.message}", Toast.LENGTH_SHORT).show() }
                            }
                        } else { Toast.makeText(context, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show() }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) { Text("가입 완료", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onNavigateToLogin) { Text("이미 계정이 있으신가요? 로그인", color = Color.Gray) }
            }
        }
    }
}

@Composable
fun HouseSetupScreen(onHouseJoined: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var setupState by remember { mutableStateOf("CHOICE") }
    var inputName by remember { mutableStateOf("") }
    var inputCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5F5)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Home, null, tint = Color(0xFF1565C0), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("하우스 시작하기", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            Text("아직 소속된 하우스가 없네요!", color = Color.Gray)
            Spacer(Modifier.height(32.dp))

            when (setupState) {
                "CHOICE" -> {
                    SetupOptionCard(Icons.Default.Add, "새 하우스 만들기", "내가 방장이 되어 룸메이트를 초대합니다.") { setupState = "CREATE" }
                    Spacer(Modifier.height(16.dp))
                    SetupOptionCard(Icons.AutoMirrored.Filled.ExitToApp, "초대 코드로 입장", "룸메이트에게 받은 코드를 입력합니다.") { setupState = "JOIN" }
                }
                "CREATE" -> {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(24.dp)) {
                            Text("하우스 이름 짓기", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(value = inputName, onValueChange = { inputName = it }, label = { Text("우리 집 이름 (예: 행복기숙사)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(24.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { setupState = "CHOICE" }) { Text("취소", color = Color.Gray) }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (inputName.isNotBlank() && user != null) {
                                            isLoading = true
                                            val newHouseId = UUID.randomUUID().toString()
                                            val inviteCode = UUID.randomUUID().toString().substring(0, 6).uppercase()
                                            val newHouse = hashMapOf("id" to newHouseId, "name" to inputName, "inviteCode" to inviteCode, "members" to listOf(user.uid), "createdAt" to System.currentTimeMillis())
                                            val batch = db.batch()
                                            batch.set(db.collection("houses").document(newHouseId), newHouse)
                                            batch.update(db.collection("users").document(user.uid), "houseId", newHouseId)
                                            batch.commit().addOnSuccessListener { isLoading = false; onHouseJoined() }
                                        }
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                                ) { Text("만들기") }
                            }
                        }
                    }
                }
                "JOIN" -> {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(24.dp)) {
                            Text("초대 코드 입력", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(value = inputCode, onValueChange = { inputCode = it.uppercase() }, label = { Text("코드 6자리") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(24.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { setupState = "CHOICE" }) { Text("취소", color = Color.Gray) }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (inputCode.isNotBlank() && user != null) {
                                            isLoading = true
                                            db.collection("houses").whereEqualTo("inviteCode", inputCode).get().addOnSuccessListener { snapshot ->
                                                if (!snapshot.isEmpty) {
                                                    val houseDoc = snapshot.documents[0]
                                                    val houseId = houseDoc.id
                                                    val batch = db.batch()
                                                    batch.update(db.collection("houses").document(houseId), "members", FieldValue.arrayUnion(user.uid))
                                                    batch.update(db.collection("users").document(user.uid), "houseId", houseId)
                                                    batch.commit().addOnSuccessListener { isLoading = false; onHouseJoined() }
                                                } else { isLoading = false; Toast.makeText(context, "잘못된 초대 코드입니다.", Toast.LENGTH_SHORT).show() }
                                            }
                                        }
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                                ) { Text("입장하기") }
                            }
                        }
                    }
                }
            }
            if (isLoading) { Spacer(Modifier.height(32.dp)); CircularProgressIndicator(color = Color(0xFF1565C0)) }
        }
    }
}

@Composable
fun SetupOptionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(100.dp)) {
        Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFFE3F2FD), shape = CircleShape, modifier = Modifier.size(56.dp)) { Icon(icon, null, tint = Color(0xFF1565C0), modifier = Modifier.padding(14.dp)) }
            Spacer(Modifier.width(16.dp))
            Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black); Text(desc, fontSize = 12.sp, color = Color.Gray) }
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray)
        }
    }
}

@Composable
fun QuestScreen(
    modifier: Modifier = Modifier,
    activeQuests: List<Quest>,
    currentUserId: String,
    allMembers: List<User>,
    approvalThreshold: Int,
    onCompleteQuest: (String) -> Unit,
    onApproveClick: (Quest) -> Unit,
    onAddNewQuest: (String, String, String, Boolean) -> Unit,
    colors: AppColors
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newQuestTitle by remember { mutableStateOf("") }
    var selectedMember by remember { mutableStateOf(allMembers.firstOrNull { it.uid == currentUserId } ?: allMembers.firstOrNull()) }
    var isRepeat by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    if (showAddDialog && selectedMember != null) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("새 미션 추가", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newQuestTitle,
                        onValueChange = { newQuestTitle = it },
                        label = { Text("미션 이름 (예: 설거지)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary, focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("누가 하나요?", fontSize = 14.sp, color = colors.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).clickable { isExpanded = true }.padding(12.dp)) {
                        Text(selectedMember!!.name, color = colors.textPrimary)
                        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                            allMembers.forEach { member -> DropdownMenuItem(text = { Text(member.name) }, onClick = { selectedMember = member; isExpanded = false }) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isRepeat, onCheckedChange = { isRepeat = it })
                        Text("반복 (완료 시 다음 사람에게 넘기기)", fontSize = 14.sp, color = colors.textPrimary)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newQuestTitle.isNotBlank()) {
                        onAddNewQuest(newQuestTitle, selectedMember!!.uid, selectedMember!!.name, isRepeat)
                        newQuestTitle = ""; isRepeat = false; showAddDialog = false
                    }
                }) { Text("등록") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("취소") } },
            containerColor = colors.cardBackground, titleContentColor = colors.textPrimary
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("진행 중인 미션 🔥", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Add", tint = colors.primary, modifier = Modifier.size(32.dp)) }
        }

        if (activeQuests.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = colors.cardBackground.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.2f))) {
                Text("지금은 평화롭네요 😌\n(+) 버튼을 눌러 집안일을 등록해보세요!", Modifier.padding(24.dp).fillMaxWidth(), color = colors.textSecondary, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(activeQuests) { quest ->
                    CleaningQuestCard(
                        quest = quest,
                        currentUserId = currentUserId,
                        approvalThreshold = approvalThreshold,
                        onCompleteClick = { onCompleteQuest(quest.id) },
                        onApproveClick = { onApproveClick(quest) },
                        colors = colors
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    myName: String,
    myStatusMessage: String,
    myProfileImage: String?,
    onStatusMessageChange: (String) -> Unit,
    myStatus: UserStatus,
    onStatusChange: (UserStatus) -> Unit,
    onProfileClick: () -> Unit,
    isLoading: Boolean,
    roommates: List<User>,
    houseName: String,
    inviteCode: String,
    houseLogs: List<HouseLog>,
    activeQuests: List<Quest>,
    currentUserId: String,
    approvalThreshold: Int,
    onApproveQuest: (Quest) -> Unit,
    onCompleteQuest: (String) -> Unit,
    onPing: (String, Long) -> Unit,
    onUpdateHouseName: (String) -> Unit,
    appColors: AppColors
) {
    if (isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }

    var showStatusDialog by remember { mutableStateOf(false) }
    var showHouseNameDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    if (showStatusDialog) ChangeTextDialog(myStatusMessage, "상태 메시지 변경", { showStatusDialog = false }) { onStatusMessageChange(it); showStatusDialog = false }
    if (showHouseNameDialog) ChangeTextDialog(houseName, "하우스 이름 변경", { showHouseNameDialog = false }) { onUpdateHouseName(it); showHouseNameDialog = false }

    Column(modifier = modifier.fillMaxSize().background(appColors.background).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("반가워요, $myName 님! 👋", fontSize = 14.sp, color = appColors.textSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(houseName, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = appColors.textPrimary)
                    IconButton(onClick = { showHouseNameDialog = true }, modifier = Modifier.size(28.dp).padding(start = 4.dp)) { Icon(Icons.Default.Edit, "Rename", tint = appColors.textSecondary, modifier = Modifier.size(16.dp)) }
                }
            }
            Surface(color = appColors.cardBackground, shape = RoundedCornerShape(50), border = BorderStroke(1.dp, appColors.textSecondary.copy(alpha = 0.2f)), modifier = Modifier.clickable { clipboardManager.setText(AnnotatedString(inviteCode)); Toast.makeText(context, "초대코드 복사됨", Toast.LENGTH_SHORT).show() }) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, null, tint = appColors.primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(inviteCode, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        Card(colors = CardDefaults.cardColors(containerColor = appColors.cardBackground), elevation = CardDefaults.cardElevation(4.dp), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Surface(Modifier.size(72.dp).clickable { onProfileClick() }, shape = CircleShape, color = Color.LightGray) {
                        if (myProfileImage != null) AsyncImage(model = myProfileImage, contentDescription = null, contentScale = ContentScale.Crop)
                        else Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.padding(16.dp))
                    }
                    Surface(shape = CircleShape, color = appColors.cardBackground, shadowElevation = 2.dp, modifier = Modifier.align(Alignment.BottomEnd).size(24.dp).border(2.dp, appColors.cardBackground, CircleShape)) { Icon(Icons.Default.PhotoCamera, null, tint = appColors.textSecondary, modifier = Modifier.padding(4.dp)) }
                }
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("나의 상태", fontSize = 12.sp, color = appColors.textSecondary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.Edit, "Msg", tint = appColors.textSecondary, modifier = Modifier.size(16.dp).clickable { showStatusDialog = true })
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(myStatusMessage.ifBlank { "상태 메시지를 입력해주세요" }, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = appColors.textPrimary, maxLines = 1)
                    Spacer(Modifier.height(12.dp))
                    StatusSelector(myStatus, onStatusChange)
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        Text("함께 사는 룸메이트", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
        Spacer(Modifier.height(12.dp))
        if (roommates.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = appColors.cardBackground.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), border = BorderStroke(1.dp, appColors.textSecondary.copy(alpha = 0.2f))) { Text("아직 혼자 살고 있어요 🥲\n초대 코드로 룸메이트를 불러보세요!", Modifier.padding(24.dp).fillMaxWidth(), color = appColors.textSecondary, textAlign = TextAlign.Center) }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { roommates.forEach { user -> RoommateCardWithPing(user, appColors, { onPing(user.uid, user.lastPing) }) } }
        }
        Spacer(Modifier.height(24.dp))

        val mainQuest = activeQuests.lastOrNull()
        CleaningQuestCard(
            quest = mainQuest,
            currentUserId = currentUserId,
            approvalThreshold = approvalThreshold,
            onCompleteClick = { if(mainQuest!=null) onCompleteQuest(mainQuest.id) },
            onApproveClick = { if(mainQuest!=null) onApproveQuest(mainQuest) },
            colors = appColors
        )
        if (activeQuests.size > 1) {
            Text("+ 외 ${activeQuests.size - 1}개의 미션이 더 있습니다 (미션 탭 확인)", fontSize = 12.sp, color = appColors.textSecondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(24.dp))

        Text("최근 소식", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary, modifier = Modifier.padding(bottom = 12.dp))
        if (houseLogs.isEmpty()) {
            Text("아직 기록이 없어요.", color = appColors.textSecondary, fontSize = 14.sp)
        } else {
            houseLogs.take(3).forEach { LogItem(it, currentUserId, appColors); Spacer(Modifier.height(10.dp)) }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun RoommateCardWithPing(user: User, colors: AppColors, onPing: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = colors.cardBackground), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().height(70.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Surface(Modifier.size(40.dp), shape = CircleShape, color = Color.LightGray) {
                    if (user.profileImage != null) AsyncImage(model = user.profileImage, contentDescription = null, contentScale = ContentScale.Crop)
                    else Icon(Icons.Default.Person, null, tint = Color.White)
                }
                val color = when(user.status) { UserStatus.AVAILABLE -> Color.Green; UserStatus.BUSY -> Color.Red; UserStatus.OUT -> Color.Yellow }
                Box(Modifier.size(12.dp).clip(CircleShape).background(color).align(Alignment.BottomEnd))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(user.statusMessage.ifBlank { "-" }, fontSize = 12.sp, color = colors.textSecondary)
            }
            IconButton(onClick = onPing) { Icon(Icons.Outlined.Notifications, "Ping", tint = Color(0xFFFFB74D)) }
        }
    }
}

@Composable
fun CleaningQuestCard(
    quest: Quest?,
    currentUserId: String,
    approvalThreshold: Int,
    onCompleteClick: () -> Unit,
    onApproveClick: () -> Unit,
    colors: AppColors,
    modifier: Modifier = Modifier
) {
    val isDark = colors.background == Color(0xFF121212)

    Column(modifier.fillMaxWidth()) {
        if (quest == null) {
            Card(colors = CardDefaults.cardColors(containerColor = if(isDark) Color(0xFF333333) else Color(0xFF212121)), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("현재 진행 중인 미션이 없습니다 🏖️", color = Color.Gray) }
            }
            return
        }

        val isAssignee = quest.assigneeId == currentUserId
        val isPending = quest.status == QuestStatus.PENDING
        val isWaiting = quest.status == QuestStatus.WAITING_FOR_APPROVAL

        Card(colors = CardDefaults.cardColors(containerColor = if(isDark) Color(0xFF333333) else Color(0xFF212121)), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val statusText = when(quest.status) {
                        QuestStatus.PENDING -> "진행 중 🔥"
                        QuestStatus.WAITING_FOR_APPROVAL -> "검사 대기 ✋"
                        QuestStatus.VERIFIED -> "완료됨 ✅"
                    }
                    val badgeColor = when(quest.status) {
                        QuestStatus.PENDING -> Color(0xFF424242)
                        QuestStatus.WAITING_FOR_APPROVAL -> Color(0xFFF9A825)
                        QuestStatus.VERIFIED -> Color(0xFF2E7D32)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = badgeColor, shape = RoundedCornerShape(8.dp)) { Text(statusText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
                        if (quest.isRepeating) { Spacer(Modifier.width(8.dp)); Icon(Icons.Outlined.Refresh, "Repeat", tint = Color.Gray, modifier = Modifier.size(16.dp)) }
                    }
                    Icon(Icons.Outlined.Notifications, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                }

                Spacer(Modifier.height(16.dp))
                Text(quest.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("당번: ${quest.assigneeName}", color = Color.Gray, fontSize = 14.sp)

                Spacer(Modifier.height(24.dp))

                if (isPending) {
                    if (isAssignee) {
                        Button(onClick = onCompleteClick, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                            Icon(Icons.Outlined.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                            Text("완료 요청하기", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    } else {
                        Text("아직 ${quest.assigneeName}님이 수행 중 입니다.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                } else if (isWaiting) {
                    val approvedByMe = quest.approvers.contains(currentUserId)
                    val currentApprovals = quest.approvers.size

                    Button(
                        onClick = onApproveClick,
                        enabled = !approvedByMe,
                        colors = ButtonDefaults.buttonColors(containerColor = if(approvedByMe) Color.Gray else Color(0xFF66BB6A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (approvedByMe) {
                            Text("승인 완료 ($currentApprovals/$approvalThreshold)", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Text("확인 및 승인하기 ($currentApprovals/$approvalThreshold)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogItem(log: HouseLog, currentUserId: String, colors: AppColors) {
    val isSystem = log.type == "SYSTEM" || log.type == "QUEST"
    val isMe = log.authorId == currentUserId

    if (isSystem) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = if(log.type == "QUEST") Color(0xFFE8F5E9) else Color(0xFFEEEEEE), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔔 ${log.content}", fontSize = 12.sp, color = Color.Black)
                    if (log.imageUrl != null) {
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(model = log.imageUrl, contentDescription = "인증샷", modifier = Modifier.size(150.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    }
                }
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
            if (!isMe) {
                Surface(Modifier.size(28.dp), shape = CircleShape, color = Color.LightGray) {
                    if (log.imageUrl != null) AsyncImage(model = log.imageUrl, contentDescription = null, contentScale = ContentScale.Crop)
                    else Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                }
                Spacer(Modifier.width(8.dp))
            }
            Surface(color = if (isMe) colors.myChatBubble else colors.cardBackground, shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = if(isMe) 0.dp else 12.dp, bottomStart = if(isMe) 12.dp else 0.dp), shadowElevation = 1.dp) {
                Text(log.content, fontSize = 14.sp, modifier = Modifier.padding(10.dp), color = if(isMe) Color.Black else colors.textPrimary)
            }
        }
    }
}

@Composable
fun HouseLogScreen(modifier: Modifier = Modifier, logs: List<HouseLog>, currentUserId: String, onSendMessage: (String) -> Unit, colors: AppColors) {
    var text by remember { mutableStateOf("") }
    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        Text("우리 집 수다방", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.padding(16.dp))
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), reverseLayout = true) {
            items(logs.size) { index -> LogItem(log = logs[index], currentUserId = currentUserId, colors = colors); Spacer(modifier = Modifier.height(12.dp)) }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(colors.cardBackground).border(1.dp, colors.textSecondary.copy(alpha = 0.3f), RoundedCornerShape(32.dp)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text("메시지...", color = colors.textSecondary, fontSize = 14.sp) }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary), maxLines = 1, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) { onSendMessage(text); text = "" } }))
                IconButton(onClick = { if (text.isNotBlank()) { onSendMessage(text); text = "" } }, modifier = Modifier.size(40.dp).background(colors.primary, CircleShape)) { Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, isDarkMode: Boolean, onThemeChange: (Boolean) -> Unit, onLogOutClick: () -> Unit, onLeaveHouse: () -> Unit, colors: AppColors) {
    var showLeaveDialog by remember { mutableStateOf(false) }
    if (showLeaveDialog) {
        AlertDialog(onDismissRequest = { showLeaveDialog = false }, title = { Text("하우스 나가기") }, text = { Text("정말 현재 하우스에서 나가시겠습니까?\n다시 들어오려면 초대 코드가 필요합니다.") }, confirmButton = { TextButton(onClick = { onLeaveHouse(); showLeaveDialog = false }) { Text("나가기", color = Color.Red) } }, dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text("취소") } }, containerColor = colors.cardBackground, titleContentColor = colors.textPrimary, textContentColor = colors.textSecondary)
    }
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("설정", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.padding(bottom = 24.dp))
        Text("계정", fontWeight = FontWeight.Bold, color = colors.textSecondary)
        ListItem(headlineContent = { Text("로그아웃", color = colors.textPrimary) }, trailingContent = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = colors.textPrimary) }, modifier = Modifier.clickable { onLogOutClick() }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
        ListItem(headlineContent = { Text("하우스 나가기", color = Color.Red) }, supportingContent = { Text("현재 집에서 완전히 탈퇴합니다.", fontSize = 12.sp, color = colors.textSecondary) }, trailingContent = { Icon(Icons.Default.Warning, null, tint = Color.Red) }, modifier = Modifier.clickable { showLeaveDialog = true }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
        HorizontalDivider()
        Text("앱 설정", fontWeight = FontWeight.Bold, color = colors.textSecondary, modifier = Modifier.padding(top = 16.dp))
        ListItem(headlineContent = { Text("다크 모드", color = colors.textPrimary) }, trailingContent = { Switch(checked = isDarkMode, onCheckedChange = { onThemeChange(it) }) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
        ListItem(headlineContent = { Text("앱 버전", color = colors.textPrimary) }, trailingContent = { Text("1.0.0", color = colors.textSecondary) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
    }
}

@Composable
fun StatusSelector(current: UserStatus, onChange: (UserStatus) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusDot(UserStatus.AVAILABLE, Color(0xFF4CAF50), current == UserStatus.AVAILABLE) { onChange(it) }
        StatusDot(UserStatus.BUSY, Color(0xFFE57373), current == UserStatus.BUSY) { onChange(it) }
        StatusDot(UserStatus.OUT, Color(0xFFFFB74D), current == UserStatus.OUT) { onChange(it) }
    }
}

@Composable
fun StatusDot(status: UserStatus, color: Color, isSelected: Boolean, onClick: (UserStatus) -> Unit) {
    val modifier = if (isSelected) Modifier.size(24.dp).border(2.dp, Color(0xFFE0E0E0), CircleShape).padding(4.dp) else Modifier.size(12.dp)
    Box(modifier.clip(CircleShape).background(color).clickable { onClick(status) })
}

@Composable
fun ChangeTextDialog(current: String, title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(text, { text = it }, modifier = Modifier.fillMaxWidth()) }, confirmButton = { Button({ onConfirm(text) }) { Text("확인") } }, dismissButton = { Button(onDismiss) { Text("취소") } })
}

@Composable
fun BottomNavigationBar(currentScreen: Screen, isDarkMode: Boolean, appColors: AppColors, onScreenSelected: (Screen) -> Unit) {
    val barContentColor = if(isDarkMode) Color.White else Color.Black
    val indicatorColor = if(isDarkMode) Color(0xFF333333) else Color(0xFFE3F2FD)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp).clip(RoundedCornerShape(32.dp)),
        shadowElevation = 12.dp, color = Color.Transparent
    ) {
        NavigationBar(containerColor = appColors.cardBackground, contentColor = barContentColor, tonalElevation = 0.dp, modifier = Modifier.wrapContentHeight()) {
            val colors = NavigationBarItemDefaults.colors(selectedIconColor = if(isDarkMode) Color.White else Color(0xFF1565C0), selectedTextColor = if(isDarkMode) Color.White else Color(0xFF1565C0), unselectedIconColor = appColors.textSecondary, unselectedTextColor = appColors.textSecondary, indicatorColor = indicatorColor)
            listOf(Screen.Home to Icons.Filled.Home, Screen.Quest to Icons.Filled.Task, Screen.HouseLog to Icons.Filled.DateRange, Screen.Settings to Icons.Filled.Settings).forEach { (screen, icon) ->
                NavigationBarItem(icon = { Icon(icon, contentDescription = screen.title) }, label = { Text(screen.title, fontSize = 10.sp, fontWeight = FontWeight.Bold) }, selected = currentScreen.route == screen.route, onClick = { onScreenSelected(screen) }, colors = colors, alwaysShowLabel = false)
            }
        }
    }
}

fun uriToBase64(context: Context, uri: android.net.Uri): String {
    val inputStream = context.contentResolver.openInputStream(uri)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    val outputStream = ByteArrayOutputStream()
    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
    return "data:image/jpeg;base64," + Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
}

fun vibratePhone(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(500)
    }
}

@Composable
fun FindAccountScreen(onNavigateToLogin: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5F5)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Text("비밀번호 재설정", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
            Spacer(Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = Color(0xFF1565C0), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("비밀번호 찾기", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            Spacer(Modifier.height(16.dp))
            FindPasswordContent()

            Spacer(Modifier.height(48.dp))
            TextButton(onClick = onNavigateToLogin) {
                Text("로그인 화면으로 돌아가기", color = Color.Gray, fontSize = 14.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun FindPasswordContent() {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("가입한 이메일로 재설정 메일을 발송합니다.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        Spacer(Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("이메일") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF1565C0))
                )
                Spacer(Modifier.height(16.dp))
                if (isLoading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF1565C0)) }
                } else {
                    Button(
                        onClick = {
                            if (email.isNotBlank()) {
                                isLoading = true
                                auth.sendPasswordResetEmail(email)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            Toast.makeText(context, "재설정 이메일을 보냈습니다.", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "메일 전송 실패. 이메일을 확인해주세요.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(context, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        Text("인증 메일 보내기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}