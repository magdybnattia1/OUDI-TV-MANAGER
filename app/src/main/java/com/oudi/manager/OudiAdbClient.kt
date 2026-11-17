package com.oudi.manager

import android.content.Context
import android.util.Base64
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

/**
 * Real ADB host connection for legacy Android TV network debugging (TCP/5555).
 * The RSA host key is generated once and retained in app-private no-backup storage,
 * so the TV's "Allow USB debugging" trust remains stable between app launches.
 */
object OudiAdbClient {
    private val encoder = object : AdbBase64 {
        override fun encodeToString(data: ByteArray): String =
            Base64.encodeToString(data, Base64.NO_WRAP)
    }

    private fun crypto(context: Context): AdbCrypto {
        val dir = File(context.noBackupFilesDir, "adb").apply { mkdirs() }
        val privateKey = File(dir, "adbkey")
        val publicKey = File(dir, "adbkey.pub")
        return if (privateKey.isFile && publicKey.isFile) {
            AdbCrypto.loadAdbKeyPair(encoder, privateKey, publicKey)
        } else {
            val generated = AdbCrypto.generateAdbKeyPair(encoder)
            generated.saveAdbKeyPair(privateKey, publicKey)
            privateKey.setReadable(false, false)
            privateKey.setReadable(true, true)
            publicKey.setReadable(true, true)
            generated
        }
    }

    /**
     * Performs ADB AUTH/CNXN, then runs a harmless shell identity check.
     * A plain TCP socket-open is deliberately not considered a successful connection.
     */
    fun connectAndVerify(context: Context, host: String, port: Int = 5555): String {
        require(host.matches(Regex("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))) { "عنوان IP غير صحيح" }
        require(port in 1..65535) { "رقم المنفذ غير صحيح" }

        val socket = Socket()
        var connection: AdbConnection? = null
        try {
            socket.connect(InetSocketAddress(host, port), 4000)
            socket.soTimeout = 12000
            connection = AdbConnection.create(socket, crypto(context))
            val authenticated = connection.connect(12, TimeUnit.SECONDS, false)
            if (!authenticated) {
                throw IOException("انتهت مهلة مصادقة ADB. وافق على رسالة السماح التي تظهر على التلفزيون.")
            }

            val stream = connection.open("shell:echo OUDI_ADB_OK")
            val output = StringBuilder()
            try {
                while (true) {
                    val chunk = stream.read()
                    output.append(String(chunk, Charsets.UTF_8))
                    if (output.length > 4096) break
                }
            } catch (_: IOException) {
                // The remote shell closes the stream after echo completes.
            } finally {
                try { stream.close() } catch (_: Exception) {}
            }
            val result = output.toString()
            if (!result.contains("OUDI_ADB_OK")) {
                throw IOException("تمت مصافحة ADB لكن فشل اختبار أوامر shell.")
            }
            return "تم اتصال ADB والمصادقة بنجاح مع $host:$port"
        } finally {
            try { connection?.close() } catch (_: Exception) {}
            try { socket.close() } catch (_: Exception) {}
        }
    }
}
