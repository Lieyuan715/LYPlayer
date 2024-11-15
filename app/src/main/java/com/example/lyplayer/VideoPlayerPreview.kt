package com.example.lyplayer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lyplayer.ui.theme.LYPlayerTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

@Preview(showBackground = true)
@Composable
fun VideoPlayerPreview() {
    LYPlayerTheme {
        // Scaffold 是用于构建界面的容器，提供了顶部栏、底部栏、浮动按钮等功能
        Scaffold(
            topBar = {
                //CustomTopBar的具体内容在CustomTopBar.kt
                CustomTopBar()
            },
            floatingActionButton = {
                // 右下角的加号按钮
                FloatingActionButton(
                    onClick = { /* 打开文件管理系统 */ },
                    modifier = Modifier.padding(16.dp) // 按钮与屏幕边缘留白 16dp
                ) {
                    // 加号按钮的图标
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }
        ) { innerPadding ->
            // 内容部分，内嵌的 Box 用于展示视频播放器内容
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
//                VideoPlayer(
//                    videoUri = Uri.parse("https://www.example.com/sample.mp4"), // 这里可以用实际的视频 URI
//                    onBackClicked = { /* 返回按钮点击时的操作（目前为预览，不执行）*/ }
//                )
            }
        }
    }
}

