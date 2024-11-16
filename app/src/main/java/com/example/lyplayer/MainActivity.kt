package com.example.lyplayer

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lyplayer.ui.theme.LYPlayerTheme


//主界面
class MainActivity : ComponentActivity() {

    private var videoUri: Uri? by mutableStateOf(null) // 存储选中的视频 URI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                videoUri = uri
            }
        }

        setContent {
            LYPlayerTheme {
                var isPlaying by remember { mutableStateOf(false) }

                // 动态检测屏幕方向并调整布局
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val isLandscape = videoUri != null

                // 设置屏幕方向
                LaunchedEffect(isLandscape) {
                    requestedOrientation = if (isLandscape) {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE // 锁定横屏
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT // 锁定竖屏
                    }
                }

                Scaffold(
                    floatingActionButton = {
                        if (videoUri == null) {
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
                            // 视频播放界面
                            VideoPlayer(
                                videoUri = videoUri!!,
                                modifier = Modifier.fillMaxSize(),
                                onBackClicked = { videoUri = null },
                                onPlaybackStateChanged = { isPlaying = it }
                            )
                        } else {
                            // 主界面
                            VideoPlayerPreview()
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (videoUri != null) {
            videoUri = null // 清空视频 URI
        } else {
            super.onBackPressed() // 执行默认的返回逻辑
        }
    }
}

