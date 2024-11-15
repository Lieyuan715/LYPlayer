package com.example.lyplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lyplayer.ui.theme.LYPlayerTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

class MainActivity : ComponentActivity() {

    private var videoUri: Uri? by mutableStateOf(null) // 存储选中的视频 URI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 注册文件选择器的启动器
        val selectFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                videoUri = uri
            } else {
                Toast.makeText(this, "未选择文件", Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            LYPlayerTheme {
                var isPlaying by remember { mutableStateOf(false) } // 控制是否全屏的状态

                Scaffold(
                    topBar = {
                        if (!isPlaying) { // 如果不是播放状态，则显示顶部栏
                            CustomTopBar()
                        }
                    },
                    floatingActionButton = {
                        if (!isPlaying) { // 如果不是播放状态，则显示悬浮按钮
                            FloatingActionButton(
                                onClick = { selectFileLauncher.launch(arrayOf("video/*")) },
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add")
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        if (videoUri != null) {
                            // 播放视频并监听播放状态
                            VideoPlayer(
                                videoUri = videoUri!!,
                                modifier = Modifier.fillMaxSize(), // 视频全屏显示
                                onBackClicked = { videoUri = null }, // 返回主界面
                                onPlaybackStateChanged = { isPlaying = it } // 更新播放状态
                            )
                        }
                    }
                }
            }
        }
    }
}

