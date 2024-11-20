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
                                },
                                onPlaybackEnded = {
                                    isPlaying = false // 播放结束时返回
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
