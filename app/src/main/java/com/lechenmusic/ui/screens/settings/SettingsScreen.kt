package com.lechenmusic.ui.screens.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.lechenmusic.ui.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val cacheSize by viewModel.cacheSize.collectAsState()
    val serverStats by viewModel.serverStats.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val context = LocalContext.current
    var showCacheDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val updateInfo by viewModel.updateInfo.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    var userTriggeredCheck by remember { mutableStateOf(false) }

    // Show toast messages (including "already latest")
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // 检查到更新时弹窗（支持手动多次触发）
    LaunchedEffect(updateInfo, userTriggeredCheck) {
        if (updateInfo != null && userTriggeredCheck) {
            showUpdateDialog = true
            userTriggeredCheck = false
        }
    }
    var musicCacheSize by remember { mutableStateOf("计算中...") }
    var otherDataSize by remember { mutableStateOf("计算中...") }

    LaunchedEffect(Unit) {
        val cacheBytes = viewModel.playerManager.getCacheBytes()
        musicCacheSize = if (cacheBytes > 0) formatSize(cacheBytes) else calculateCacheSize(context, "music_cache")
        otherDataSize = calculateOtherDataSize(context)
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                Text("设置", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            SectionTitle("服务器信息")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    InfoRow("服务器地址", serverUrl)
                    InfoRow("用户名", username)
                    InfoRow("歌曲数量", if (serverStats.songCount > 0) "${serverStats.songCount}" else "加载中...")
                    InfoRow("专辑数量", if (serverStats.albumCount > 0) "${serverStats.albumCount}" else "加载中...")
                    InfoRow("歌手数量", if (serverStats.artistCount > 0) "${serverStats.artistCount}" else "加载中...")
                    InfoRow("歌单数量", if (serverStats.playlistCount > 0) "${serverStats.playlistCount}" else "加载中...")
                }
            }
        }

        item {
            SectionTitle("外观设置")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column {
                    SettingsToggleItem(icon = Icons.Default.Settings, iconBg = Color(0xFFA55EEA).copy(alpha = 0.15f), label = "深色模式", checked = themeMode == "dark", onCheckedChange = { viewModel.setThemeMode(if (it) "dark" else "light") })
                }
            }
        }

        item {
            SectionTitle("缓存设置")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column {
                    SettingsClickItem(icon = Icons.Default.Storage, iconBg = Color(0xFFFF4757).copy(alpha = 0.15f), label = "音乐缓存大小", value = "${cacheSize} GB", onClick = { showCacheDialog = true })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsClickItem(icon = Icons.Default.Sync, iconBg = Color(0xFF2ED573).copy(alpha = 0.15f), label = "同步服务器数据", value = if (syncStatus.isNotEmpty()) syncStatus else "同步歌单、歌手、收藏、专辑、歌曲、统计", onClick = { viewModel.syncData() })
                }
            }
        }

        item {
            SectionTitle("存储空间")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val exoCacheBytes = viewModel.playerManager.getCacheBytes()
                    val musicBytes = if (exoCacheBytes > 0) exoCacheBytes else getCacheSizeBytes(context, "music_cache")
                    val otherBytes = getOtherDataSizeBytes(context)
                    val totalUsed = musicBytes + otherBytes
                    val maxBytes = cacheSize.toLong() * 1024 * 1024 * 1024
                    val progress = if (maxBytes > 0) (totalUsed.toFloat() / maxBytes).coerceIn(0f, 1f) else 0f
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("已使用 ${formatSize(totalUsed)}", fontSize = 13.sp)
                        Text("共 ${cacheSize} GB", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🎵 音乐缓存 $musicCacheSize", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("📦 其他数据 $otherDataSize", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.playerManager.clearMusicCache(); musicCacheSize = "0 B"; otherDataSize = calculateOtherDataSize(context) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("清除缓存", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            SectionTitle("账号")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column {
                    SettingsClickItem(icon = Icons.Default.Person, iconBg = Color(0xFF5352ED).copy(alpha = 0.15f), label = "当前账号", value = username, onClick = { })
                    SettingsClickItem(icon = Icons.Default.Link, iconBg = Color(0xFFFFA502).copy(alpha = 0.15f), label = "服务器地址", value = serverUrl, onClick = { })
                    Box(modifier = Modifier.fillMaxWidth().clickable { showLogoutDialog = true }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        Text("切换服务器", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    }
                }
            }
        }

        // 版本更新
        item {
            SectionTitle("版本更新")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column {
                    val isChecking by viewModel.isCheckingUpdate.collectAsState()
                    SettingsClickItem(
                        icon = Icons.Default.Refresh,
                        iconBg = Color(0xFF1E90FF).copy(alpha = 0.15f),
                        label = "检查更新",
                        value = if (isChecking) "检查中..."
                                else if (updateInfo != null) "有新版本 v${updateInfo!!.versionName}"
                                else if (updateStatus.isNotEmpty()) updateStatus
                                else "当前 ${getCurrentVersionName(context)}",
                        onClick = { viewModel.dismissUpdate(); userTriggeredCheck = true; viewModel.checkForUpdate(silent = false) }
                    )
                }
            }
        }

        item {
            SectionTitle("关于")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column {
                    SettingsClickItem(icon = Icons.Default.Info, iconBg = Color(0xFF2ED573).copy(alpha = 0.15f), label = "关于悦音", value = "", onClick = { showAboutDialog = true })
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于悦音", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("官网", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("http://yy.tthsdd.top", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("邮箱", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("10711306@qq.com", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("确定") } }
        )
    }

    // 更新弹窗
    if (showUpdateDialog && updateInfo != null) {
        val info = updateInfo!!
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("发现新版本 v${info.versionName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("更新内容：", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(info.updateLog, fontSize = 14.sp)
                    if (updateStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(updateStatus, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.downloadUpdate() }) {
                    Text("立即更新", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.skipUpdate(); showUpdateDialog = false }) { Text("跳过该版本") }
            }
        )
    }

    if (showCacheDialog) {
        AlertDialog(onDismissRequest = { showCacheDialog = false }, title = { Text("选择缓存大小") }, text = {
            Column {
                listOf(2, 4, 8, 16).forEach { size ->
                    Text("${size} GB", modifier = Modifier.fillMaxWidth().clickable { viewModel.setCacheSize(size); showCacheDialog = false }.padding(vertical = 14.dp), fontSize = 15.sp, fontWeight = if (size == cacheSize) FontWeight.Bold else FontWeight.Normal, color = if (size == cacheSize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }, confirmButton = { TextButton(onClick = { showCacheDialog = false }) { Text("取消") } })
    }

    if (showLogoutDialog) {
        AlertDialog(onDismissRequest = { showLogoutDialog = false }, title = { Text("切换服务器") }, text = { Text("确定要退出当前服务器吗？退出后需要重新输入服务器地址登录。") }, confirmButton = {
            TextButton(onClick = { viewModel.logout(); showLogoutDialog = false; onLogout() }) { Text("确定", color = MaterialTheme.colorScheme.primary) }
        }, dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("取消") } })
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, letterSpacing = 1.sp, modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

@Composable
private fun SettingsToggleItem(icon: ImageVector, iconBg: Color, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(32.dp), shape = RoundedCornerShape(8.dp), color = iconBg) { Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) } }
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsClickItem(icon: ImageVector, iconBg: Color, label: String, value: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(32.dp), shape = RoundedCornerShape(8.dp), color = iconBg) { Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) } }
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

private fun getCacheSizeBytes(context: Context, dirName: String): Long {
    val dir = java.io.File(context.cacheDir, dirName)
    if (!dir.exists()) return 0
    return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

private fun getOtherDataSizeBytes(context: Context): Long {
    var size = 0L
    val datastoreDir = java.io.File(context.filesDir, "datastore")
    if (datastoreDir.exists()) size += datastoreDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    context.cacheDir.listFiles()?.forEach { f ->
        if (f.name != "music_cache") {
            size += if (f.isDirectory) f.walkTopDown().filter { it.isFile }.sumOf { it.length() } else f.length()
        }
    }
    return size
}

private fun calculateCacheSize(context: Context, dirName: String): String = formatSize(getCacheSizeBytes(context, dirName))
private fun calculateOtherDataSize(context: Context): String = formatSize(getOtherDataSizeBytes(context))

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}

private fun clearCache(context: Context, dirName: String) {
    val dir = java.io.File(context.cacheDir, dirName)
    if (dir.exists()) { dir.deleteRecursively(); dir.mkdirs() }
}

private fun getCurrentVersionName(context: Context): String {
    return try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0" } catch (_: Exception) { "1.0.0" }
}
