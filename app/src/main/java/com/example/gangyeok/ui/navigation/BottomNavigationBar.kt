package com.example.gangyeok.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.gangyeok.Screen

@Composable
fun BottomNavigationBar(currentScreen: com.example.gangyeok.Screen, onScreenSelected: (com.example.gangyeok.Screen) -> Unit) {
    NavigationBar {
        NavigationBarItem(icon = { Icon(Icons.Filled.Home, "Home") }, label = { Text("홈") }, selected = currentScreen.route == com.example.gangyeok.Screen.Home.route, onClick = { onScreenSelected(
            com.example.gangyeok.Screen.Home) })
        NavigationBarItem(icon = { Icon(Icons.Filled.Task, "Quest") }, label = { Text("퀘스트") }, selected = currentScreen.route == com.example.gangyeok.Screen.Quest.route, onClick = { onScreenSelected(
            com.example.gangyeok.Screen.Quest) })
        NavigationBarItem(icon = { Icon(Icons.Filled.DateRange, "Log") }, label = { Text("수다방") }, selected = currentScreen.route == com.example.gangyeok.Screen.HouseLog.route, onClick = { onScreenSelected(
            com.example.gangyeok.Screen.HouseLog) })
        NavigationBarItem(icon = { Icon(Icons.Filled.Settings, "Settings") }, label = { Text("설정") }, selected = currentScreen.route == com.example.gangyeok.Screen.Settings.route, onClick = { onScreenSelected(
            Screen.Settings) })
    }
}