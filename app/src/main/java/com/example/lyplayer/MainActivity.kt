package com.example.lyplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import com.example.lyplayer.ui.theme.LYPlayerTheme
import androidx.compose.ui.Alignment
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.SimpleExoPlayer
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationCompat

// SharedPreferences 管理代码
fun getSharedPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences("FolderPrefs", Context.MODE_PRIVATE)
}

fun saveSelectedFolderUri(context: Context, folderUri: Uri) {
    val sharedPreferences = getSharedPreferences(context)
    sharedPreferences.edit().apply {
        putString("selected_folder_uri", folderUri.toString())
        apply()  // 异步保存
    }
}

fun getSavedFolderUri(context: Context): Uri? {
    val sharedPreferences = getSharedPreferences(context)
    val uriString = sharedPreferences.getString("selected_folder_uri", null)
    return uriString?.let { Uri.parse(it) }  // 如果保存了路径，返回 Uri
}

fun saveSortOption(context: Context, sortOption: SortOption) {
    val sharedPreferences = getSharedPreferences(context)
    sharedPreferences.edit().apply {
        putString("sort_option", sortOption.name)  // 保存排序选项的名称
        apply()  // 异步保存
    }
}

fun getSavedSortOption(context: Context): SortOption {
    val sharedPreferences = getSharedPreferences(context)
    val savedOption = sharedPreferences.getString("sort_option", SortOption.TYPE.name)  // 默认排序按类型
    return SortOption.valueOf(savedOption ?: SortOption.TYPE.name)
}

class MainActivity : ComponentActivity() {

    private var selectedFolderUri: Uri? by mutableStateOf(null)
    private val REQUEST_CODE = 1001  // 请求码

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取上次选择的文件夹 URI
        selectedFolderUri = getSavedFolderUri(this)

        // 请求存储权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
        }

        // 文件夹选择器
        val folderPickerLauncher: ActivityResultLauncher<Uri?> = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                selectedFolderUri = uri // 记录选择的文件夹 URI
                saveSelectedFolderUri(this, uri) // 保存选中的文件夹 URI
            }
        }

        setContent {
            LYPlayerTheme {
                var isPlaying by remember { mutableStateOf(false) }
                var title by remember { mutableStateOf("LYPlayer") } // 初始标题

                // 从 SharedPreferences 获取并设置排序选项
                var sortOption by remember { mutableStateOf(getSavedSortOption(this)) } // 加载保存的排序方式

                val currentFolderTitle = selectedFolderUri?.lastPathSegment?.replace("primary:", "根文件夹:") ?: title

                Scaffold(
                    topBar = {
                        if (!isPlaying) { // 播放器界面不显示顶部栏
                            CustomTopBar(
                                title = currentFolderTitle,
                                onSettingsClicked = {
                                    // 处理设置点击事件
                                },
                                onSortOptionSelected = { selectedSortOption ->
                                    sortOption = selectedSortOption  // 更新排序选项
                                    saveSortOption(this, selectedSortOption)  // 保存到 SharedPreferences
                                },
                                selectedSortOption = sortOption // 将 sortOption 作为 selectedSortOption 传递
                            )
                        }
                    },
                    floatingActionButton = {
                        if (!isPlaying) { // 播放器界面不显示悬浮按钮
                            FloatingActionButton(
                                onClick = { folderPickerLauncher.launch(selectedFolderUri ?: Uri.EMPTY) },
                                modifier = Modifier.padding(16.dp),
                                containerColor = Color.Black,
                                contentColor = Color.White,
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add Folder")
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(Color.White)
                    ) {
                        // 这里使用 LaunchedEffect 来确保每次 selectedFolderUri 更新时重新加载内容
                        LaunchedEffect(selectedFolderUri) {
                            // 每当 selectedFolderUri 发生变化时重新刷新内容
                        }

                        // 调用 FolderView 时，要确保它在 Composable 函数中被调用
                        selectedFolderUri?.let { folderUri ->
                            FolderView(
                                selectedFolderUri = folderUri,
                                onVideoClicked = { videoUri ->
                                    isPlaying = true
                                    val serviceIntent = Intent(this@MainActivity, VideoPlayerService::class.java)
                                    startService(serviceIntent)
                                },
                                onPlaybackEnded = {
                                    isPlaying = false
                                    stopService(Intent(this@MainActivity, VideoPlayerService::class.java))
                                },
                                sortOption = sortOption // 将排序选项传递给 FolderView
                            )
                        } ?: run {
                            // 如果没有文件夹选择，显示提示文本
                            Text(
                                text = "点击 '+' 按钮选择一个文件夹。",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }

    // 处理权限请求的结果
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "存储权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "存储权限被拒绝", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class VideoPlayerService : Service() {

    private var player: SimpleExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        // 初始化 ExoPlayer
        player = SimpleExoPlayer.Builder(this).build()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // 创建通知渠道（仅在 Android 8.0 及以上版本需要）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "video_player_channel"
            val channelName = "Video Player Notifications"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // 创建前台通知
        val notification: Notification = NotificationCompat.Builder(this, "video_player_channel")
            .setContentTitle("视频正在播放")
            .setContentText("您的视频正在后台播放。")
            .setSmallIcon(android.R.drawable.ic_media_play)  // 使用系统自带的图标
            .build()

        // 启动前台服务
        startForeground(1, notification)

        // 返回START_STICKY，表示服务将在后台运行直到明确停止
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放 ExoPlayer 资源
        player?.release()
        player = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null  // 不需要绑定
    }
}
