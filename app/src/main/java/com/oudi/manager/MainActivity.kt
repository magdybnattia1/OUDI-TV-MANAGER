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
    var ip by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("جاهز للبحث عن تلفزيونك") }
    var scanning by remember { mutableStateOf(false) }
    var connecting by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf(listOf<String>()) }
    var connected by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun scan() {
        scanning = true
        connected = false
        status = "جاري فحص الشبكة المحلية بحثاً عن ADB…"
        found = emptyList()
        scope.launch {
            val candidates = withContext(Dispatchers.IO) {
                val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                val dhcp = wifi.dhcpInfo
                if (dhcp == null || dhcp.ipAddress == 0) emptyList() else {
                    val ipInt = dhcp.ipAddress
                    val mask = dhcp.netmask
                    val network = ipInt and mask
                    val broadcast = network or mask.inv()
                    val prefix = (network.toLong() and 0xffffffffL)
                    (1..254).mapNotNull { host ->
                        val address = (prefix + host).toInt()
                        if (address == ipInt) null else ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(address).array().joinToString(".") { (it.toInt() and 0xff).toString() }
                    }.takeIf { (broadcast - network) > 2 } ?: emptyList()
                }
            }
            if (candidates.isEmpty()) {
                scanning = false
                status = "تعذر قراءة شبكة Wi‑Fi. أدخل IP التلفزيون يدوياً."
                return@launch
            }
            val gate = Semaphore(24)
            val reachable = candidates.map { host -> async(Dispatchers.IO) {
                gate.acquire()
                try {
                    Socket().use { it.connect(InetSocketAddress(host, 5555), 450) }
                    host
                } catch (_: Exception) { null }
                finally { gate.release() }
            } }.awaitAll().filterNotNull().distinct()
            found = reachable
            scanning = false
            if (reachable.isEmpty()) status = "لم نعثر على ADB على المنفذ 5555. جرّب إدخال IP يدوياً وتأكد من تفعيل تصحيح ADB عبر الشبكة."
            else {
                status = "عُثر على ${reachable.size} جهاز يستجيب على منفذ ADB. اختر تلفزيون Tornado."
                if (ip.isBlank()) ip = reachable.first()
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Ink, surface = Panel)) {
        Column(Modifier.fillMaxSize().background(Ink).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(18.dp))
            Text("OUDI", color = Gold, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text("TV MANAGER", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("إدارة تلفزيونك من الموبايل", color = Color.LightGray, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(26.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("1  •  اكتشاف التلفزيون", color = Gold, fontWeight = FontWeight.Bold)
                    Text("وصّل الموبايل والتلفزيون على نفس شبكة Wi‑Fi. يبحث التطبيق عن الأجهزة التي تستجيب على منفذ ADB 5555.", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                    Button(onClick = { scan() }, enabled = !scanning, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text(if (scanning) "جاري البحث…" else "ابحث عن التلفزيونات") }
                    if (found.isNotEmpty()) {
                        found.forEach { host -> TextButton(onClick = { ip = host }) { Text("اختيار $host", color = Gold) } }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("2  •  الاتصال", color = Gold, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("عنوان IP للتلفزيون") }, placeholder = { Text("مثال: 192.168.1.25") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        connecting = true; connected = false
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                try { Socket().use { it.connect(InetSocketAddress(ip.trim(), 5555), 2500) }; true } catch (_: Exception) { false }
                            }
                            connecting = false; connected = ok
                            status = if (ok) "منفذ ADB يستجيب. الخطوة التالية تتطلب مصادقة ADB من التلفزيون." else "لم يتم الوصول إلى منفذ ADB 5555. فعّل Network ADB من إعدادات التلفزيون وتأكد من نفس الشبكة."
                        }
                    }, enabled = ip.isNotBlank() && !connecting, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text(if (connecting) "جاري الاتصال…" else "اختبار الاتصال") }
                }
            }
            Spacer(Modifier.height(14.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("3  •  تجهيز OUDI", color = Gold, fontWeight = FontWeight.Bold)
                    Text("حالة الاتصال: $status", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                    if (connected) Text("تم الوصول إلى منفذ ADB فقط. يلزم إتمام مصادقة ADB قبل إرسال أي أمر.", color = Gold, modifier = Modifier.padding(top = 8.dp))
                    Text("هذه نسخة فحص اتصال فقط: لا تثبّت تطبيقات ولا تغيّر الواجهة الرئيسية بعد. سنضيف محرك ADB موثوقاً وخطة استعادة قبل تنفيذ التغييرات.", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            Text("OUDI • إصدار تجريبي 0.2.0", color = Color.Gray, fontSize = 11.sp)
        }
    }
}
