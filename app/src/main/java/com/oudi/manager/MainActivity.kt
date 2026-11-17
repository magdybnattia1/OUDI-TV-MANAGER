package com.oudi.manager

import android.content.Context
import android.net.wifi.WifiManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Semaphore
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

private val Ink = Color(0xFF10131C)
private val Gold = Color(0xFFD6A84F)
private val Panel = Color(0xFF1B2130)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OudiManager(this) }
    }
}

@Composable
private fun OudiManager(context: Context) {
    var ip by remember { mutableStateOf("192.168.1.2") }
    var status by remember { mutableStateOf("جاهز للاتصال") }
    var scanning by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()

    fun action(work: suspend () -> String) {
        if (busy || ip.isBlank()) return
        busy = true
        scope.launch {
            status = "جاري التنفيذ…"
            val result = withContext(Dispatchers.IO) { runCatching { work() } }
            status = result.getOrElse { it.message ?: "فشلت العملية" }
            busy = false
        }
    }

    fun scan() {
        scanning = true
        scope.launch {
            val candidates = withContext(Dispatchers.IO) {
                val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION") val dhcp = wifi.dhcpInfo
                if (dhcp == null || dhcp.ipAddress == 0) emptyList() else {
                    val local = dhcp.ipAddress
                    val network = local and dhcp.netmask
                    (1..254).mapNotNull { n ->
                        val a = (network.toLong() and 0xffffffffL) + n
                        val addr = a.toInt()
                        if (addr == local) null else ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(addr).array().joinToString(".") { (it.toInt() and 255).toString() }
                    }
                }
            }
            val gate = Semaphore(24)
            found = candidates.map { host -> async(Dispatchers.IO) {
                gate.acquire()
                try { Socket().use { it.connect(InetSocketAddress(host, 5555), 450) }; host }
                catch (_: Exception) { null }
                finally { gate.release() }
            } }.awaitAll().filterNotNull().distinct()
            scanning = false
            status = if (found.isEmpty()) "لم يتم العثور على جهاز؛ أدخل IP يدوياً" else "اختر التلفزيون من القائمة"
            if (found.isNotEmpty()) ip = found.first()
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Ink, surface = Panel)) {
        Column(Modifier.fillMaxSize().background(Ink).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("OUDI", color = Gold, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text("TV MANAGER", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("إدارة تلفزيونك من الموبايل", color = Color.LightGray)
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("1 • الاتصال بالتلفزيون", color = Gold, fontWeight = FontWeight.Bold)
                    Button(onClick = { scan() }, enabled = !scanning && !busy, modifier = Modifier.fillMaxWidth()) {
                        Text(if (scanning) "جاري البحث…" else "ابحث عن التلفزيون")
                    }
                    found.forEach { host -> TextButton(onClick = { ip = host }) { Text(host, color = Gold) } }
                    OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("IP التلفزيون") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { action { OudiAdbClient.connectAndVerify(context, ip.trim()) } }, enabled = ip.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("اختبار الاتصال")
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("2 • تثبيت وتشغيل OUDI HOME", color = Gold, fontWeight = FontWeight.Bold)
                    Text("ينقل التطبيق المثبت على الموبايل إلى التلفزيون. الواجهة الأصلية لن تُحذف.", color = Color.White, fontSize = 13.sp)
                    Button(onClick = { action { OudiAdbClient.installHome(context, ip.trim()) } }, enabled = ip.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("تثبيت OUDI HOME على التلفزيون")
                    }
                    Button(onClick = { action { OudiAdbClient.openHome(context, ip.trim()) } }, enabled = ip.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("تشغيل OUDI HOME للتجربة")
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("3 • الرجوع للواجهة الأصلية", color = Gold, fontWeight = FontWeight.Bold)
                    Text("يمسح اختيار OUDI كواجهة افتراضية ويعرض اختيار واجهة الشاشة من جديد.", color = Color.White, fontSize = 13.sp)
                    OutlinedButton(onClick = { action { OudiAdbClient.restoreHome(context, ip.trim()) } }, enabled = ip.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("إلغاء اختيار OUDI HOME")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("الحالة: $status", color = Gold, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text("OUDI • إصدار تجريبي", color = Color.Gray, fontSize = 11.sp)
        }
    }
}
