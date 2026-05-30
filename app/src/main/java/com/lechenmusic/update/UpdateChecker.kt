package com.lechenmusic.update

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val updateLog: String
)

object UpdateChecker {

    // GitHub releases API
    private const val GITHUB_API_URL = "https://api.github.com/repos/yueyoue/LeChenMusic/releases/latest"
    // Fallback custom server
    private const val FALLBACK_UPDATE_URL = "https://yy.tthsdd.top/musicapp/update/version.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun check(currentVersionCode: Int): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            // 优先使用自定义服务器（国内速度快）
            val info = tryCustomServer(currentVersionCode)
            if (info != null) {
                return@withContext info
            }
            // 备用：GitHub releases（国内可能较慢）
            tryGitHubReleases()
        }
    }












    private fun tryGitHubReleases(): UpdateInfo? {
        return try {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)

            val tagName = json.getString("tag_name") // e.g. "v1.1.1"
            val versionName = tagName.removePrefix("v")
            // Extract version code from body or parse from tag
            val bodyText = json.optString("body", "")
            val versionCodeMatch = Regex("versionCode:\\s*(\\d+)").find(bodyText)
            val versionCode = versionCodeMatch?.groupValues?.get(1)?.toIntOrNull()
                ?: parseVersionCodeFromTag(versionName)

            // Find APK asset
            val assets = json.getJSONArray("assets")
            var apkUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }
            if (apkUrl.isEmpty()) return null

            UpdateInfo(
                versionCode = versionCode,
                versionName = versionName,
                apkUrl = apkUrl,
                updateLog = bodyText
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseVersionCodeFromTag(versionName: String): Int {
        // Parse "1.1.1" -> 11 (major*100 + minor*10 + patch, but simple mapping)
        val parts = versionName.split(".")
        return try {
            when {
                parts.size >= 3 -> parts[0].toInt() * 100 + parts[1].toInt() * 10 + parts[2].toInt()
                parts.size == 2 -> parts[0].toInt() * 100 + parts[1].toInt() * 10
                else -> parts[0].toInt() * 100
            }
        } catch (e: Exception) { 0 }
    }

    private fun tryCustomServer(currentVersionCode: Int): UpdateInfo? {
        return try {
            val request = Request.Builder()
                .url(FALLBACK_UPDATE_URL)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val info = UpdateInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.getString("versionName"),
                apkUrl = json.getString("apkUrl"),
                updateLog = json.optString("updateLog", "")
            )
            if (info.versionCode > currentVersionCode) info else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: ((String) -> Unit)? = null
    ): File? {
        return withContext(Dispatchers.IO) {
            val apkFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "LeChenMusic-update.apk"
            )
            if (apkFile.exists()) apkFile.delete()

            // Try with default SSL first
            withContext(Dispatchers.Main) { onProgress?.invoke("正在下载...") }
            val result = tryDownload(apkFile, apkUrl, onProgress)
            if (result != null) return@withContext result

            // Fallback: trust all SSL (for self-signed / expired certs)
            withContext(Dispatchers.Main) { onProgress?.invoke("重试中...") }
            val fallback = tryDownloadTrustAll(apkFile, apkUrl, onProgress)
            if (fallback != null) return@withContext fallback

            withContext(Dispatchers.Main) { onProgress?.invoke("下载失败，请手动下载") }
            null
        }
    }

    private suspend fun tryDownload(
        apkFile: File,
        apkUrl: String,
        onProgress: ((String) -> Unit)? = null
    ): File? {
        return try {
            val downloadClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
            downloadFile(downloadClient, apkFile, apkUrl, onProgress)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun tryDownloadTrustAll(
        apkFile: File,
        apkUrl: String,
        onProgress: ((String) -> Unit)? = null
    ): File? {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val downloadClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
            downloadFile(downloadClient, apkFile, apkUrl, onProgress)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun downloadFile(
        client: OkHttpClient,
        apkFile: File,
        apkUrl: String,
        onProgress: ((String) -> Unit)? = null
    ): File? {
        val request = Request.Builder().url(apkUrl).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            withContext(Dispatchers.Main) { onProgress?.invoke("下载失败 (${response.code})") }
            return null
        }
        val body = response.body ?: return null
        val totalBytes = body.contentLength()
        var downloadedBytes = 0L
        body.byteStream().use { input ->
            apkFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        val progress = (downloadedBytes * 100 / totalBytes).toInt()
                        withContext(Dispatchers.Main) {
                            onProgress?.invoke("正在下载... $progress%")
                        }
                    }
                }
            }
        }
        return if (apkFile.exists() && apkFile.length() > 0) apkFile else null
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        return try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                android.widget.Toast.makeText(context, "安装包文件无效", android.widget.Toast.LENGTH_LONG).show()
                return false
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(
                context,
                "安装失败: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
            false
        }
    }
}
