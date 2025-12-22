package com.example.gangyeok.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

class GangyeokWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val dataString = prefs[stringPreferencesKey("widget_data")] ?: ""

            val users = if (dataString.isBlank()) emptyList() else dataString.split(",").map {
                val parts = it.split("|")
                if (parts.size >= 2) parts[0] to parts[1] else "알 수 없음" to "AVAILABLE"
            }

            GangyeokWidgetContent(users)
        }
    }
}

class GangyeokWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GangyeokWidget()
}

@SuppressLint("RestrictedApi")
@Composable
fun GangyeokWidgetContent(users: List<Pair<String, String>>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "우리 집 상태",
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ColorProvider(Color.Black))
        )
        Spacer(modifier = GlanceModifier.height(12.dp))

        if (users.isEmpty()) {
            Text("로그인 필요", style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color.Gray)))
        } else {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                users.forEach { (name, status) ->
                    WidgetStatusItem(name, status)

                    if (name != users.last().first) {
                        Spacer(modifier = GlanceModifier.width(8.dp))
                    }
                }
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun RowScope.WidgetStatusItem(name: String, status: String) {
    val (color, statusText) = when (status) {
        "BUSY" -> Color(0xFFE57373) to "방해 금지"
        "OUT" -> Color(0xFFFFB74D) to "외출 중"
        else -> Color(0xFF4CAF50) to "대화 환영"
    }

    Column(
        modifier = GlanceModifier
            .background(Color(0xFFF5F5F5))
            .padding(8.dp)
            .defaultWeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = GlanceModifier.size(16.dp).background(color)) {}
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(text = name, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ColorProvider(Color.Black)))
        Text(text = statusText, style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color.Gray)))
    }
}