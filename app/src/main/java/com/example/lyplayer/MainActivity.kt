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
                var isPlaying by remember { mutableStateOf(false) }

                // 根据 `videoUri` 控制界面内容和组件显示
                val showTopBar = videoUri == null
                val showFab = videoUri == null

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
                                onBackClicked = { videoUri = null }, // 清空 URI
                                onPlaybackStateChanged = { isPlaying = it } // 更新播放状态
                            )
                        } else {
                            // 主界面预览
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
