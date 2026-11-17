package com.oudi.manager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HomeBg = Color(0xFF080D18)
private val HomeText = Color(0xFFF4F6FA)
private val HomeMuted = Color(0xFFB9C2D2)
private val HomeGold = Color(0xFFE8BD78)

class OudiHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )
        setContent { OudiHome { packageName -> launchApp(packageName) } }
    }

    private fun launchApp(packageName: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) startActivity(launch)
        else startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER))
    }
}

private data class TvApp(val name: String, val mark: String, val tint: Color, val pkg: String)

@Composable
private fun OudiHome(onAppClick: (String) -> Unit) {
    var clock by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
    var date by remember { mutableStateOf(SimpleDateFormat("EEEE، d MMMM", Locale("ar")).format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            date = SimpleDateFormat("EEEE، d MMMM", Locale("ar")).format(Date())
            delay(15000)
        }
    }
    val apps = listOf(
        TvApp("YouTube", "▶", Color(0xFFFF3434), "com.google.android.youtube.tv"),
        TvApp("Netflix", "N", Color(0xFFE50914), "com.netflix.ninja"),
        TvApp("Prime Video", "prime", Color(0xFF168ED0), "com.amazon.amazonvideo.livingroom"),
        TvApp("Disney+", "D+", Color(0xFF173A83), "com.disney.disneyplus"),
        TvApp("Shahid", "ش", Color(0xFF18A7A0), "net.mbc.shahid"),
        TvApp("التطبيقات", "▦", Color(0xFF414B60), "")
    )
    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF283B58), Color(0xFF101B2B), HomeBg)))) {
            // Calm scenic-style backdrop with a softly lit horizon.
            Box(Modifier.fillMaxWidth().fillMaxHeight(0.62f).background(Brush.horizontalGradient(listOf(Color(0x332D638D), Color(0x005E8DBA), Color(0x553C5272)))))
            Box(Modifier.align(Alignment.TopEnd).offset(x = 90.dp, y = (-130).dp).size(520.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0x668CB7D8), Color(0x222C537A), Color.Transparent))))
            Column(Modifier.fillMaxSize().padding(horizontal = 46.dp, vertical = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("OUDI", color = HomeText, fontSize = 31.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                        Text("T V   H O M E", color = HomeMuted, fontSize = 10.sp, letterSpacing = 3.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("◉   Wi‑Fi", color = HomeMuted, fontSize = 15.sp)
                    Spacer(Modifier.width(26.dp))
                    Text(clock, color = HomeText, fontSize = 26.sp, fontWeight = FontWeight.Light)
                    Spacer(Modifier.width(18.dp))
                    Text(date, color = HomeMuted, fontSize = 13.sp)
                    Spacer(Modifier.width(20.dp))
                    Text("⚙", color = HomeText, fontSize = 25.sp, modifier = Modifier.clickable { })
                }
                Spacer(Modifier.height(26.dp))
                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)).background(Brush.horizontalGradient(listOf(Color(0xAA101A2A), Color(0x333C658A), Color(0x88405068)))).border(1.dp, Color(0x557C9BB9), RoundedCornerShape(28.dp))) {
                    Box(Modifier.align(Alignment.CenterEnd).padding(end = 55.dp).size(300.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0x99D7E6F0), Color(0x553E668B), Color(0x00243D5B)))))
                    Column(Modifier.align(Alignment.CenterStart).padding(start = 48.dp).width(430.dp)) {
                        Text("مرحباً بك في", color = HomeMuted, fontSize = 22.sp)
                        Text("OUDI HOME", color = HomeText, fontSize = 48.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("تجربة تلفزيون أكثر بساطة وأناقة", color = HomeMuted, fontSize = 19.sp, modifier = Modifier.padding(top = 8.dp))
                        Spacer(Modifier.height(22.dp))
                        Text("استكشف تطبيقاتك ومحتواك المفضل", color = HomeGold, fontSize = 15.sp)
                    }
                    Text("⌁", color = Color(0x668FB9D8), fontSize = 180.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 110.dp))
                }
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("تطبيقاتك", color = HomeText, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("اضغط OK للفتح", color = HomeMuted, fontSize = 13.sp)
                }
                Spacer(Modifier.height(13.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(apps) { app ->
                        var focused by remember { mutableStateOf(false) }
                        Column(
                            Modifier.width(148.dp).height(112.dp)
                                .shadow(if (focused) 16.dp else 2.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(Brush.verticalGradient(listOf(app.tint.copy(alpha = 0.95f), Color(0xFF151C29))))
                                .border(if (focused) 3.dp else 1.dp, if (focused) Color.White else Color(0x447C8DA5), RoundedCornerShape(20.dp))
                                .onFocusChanged { focused = it.isFocused }
                                .focusable()
                                .clickable { if (app.pkg.isNotEmpty()) onAppClick(app.pkg) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(app.mark, color = Color.White, fontSize = if (app.mark.length > 3) 22.sp else 34.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(app.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("اكتشف المزيد", color = HomeText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(14.dp))
                    Text("أفلام   •   مسلسلات   •   محتوى محلي", color = HomeMuted, fontSize = 14.sp)
                }
            }
        }
    }
}
