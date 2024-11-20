package com.example.lyplayer

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lyplayer.ui.theme.LYPlayerTheme
import java.io.File
import androidx.compose.ui.Alignment


@Preview(showBackground = true)
@Composable
fun VideoPlayerPreview() {
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) } // 当前选择的视频 URI
    var currentFolder by remember { mutableStateOf<File?>(null) } // 当前文件夹
    val rootFolder = File("/storage/emulated/0") // 根文件夹路径（根据实际情况调整路径）

    LYPlayerTheme {
        Scaffold(
            topBar = {
                CustomTopBar(
                    title = currentFolder?.name ?: "LYPlayer", // 动态显示当前文件夹名
                    onSettingsClicked = { /* 打开设置的逻辑 */ }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        currentFolder = rootFolder // 打开根文件夹供用户选择
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Folder")
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when {
                    selectedVideoUri != null -> {
                        // 播放视频界面
                        VideoPlayer(
                            videoUri = selectedVideoUri!!,
                            onBackClicked = { selectedVideoUri = null }, // 返回到文件夹浏览界面
                            onPlaybackStateChanged = { /* 播放状态变化逻辑 */ }
                        )
                    }
                    currentFolder != null -> {
                        // 显示文件夹内容
                        FolderContent(
                            currentFolder = currentFolder!!,
                            onFolderClicked = { folder -> currentFolder = folder }, // 进入子文件夹
                            onVideoClicked = { videoUri -> selectedVideoUri = videoUri } // 播放选定视频
                        )
                    }
                    else -> {
                        // 空白区域，等待用户选择文件夹
                        Text(
                            text = "No folder selected. Click the '+' button to choose one.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun FolderContent(
    currentFolder: File,
    onFolderClicked: (File) -> Unit,
    onVideoClicked: (Uri) -> Unit
) {
    val files = currentFolder.listFiles()?.sortedBy { it.isFile } ?: emptyList()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 返回上一级文件夹按钮
        if (currentFolder.parentFile != null) {
            item {
                Text(
                    text = "⬆️ Go to Parent Folder",
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { onFolderClicked(currentFolder.parentFile!!) }
                )
            }
        }

        // 显示当前文件夹内容
        items(files) { file ->
            if (file.isDirectory) {
                Text(
                    text = "📁 ${file.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { onFolderClicked(file) }
                )
            } else if (file.name.endsWith(".mp4", ignoreCase = true)) {
                Text(
                    text = "🎥 ${file.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { onVideoClicked(Uri.fromFile(file)) }
                )
            }
        }
    }
}






