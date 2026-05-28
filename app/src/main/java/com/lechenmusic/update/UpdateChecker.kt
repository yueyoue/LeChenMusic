package com.lechenmusic.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val updateLog: String
)

object UpdateChecker {

    private const val UPDATE_CHECK_URL = "https://lb.tthsdd.top/musicapp/update/version.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun check(currentVersionCode: Int): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(UPDATE_CHECK_URL)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext null

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

                onProgress?.invoke("正在下载...")

                val request = DownloadManager.Request(Uri.parse(apkUrl))
                    .setTitle("LeChenMusic 更新")
                    .setDescription("正在下载新版本...")
                    .setDestinationUri(Uri.fromFile(apkFile))
                    .setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    )
                    .setAllowedOverMetered(true)

                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val downloadId = dm.enqueue(request)

                val success = suspendCancellableCoroutine<Boolean> { cont ->
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context, intent: Intent) {
                            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                            if (id == downloadId) {
                                context.unregisterReceiver(this)
                                val query = DownloadManager.Query().setFilterById(downloadId)
                                val cursor = dm.query(query)
                                if (cursor != null && cursor.moveToFirst()) {
                                    val status = cursor.getInt(
                                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                                    )
                                    cursor.close()
                                    cont.resume(status == DownloadManager.STATUS_SUCCESSFUL)
                                } else {
                                    cursor?.close()
                                    cont.resume(false)
                                }
                            }
                        }
                    }

                    context.registerReceiver(
                        receiver,
                        IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                    )

                    cont.invokeOnCancellation {
                        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
                    }
                }

                if (success && apkFile.exists()) {
                    onProgress?.invoke("下载完成，正在安装...")
                    installApk(context, apkFile)
                    apkFile
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

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
