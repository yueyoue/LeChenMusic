package com.lechenmusic.update

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.ConnectionPool
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
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

    private const val TAG = "UpdateChecker"
    private const val GITHUB_API_URL = "https://api.github.com/repos/yueyoue/LeChenMusic/releases/latest"
    private const val CUSTOM_SERVER_URL = "https://yy.tthsdd.top/musicapp/update/version.json"

    // 普通请求用的 client（检查版本信息）
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // 下载用的 client - 支持 HTTP/1.1 和 HTTP/2，自动协商
    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun check(currentVersionCode: Int): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            val info = tryCustomServer(currentVersionCode)
            if (info != null) {
                Log.d(TAG, "Found update from custom server: v${info.versionName} (${info.versionCode})")
                return@withContext info
            }
            val ghInfo = tryGitHubReleases(currentVersionCode)
            if (ghInfo != null) {
                Log.d(TAG, "Found update from GitHub: v${ghInfo.versionName} (${ghInfo.versionCode})")
            }
            ghInfo
        }
    }

    private fun tryGitHubReleases(currentVersionCode: Int): UpdateInfo? {
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

            val tagName = json.getString("tag_name")
            val versionName = tagName.removePrefix("v")
            val bodyText = json.optString("body", "")
            val versionCodeMatch = Regex("versionCode:\\s*(\\d+)").find(bodyText)
            val versionCode = versionCodeMatch?.groupValues?.get(1)?.toIntOrNull()
                ?: parseVersionCodeFromTag(versionName)

            if (versionCode <= currentVersionCode) return null

            val assets = json.getJSONArray("assets")
            var apkUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }
            if (apkUrl.isEmpty()) return null

            val updateLog = bodyText
                .lines()
                .filter { !it.trim().startsWith("versionCode:") }
                .joinToString("\n")
                .trim()

            UpdateInfo(versionCode, versionName, apkUrl, updateLog)
        } catch (e: Exception) {
            Log.e(TAG, "GitHub check failed", e)
            null
        }
    }

    private fun parseVersionCodeFromTag(versionName: String): Int {
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
                .url(CUSTOM_SERVER_URL)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Custom server returned ${response.code}")
                return null
            }
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val info = UpdateInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.getString("versionName"),
                apkUrl = json.getString("apkUrl"),
                updateLog = json.optString("updateLog", "")
            )
            Log.d(TAG, "Custom server version: ${info.versionCode}, current: $currentVersionCode")
            if (info.versionCode > currentVersionCode) info else null
        } catch (e: Exception) {
            Log.e(TAG, "Custom server check failed", e)
            null
        }
    }

    private fun getGitHubApkUrl(): String? {
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
            val assets = json.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    return asset.getString("browser_download_url")
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get GitHub APK URL", e)
            null
        }
    }

    /**
     * 创建信任所有证书的 OkHttpClient（强制 HTTP/1.1）
     */
    private fun createTrustAllClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
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

            Log.d(TAG, "=== Starting download from: $apkUrl ===")

            // === 第一步：标准下载（每次用全新 client） ===
            for (attempt in 1..3) {
                val attemptMsg = if (attempt == 1) "正在下载..." else "重试中 ($attempt/3)..."
                withContext(Dispatchers.Main) { onProgress?.invoke(attemptMsg) }
                Log.d(TAG, "Standard download attempt $attempt/3")

                val freshClient = newFreshClient()
                try {
                    val result = downloadFile(freshClient, apkFile, apkUrl, onProgress)
                    if (result != null) {
                        Log.d(TAG, "✅ Standard download succeeded, size=${result.length()}")
                        return@withContext result
                    }
                } catch (e: Exception) {
                    val errDetail = "${e.javaClass.simpleName}: ${e.message}"
                    Log.e(TAG, "❌ Standard attempt $attempt failed: $errDetail")
                    withContext(Dispatchers.Main) {
                        onProgress?.invoke("下载失败 ($errDetail)")
                    }
                } finally {
                    freshClient.dispatcher.executorService.shutdown()
                    freshClient.connectionPool.evictAll()
                }

                if (apkFile.exists()) apkFile.delete()
                if (attempt < 3) delay(1000L * attempt)
            }

            // === 第二步：信任所有证书下载（解决证书链不完整问题） ===
            withContext(Dispatchers.Main) { onProgress?.invoke("正在尝试备用连接...") }
            Log.d(TAG, "Trying trust-all download (HTTP/1.1)")

            try {
                val trustAllClient = createTrustAllClient()
                val result = downloadFile(trustAllClient, apkFile, apkUrl, onProgress)
                if (result != null) {
                    Log.d(TAG, "✅ Trust-all download succeeded, size=${result.length()}")
                    return@withContext result
                }
            } catch (e: Exception) {
                val errDetail = "${e.javaClass.simpleName}: ${e.message}"
                Log.e(TAG, "❌ Trust-all download failed: $errDetail")
                withContext(Dispatchers.Main) {
                    onProgress?.invoke("备用连接失败 ($errDetail)")
                }
            }
            if (apkFile.exists()) apkFile.delete()

            // === 第三步：GitHub 回退 ===
            val githubUrl = getGitHubApkUrl()
            if (githubUrl != null && githubUrl != apkUrl) {
                withContext(Dispatchers.Main) { onProgress?.invoke("正在从 GitHub 下载...") }
                Log.d(TAG, "Trying GitHub fallback: $githubUrl")
                for (attempt in 1..2) {
                    try {
                        val result = downloadFile(downloadClient, apkFile, githubUrl, onProgress)
                        if (result != null) {
                            Log.d(TAG, "✅ GitHub download succeeded")
                            return@withContext result
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ GitHub attempt $attempt failed: ${e.message}")
                    }
                    if (apkFile.exists()) apkFile.delete()
                    if (attempt < 2) delay(1000L)
                }
                // GitHub trust-all
                try {
                    val trustAllClient = createTrustAllClient()
                    val result = downloadFile(trustAllClient, apkFile, githubUrl, onProgress)
                    if (result != null) {
                        Log.d(TAG, "✅ GitHub trust-all succeeded")
                        return@withContext result
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ GitHub trust-all failed: ${e.message}")
                }
                if (apkFile.exists()) apkFile.delete()
            }

            withContext(Dispatchers.Main) { onProgress?.invoke("下载失败，请手动下载") }
            Log.e(TAG, "=== ALL download attempts FAILED for: $apkUrl ===")
            null
        }
    }

    /**
     * 构建完全干净的请求 —— 不带任何 Range 头，不走缓存
     */
    private fun buildFreshRequest(apkUrl: String): Request {
        return Request.Builder()
            .url(apkUrl)
            .header("Accept-Encoding", "identity")
            .header("User-Agent", "LeChenMusic/1.0")
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("Connection", "close")
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()
    }

    /**
     * 创建一个全新的 OkHttpClient（独立连接池，不受旧连接影响）
     */
    private fun newFreshClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
            .build()
    }

    private suspend fun downloadFile(
        client: OkHttpClient,
        apkFile: File,
        apkUrl: String,
        onProgress: ((String) -> Unit)? = null
    ): File? {
        Log.d(TAG, "downloadFile: $apkUrl")
        if (apkFile.exists()) apkFile.delete()

        val request = buildFreshRequest(apkUrl)

        val response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            Log.e(TAG, "HTTP request failed: ${e.javaClass.simpleName}: ${e.message}")
            throw e
        }

        // 416: 服务器拒绝 Range 请求 → 用全新 client + 无 Range 头重试
        if (response.code == 416) {
            Log.w(TAG, "Got 416, creating fresh client for clean retry")
            response.close()
            if (apkFile.exists()) apkFile.delete()

            val freshClient = newFreshClient()
            try {
                val freshRequest = buildFreshRequest(apkUrl)
                val freshResponse = freshClient.newCall(freshRequest).execute()
                if (!freshResponse.isSuccessful) {
                    val errMsg = "HTTP ${freshResponse.code}: ${freshResponse.message}"
                    Log.e(TAG, "416 retry failed: $errMsg")
                    freshResponse.close()
                    throw IOException(errMsg)
                }
                return downloadFromResponse(freshResponse, apkFile, onProgress)
            } finally {
                freshClient.dispatcher.executorService.shutdown()
                freshClient.connectionPool.evictAll()
            }
        }

        if (!response.isSuccessful) {
            val errMsg = "HTTP ${response.code}: ${response.message}"
            Log.e(TAG, "HTTP error: $errMsg")
            response.close()
            throw IOException(errMsg)
        }

        return downloadFromResponse(response, apkFile, onProgress)
    }

    private suspend fun downloadFromResponse(
        response: okhttp3.Response,
        apkFile: File,
        onProgress: ((String) -> Unit)? = null
    ): File? {
        val body = response.body ?: run {
            response.close()
            throw IOException("Response body is null")
        }
        val totalBytes = body.contentLength()
        var downloadedBytes = 0L
        var lastProgressTime = 0L

        Log.d(TAG, "Content-Length: $totalBytes bytes")

        try {
            body.byteStream().use { input ->
                apkFile.outputStream().buffered(65536).use { output ->
                    val buffer = ByteArray(65536)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val now = System.currentTimeMillis()
                        if (totalBytes > 0 && now - lastProgressTime >= 500) {
                            lastProgressTime = now
                            val progress = (downloadedBytes * 100 / totalBytes).toInt()
                            val sizeMB = downloadedBytes / 1024.0 / 1024.0
                            val totalMB = totalBytes / 1024.0 / 1024.0
                            withContext(Dispatchers.Main) {
                                onProgress?.invoke("正在下载... $progress%% (%.1f/%.1f MB)".format(sizeMB, totalMB))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stream error at $downloadedBytes/$totalBytes bytes: ${e.message}")
            if (apkFile.exists()) apkFile.delete()
            throw e
        } finally {
            response.close()
        }

        // 检查是否下载完整
        if (totalBytes > 0 && downloadedBytes < totalBytes) {
            Log.e(TAG, "Incomplete download: $downloadedBytes / $totalBytes bytes")
            if (apkFile.exists()) apkFile.delete()
            return null
        }

        if (!apkFile.exists() || apkFile.length() < 100_000L) {
            Log.e(TAG, "Downloaded file too small or missing: ${apkFile.length()} bytes")
            if (apkFile.exists()) apkFile.delete()
            return null
        }

        // Verify APK magic bytes (PK ZIP header)
        try {
            val header = ByteArray(2)
            apkFile.inputStream().use { it.read(header) }
            if (header[0] != 0x50.toByte() || header[1] != 0x4B.toByte()) {
                Log.e(TAG, "Invalid APK file: not a ZIP archive (header: ${header[0]}, ${header[1]})")
                apkFile.delete()
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify APK header: ${e.message}")
        }

        Log.d(TAG, "Download complete: ${apkFile.length()} bytes")
        return apkFile
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
            Log.e(TAG, "Install failed", e)
            android.widget.Toast.makeText(
                context,
                "安装失败: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
            false
        }
    }
}
