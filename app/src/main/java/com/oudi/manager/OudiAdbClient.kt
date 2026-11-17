package com.oudi.manager

import android.content.Context
import android.util.Base64
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import com.tananaev.adblib.AdbStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

object OudiAdbClient {
    private val encoder = object : AdbBase64 {
        override fun encodeToString(data: ByteArray): String = Base64.encodeToString(data, Base64.NO_WRAP)
    }

    private fun crypto(context: Context): AdbCrypto {
        val dir = File(context.noBackupFilesDir, "adb").apply { mkdirs() }
        val privateKey = File(dir, "adbkey")
        val publicKey = File(dir, "adbkey.pub")
        return if (privateKey.isFile && publicKey.isFile) AdbCrypto.loadAdbKeyPair(encoder, privateKey, publicKey)
        else AdbCrypto.generateAdbKeyPair(encoder).also {
            it.saveAdbKeyPair(privateKey, publicKey)
            privateKey.setReadable(false, false)
            privateKey.setReadable(true, true)
            publicKey.setReadable(true, true)
        }
    }

    private fun connect(context: Context, host: String): Pair<Socket, AdbConnection> {
        require(host.matches(Regex("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))) { "عنوان IP غير صحيح" }
        val socket = Socket()
        socket.connect(InetSocketAddress(host, 5555), 5000)
        socket.soTimeout = 20000
        val conn = AdbConnection.create(socket, crypto(context))
        if (!conn.connect(15, TimeUnit.SECONDS, false)) {
            socket.close()
            throw IOException("انتهت مهلة ADB؛ وافق على رسالة السماح في التلفزيون")
        }
        return socket to conn
    }

    private fun shell(context: Context, host: String, command: String): String {
        val (socket, conn) = connect(context, host)
        try {
            val stream = conn.open("shell:$command")
            val out = ByteArrayOutputStream()
            try {
                while (true) {
                    val part = stream.read()
                    out.write(part)
                    if (out.size() > 65536) break
                }
            } catch (_: IOException) { }
            finally { try { stream.close() } catch (_: Exception) {} }
            return out.toString("UTF-8").trim()
        } finally {
            try { conn.close() } catch (_: Exception) {}
            try { socket.close() } catch (_: Exception) {}
        }
    }

    fun connectAndVerify(context: Context, host: String): String {
        val response = shell(context, host, "echo OUDI_ADB_OK")
        if (!response.contains("OUDI_ADB_OK")) throw IOException("فشل اختبار أوامر ADB")
        return "تم الاتصال والمصادقة بنجاح مع $host:5555"
    }

    private fun writeInt(stream: AdbStream, value: Int) {
        stream.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array())
    }

    private fun sendSync(stream: AdbStream, tag: String, payload: ByteArray) {
        stream.write(tag.toByteArray(Charsets.US_ASCII))
        writeInt(stream, payload.size)
        if (payload.isNotEmpty()) stream.write(payload)
    }

    private fun pushApk(stream: AdbStream, file: File) {
        val destination = "/data/local/tmp/OUDI-TV.apk,420".toByteArray(Charsets.UTF_8)
        sendSync(stream, "SEND", destination)
        file.inputStream().use { input ->
            val buffer = ByteArray(1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                val data = ByteArray(4 + count)
                ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).putInt(count).put(buffer, 0, count)
                stream.write("DATA".toByteArray(Charsets.US_ASCII))
                stream.write(data)
            }
        }
        stream.write("DONE".toByteArray(Charsets.US_ASCII))
        writeInt(stream, (System.currentTimeMillis() / 1000L).toInt())
        val reply = stream.read()
        if (reply.size < 4 || String(reply, 0, 4, Charsets.US_ASCII) != "OKAY") {
            val reason = if (reply.size > 8) String(reply, 8, reply.size - 8, Charsets.UTF_8) else "رفض التلفزيون نقل ملف APK"
            throw IOException(reason)
        }
    }

    fun installHome(context: Context, host: String): String {
        val apk = File(context.applicationInfo.sourceDir)
        if (!apk.isFile || apk.length() < 100000) throw IOException("ملف التطبيق غير متاح على الموبايل")
        val (socket, conn) = connect(context, host)
        try {
            val sync = conn.open("sync:")
            try { pushApk(sync, apk) } finally { try { sync.close() } catch (_: Exception) {} }
        } finally {
            try { conn.close() } catch (_: Exception) {}
            try { socket.close() } catch (_: Exception) {}
        }
        val result = shell(context, host, "pm install -r /data/local/tmp/OUDI-TV.apk")
        if (!result.contains("Success", ignoreCase = true)) throw IOException("فشل تثبيت OUDI HOME: $result")
        return "تم تثبيت OUDI HOME على التلفزيون بنجاح. الواجهة الأصلية ما زالت كما هي."
    }

    fun openHome(host: String): String {
        val result = shellWithoutContext(host, "am start -n com.oudi.manager/.OudiHomeActivity")
        if (result.contains("Error", true) || result.contains("Exception", true)) throw IOException(result)
        return "تم إرسال أمر تشغيل OUDI HOME. افحص الشاشة."
    }

    fun restoreHome(host: String): String {
        val result = shellWithoutContext(host, "cmd package clear-package-preferred-activities com.oudi.manager; am start -a android.intent.action.MAIN -c android.intent.category.HOME")
        return "تم إلغاء تفضيل OUDI. اختر واجهة التلفزيون الأصلية من شاشة الاختيار إن ظهرت. $result"
    }

    private fun shellWithoutContext(host: String, command: String): String {
        throw IOException("يلزم إعادة استخدام مفتاح ADB المحفوظ؛ افتح الإجراء من التطبيق بعد إتمام الاتصال.")
    }
}
