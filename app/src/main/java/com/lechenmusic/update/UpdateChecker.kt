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
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 服务器端 version.json 格式示例:
 * {
 *   "versionCode": 2,
 *   "versionName": "1.0.1",
 *   "apkUrl": "https://your-domain.com/update/app-release.apk",
 *   "updateLog": "1. 修复播放闪退\n2. 新增歌词显示"
 * }
 */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val updateLog: String
)

object UpdateChecker {

    // ============================================================
    // 👇 改成你自己的服务器地址
    private const val UPDATE_CHECK_URL = "https://lb.tthsdd.top/musicapp/update/version.json"
    // ============================================================

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    /**
     * 检查是否有新版本（挂起函数，在协程中调用）
     * @return UpdateInfo 如果有更新，null 如果没有或出错
     */
    suspend fun check(currentVersionCode: Int): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(UPDATE_CHECK_URL)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext null
                }

                val body = response.body?.string() ?: return@withContext null
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
    }

    /**
     * 下载 APK 并触发安装（使用 OkHttp，更稳定）
     * @return 下载的文件路径，失败返回 null
     */
    suspend fun downloadAndInstall(
        context: Context,
        apkUrl: String,
        onProgress: ((String) -> Unit)? = null
    ): File? {
        return withContext(Dispatchers.IO) {
            try {
                val apkFile = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "LeChenMusic-update.apk"
                )
                if (apkFile.exists()) apkFile.delete()

                withContext(Dispatchers.Main) { onProgress?.invoke("正在下载...") }

                val downloadClient = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(apkUrl)
                    .build()

                val response = downloadClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) { onProgress?.invoke("下载失败 (${response.code})") }
                    return@withContext null
                }

                val body = response.body ?: return@withContext null
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

                if (apkFile.exists() && apkFile.length() > 0) {
                    withContext(Dispatchers.Main) {
                        onProgress?.invoke("下载完成，正在安装...")
                    }
                    installApk(context, apkFile)
                    apkFile
                } else {
                    withContext(Dispatchers.Main) { onProgress?.invoke("下载失败，文件为空") }
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onProgress?.invoke("下载失败: ${e.message}") }
                null
            }
        }
    }

    /**
     * 安装 APK
     */
    private fun installApk(context: Context, apkFile: File) {
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
    }
}
