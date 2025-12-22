// ui/navigation/Screen.kt
package com.example.gangyeok.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "홈")
    object Status : Screen("status", "현황")
    object Quest : Screen("quest", "퀘스트")
    object Settings : Screen("settings", "설정")
    object HouseLog: Screen("houseLog", "로그")
}