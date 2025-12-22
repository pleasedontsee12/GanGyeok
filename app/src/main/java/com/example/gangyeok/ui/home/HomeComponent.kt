package com.example.gangyeok.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gangyeok.model.UserStatus
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Notifications
import com.example.gangyeok.model.HouseLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 🎨 디자인에서 추출한 컬러 팔레트
val GreenStatus = Color(0xFF4CAF50) // 대화 환영
val RedStatus = Color(0xFFE57373)   // 방해 금지
val YellowStatus = Color(0xFFFFB74D) // 외출 중
val BgBlueTint = Color(0xFFE8EAF6)  // 집중 모드 배경 (연한 파랑)
val BgOrangeTint = Color(0xFFFFF3E0) // 에너지 방전 배경 (연한 주황)
val TextGray = Color(0xFF757575)

@Composable
fun MyStatusCard(
    currentStatus: UserStatus,
    onStatusChange: (UserStatus) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 1. 프로필 및 상태 선택 점
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 이름 및 현재 상태 텍스트
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "나", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = when (currentStatus) {
                            UserStatus.AVAILABLE -> "대화 환영"
                            UserStatus.BUSY -> "방해 금지"
                            UserStatus.OUT -> "외출 중"
                        },
                        fontSize = 14.sp,
                        color = TextGray
                    )
                }

                // 상태 선택 토글
                StatusSelector(currentStatus, onStatusChange)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. 퀵 액션 버튼
            Row(modifier = Modifier.fillMaxWidth()) {
                // 집중 모드 버튼
                StatusActionButton(
                    text = "⚡ 집중 모드",
                    backgroundColor = BgBlueTint,
                    textColor = Color(0xFF3F51B5),
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange(UserStatus.BUSY) } // 클릭 시 상태 변경
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 에너지 방전 버튼
                StatusActionButton(
                    text = "☕ 에너지 방전",
                    backgroundColor = BgOrangeTint,
                    textColor = Color(0xFFE65100),
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange(UserStatus.BUSY) }
                )
            }
        }
    }
}

@Composable
fun StatusSelector(current: UserStatus, onChange: (UserStatus) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusDot(UserStatus.AVAILABLE, GreenStatus, current == UserStatus.AVAILABLE) { onChange(it) }
        StatusDot(UserStatus.BUSY, RedStatus, current == UserStatus.BUSY) { onChange(it) }
        StatusDot(UserStatus.OUT, YellowStatus, current == UserStatus.OUT) { onChange(it) }
    }
}

@Composable
fun StatusDot(status: UserStatus, color: Color, isSelected: Boolean, onClick: (UserStatus) -> Unit) {
    val modifier = if (isSelected) {
        Modifier
            .size(24.dp)
            .border(2.dp, Color(0xFFE0E0E0), CircleShape)
            .padding(4.dp)
    } else {
        Modifier.size(12.dp)
    }

    Box(
        modifier = Modifier
            .then(modifier)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick(status) }
    )
}

@Composable
fun StatusActionButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = text, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun RoommateCard(
    name: String,
    status: UserStatus,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .height(80.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )
                val statusColor = when (status) {
                    UserStatus.AVAILABLE -> GreenStatus
                    UserStatus.BUSY -> RedStatus
                    UserStatus.OUT -> YellowStatus
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                        .border(2.dp, Color.White, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = when (status) {
                        UserStatus.AVAILABLE -> "대화 환영"
                        UserStatus.BUSY -> "방해 금지"
                        UserStatus.OUT -> "외출 중"
                    },
                    fontSize = 12.sp,
                    color = TextGray
                )
            }

            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Silent Ping",
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CleaningQuestCard(
    questName: String,
    assignee: String,
    dDay: String,
    modifier: Modifier = Modifier,
    onCameraClick: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "이번 주 미션",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF212121)), // 🌑 진한 검정/회색
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // D-Day 뱃지
                    Surface(
                        color = Color(0xFF424242),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = dDay,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.Camera,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = questName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "오늘의 당번: $assignee",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onCameraClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "인증샷 찍고 완료하기",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HouseLogSection(
    logs: List<HouseLog>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "하우스 로그 & 채팅",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (logs.isEmpty()) {
            Text("아직 기록된 로그가 없어요.", color = Color.Gray, fontSize = 14.sp)
        } else {
            logs.take(3).forEach { log ->
                LogItem(log = log)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun LogItem(log: HouseLog) {
    val isSystem = log.type == "SYSTEM"

    val timeString = SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(log.timestamp))

    if (isSystem) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                color = Color(0xFFEEEEEE),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "🔔 ${log.content}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            // 이름과 시간
            Column {
                Text(text = log.authorName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Surface(
                    color = Color(0xFFE3F2FD), // 연한 파랑 말풍선
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 12.dp)
                ) {
                    Text(
                        text = log.content,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = timeString, fontSize = 10.sp, color = Color.LightGray)
        }
    }
}