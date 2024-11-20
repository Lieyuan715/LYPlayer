package com.example.lyplayer

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.compose.ui.Alignment


@Composable
fun FolderView(
    selectedFolderUri: Uri,
    onVideoClicked: (Uri) -> Unit,
    onPlaybackEnded: () -> Unit
) {
    val context = LocalContext.current
    var currentFolderUri by remember { mutableStateOf(selectedFolderUri) }
    val currentFolder = DocumentFile.fromTreeUri(context, currentFolderUri)

    var isPlaying by remember { mutableStateOf(false) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }

    if (isPlaying && selectedVideoUri != null) {
        VideoPlayer(
            videoUri = selectedVideoUri!!,
            onBackClicked = {
                isPlaying = false
                selectedVideoUri = null
                onPlaybackEnded() // 通知主页面停止播放
            },
            onPlaybackStateChanged = {}
        )
    } else if (currentFolder != null && currentFolder.isDirectory) {
        val folderItems = currentFolder.listFiles().toList()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = currentFolder.name ?: "Selected Folder",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (currentFolderUri != selectedFolderUri) {
                item {
                    Text(
                        text = "Back to Parent Folder",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                currentFolderUri = currentFolder.parentFile?.uri ?: selectedFolderUri
                            }
                    )
                }
            }

            items(folderItems) { file ->
                if (file.isDirectory) {
                    FolderItem(
                        name = file.name ?: "Unknown Folder",
                        onClick = { currentFolderUri = file.uri }
                    )
                } else if (file.name?.endsWith(".mp4") == true) {
                    FileItem(
                        name = file.name ?: "Unknown File",
                        onClick = {
                            selectedVideoUri = file.uri
                            isPlaying = true
                            onVideoClicked(file.uri)
                        }
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "The selected folder is empty or invalid.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}


@Composable
fun FolderItem(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FileItem(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

