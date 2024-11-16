package com.example.lyplayer

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lyplayer.ui.theme.LYPlayerTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add


//主界面内容
@Preview(showBackground = true)
@Composable
fun VideoPlayerPreview() {
    LYPlayerTheme {
        var isVideoPlaying by remember { mutableStateOf(false) } // 初始状态为非播放界面

        if (isVideoPlaying) {
            // 视频播放界面
            VideoPlayer(
                videoUri = Uri.EMPTY, // 视频播放的 URI（实际使用时传递有效 URI）
                onBackClicked = { isVideoPlaying = false }, // 返回切换到主界面
                onPlaybackStateChanged = { /* 播放状态变化逻辑 */ }
            )
        } else {
            // 主界面内容
            Scaffold(
                topBar = {
                    CustomTopBar() // 显示顶部栏
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { isVideoPlaying = true }, // 切换到视频播放界面
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add")
                    }
                }
            ) { innerPadding ->
                // 主界面具体内容
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background) // 使用主题背景色
                ) {
                    // 主界面其余部分（空白或其他内容）
                }
            }
        }
    }
}
