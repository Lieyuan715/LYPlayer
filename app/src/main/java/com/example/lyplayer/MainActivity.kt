package com.example.lyplayer

import android.content.Intent
import android.net.Uri
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
import androidx.core.app.NotificationCompat
import com.google.android.exoplayer2.SimpleExoPlayer
import android.app.Service


class MainActivity : ComponentActivity() {

    private var selectedFolderUri: Uri? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 文件夹选择器
        val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                selectedFolderUri = uri // 记录选择的文件夹 URI
            }
        }

        setContent {
            LYPlayerTheme {
                var scaffoldTitle by remember { mutableStateOf("LYPlayer") } // Scaffold 动态标题
                var isPlaying by remember { mutableStateOf(false) } // 播放器状态

                Scaffold(
                    topBar = {
                        if (!isPlaying) { // 播放器界面不显示顶部栏
                            CustomTopBar(
                                title = scaffoldTitle,
                                onSettingsClicked = { /* 打开设置的逻辑 */ }
                            )
                        }
                    },
                    floatingActionButton = {
                        if (!isPlaying) { // 播放器界面不显示悬浮按钮
                            FloatingActionButton(
                                onClick = { folderPickerLauncher.launch(null) }, // 打开文件夹选择器
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add Folder")
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        if (selectedFolderUri != null) {
                            FolderView(
                                selectedFolderUri = selectedFolderUri!!,
                                onVideoClicked = { videoUri ->
                                    isPlaying = true // 进入播放器

                                    // 启动前台服务以确保视频在后台播放
                                    val serviceIntent = Intent(this@MainActivity, VideoPlayerService::class.java)
                                    startService(serviceIntent)
                                },
                                onPlaybackEnded = {
                                    isPlaying = false // 播放结束时返回

                                    // 停止前台服务
                                    stopService(Intent(this@MainActivity, VideoPlayerService::class.java))
                                }
                            )
                        } else {
                            Text(
                                text = "No folder selected. Click the '+' button to choose one.",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
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
            .setContentTitle("Video is playing")
            .setContentText("Your video is playing in the background.")
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