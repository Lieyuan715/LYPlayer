package com.example.lyplayer

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

enum class SortOption {
    NAME, DATE, SIZE, TYPE
}

@Composable
fun FolderView(
    selectedFolderUri: Uri,
    onVideoClicked: (Uri) -> Unit,
    onPlaybackEnded: () -> Unit,
    sortOption: SortOption
) {
    val context = LocalContext.current
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val containerHeight = (screenHeight / 10).dp  // 每个容器的高度为屏幕高度的十分之一

    // 用来管理当前文件夹路径
    var currentFolderUri by remember { mutableStateOf(selectedFolderUri) }
    var folderHistory by remember { mutableStateOf(listOf(selectedFolderUri)) }

    // 更新 currentFolderUri，当 selectedFolderUri 改变时同步更新
    LaunchedEffect(selectedFolderUri) {
        currentFolderUri = selectedFolderUri
    }

    // 确保返回时 currentFolderUri 和 folderHistory 同步
    LaunchedEffect(folderHistory) {
        if (folderHistory.isNotEmpty()) {
            currentFolderUri = folderHistory.last()
        }
    }

    // 获取当前文件夹并移除 primary: 前缀
    val currentFolder = DocumentFile.fromTreeUri(context, currentFolderUri)
    val currentFolderTitle = currentFolder?.name?.replace("primary:", "") ?: "选中的文件夹"

    var isPlaying by remember { mutableStateOf(false) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }

    val folderItems = currentFolder?.listFiles()?.sortedWith { file1, file2 ->
        when (sortOption) {
            SortOption.NAME -> {
                // 提取文件名并比较
                val name1 = file1.name ?: ""
                val name2 = file2.name ?: ""
                name1.compareTo(name2)
            }
            SortOption.DATE -> {
                // 比较修改时间
                file1.lastModified().compareTo(file2.lastModified())
            }
            SortOption.SIZE -> {
                // 比较文件大小
                file1.length().compareTo(file2.length())
            }
            SortOption.TYPE -> {
                // 提取并比较文件类型（后缀名）
                val type1 = file1.name?.substringAfterLast('.', "") ?: ""
                val type2 = file2.name?.substringAfterLast('.', "") ?: ""
                type1.compareTo(type2)
            }
        }
    } ?: emptyList()

    // 如果视频正在播放，显示视频播放器
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
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = currentFolderTitle,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 返回上级文件夹按钮
                    if (folderHistory.size > 1) {
                        Text(
                            text = "返回上级文件夹",
                            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable {
                                    folderHistory = folderHistory.dropLast(1)
                                }
                        )
                    }
                }
            }

            item {
                Divider(thickness = 1.dp)
            }

            items(folderItems) { file ->
                val fileDate = getFileDate(file)

                // 获取文件或文件夹的大小，如果是文件夹则计算总大小
                val rawFileSize = if (file.isDirectory) {
                    // 获取文件夹大小，返回 Long 类型
                    getFolderSize(file)
                } else {
                    // 获取文件大小，返回 Long 类型
                    getFileSize(file)
                }

                // 将 Long 类型的文件大小转换为 String 类型
                val formattedFileSize = formatSize(rawFileSize)  // 格式化为可读的文件大小字符串

                if (file.isDirectory) {
                    FolderItemWithIcon(
                        name = file.name ?: "未知文件夹",
                        isFolder = true,
                        icon = {
                            Image(
                                painter = painterResource(id = R.drawable.ic_folder),
                                contentDescription = "文件夹图标",
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        onClick = {
                            folderHistory = folderHistory + file.uri
                        },
                        containerHeight = containerHeight,
                        fileDate = fileDate,
                        fileSize = formattedFileSize  // 传递格式化后的文件大小
                    )
                } else {
                    val fileExtension = file.name?.substringAfterLast('.', "")?.lowercase() ?: ""
                    if (fileExtension in listOf("mp4", "avi", "mkv", "mov", "flv", "webm", "wmv", "mpeg", "mpg")) {
                        VideoItemWithFirstFrame(
                            name = file.name ?: "未知文件",
                            videoUri = file.uri,
                            onClick = {
                                selectedVideoUri = file.uri
                                isPlaying = true
                                onVideoClicked(file.uri)
                            },
                            containerHeight = containerHeight,
                            fileDate = fileDate,
                            fileSize = formattedFileSize  // 传递格式化后的文件大小
                        )
                    }
                }
            }


        }
    }
}

@Composable
fun FolderItemWithIcon(
    name: String,
    isFolder: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    containerHeight: Dp,
    fileDate: String? = null,
    fileSize: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(containerHeight)  // 设置固定高度
            .padding(8.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)  // 正方形图标的尺寸
                .padding(end = 8.dp)  // 图标和标题之间的间隔
        ) {
            icon()  // 渲染图标
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis  // 超出部分显示省略号
            )

            // 如果有文件日期和文件大小
            if (fileDate != null || fileSize != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (fileDate != null) {
                        Text(
                            text = fileDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    // 如果有文件日期，添加 '-'
                    if (fileDate != null && fileSize != null) {
                        Spacer(modifier = Modifier.width(4.dp))  // 添加间距
                        Text(
                            text = "-",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))  // 添加间距
                    }

                    if (fileSize != null) {
                        Text(
                            text = fileSize,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(top = 4.dp))  // 下划线
        }
    }
}

@Composable
fun VideoItemWithFirstFrame(
    name: String,
    videoUri: Uri,
    onClick: () -> Unit,
    containerHeight: Dp,  // 固定容器的高度
    fileDate: String? = null,  // 新增：文件日期
    fileSize: String? = null   // 新增：文件大小
) {
    var videoThumbnail by remember { mutableStateOf<ImageBitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // 异步加载视频的第一帧
    LaunchedEffect(videoUri) {
        try {
            // 异步加载视频的第一帧
            videoThumbnail = withContext(Dispatchers.IO) {
                getVideoFirstFrame(context = context, videoUri = videoUri)?.asImageBitmap() // 转换为ImageBitmap
            }
        } catch (e: Exception) {
            // 捕获异常，显示错误消息
            errorMessage = "加载缩略图失败: ${e.message}"
        }
    }

    FolderItemWithIcon(
        name = name,
        isFolder = false,
        icon = {
            // 如果获取到视频的缩略图，显示它
            videoThumbnail?.let {
                Image(
                    painter = BitmapPainter(it),
                    contentDescription = "视频缩略图",
                    modifier = Modifier
                        .fillMaxWidth()  // 宽度填满
                        .aspectRatio(1f)  // 强制宽高比为1，实现正方形
                        .padding(1.dp),  // 可选：添加一些边距
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                // 如果没有获取到视频缩略图，则显示默认占位图
                Image(
                    painter = painterResource(id = R.drawable.ic_video_placeholder),  // 使用XML矢量图作为占位图
                    contentDescription = "默认缩略图",
                    modifier = Modifier
                        .fillMaxWidth()  // 宽度填满
                        .aspectRatio(1f)  // 强制宽高比为1，实现正方形
                        .padding(1.dp),  // 可选：添加一些边距
                    contentScale = ContentScale.Crop
                )
            }
        },
        onClick = onClick,
        containerHeight = containerHeight,
        fileDate = fileDate,
        fileSize = fileSize
    )

    // 如果有错误消息，显示错误提示
    errorMessage?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(8.dp)
        )
    }
}

fun getFileDate(file: DocumentFile): String {
    val lastModified = file.lastModified()
    val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())  // 修改为只显示日期
    return date.format(java.util.Date(lastModified))
}

fun getFolderSize(folder: DocumentFile): Long {
    var totalSize: Long = 0L  // 显式声明为 Long 类型

    folder.listFiles()?.forEach { file ->
        totalSize += if (file.isDirectory) {
            // 如果是文件夹，递归计算文件夹大小
            getFolderSize(file)
        } else {
            // 如果是文件，直接加上文件大小
            file.length()  // file.length() 返回 Long 类型
        }
    }

    return totalSize  // 返回 Long 类型
}

fun getFileSize(file: DocumentFile): Long {
    return file.length()  // 返回 Long 类型
}


fun formatSize(size: Long): String {
    return when {
        size >= 1e9 -> "%.2f GB".format(size / 1e9)
        size >= 1e6 -> "%.2f MB".format(size / 1e6)
        size >= 1e3 -> "%.2f KB".format(size / 1e3)
        else -> "$size B"
    }
}

// 提取视频的第一帧
fun getVideoFirstFrame(context: Context, videoUri: Uri): Bitmap? {
    val retriever = MediaMetadataRetriever()
    var bitmap: Bitmap? = null
    try {
        retriever.setDataSource(context, videoUri)  // 使用 Context 和 Uri 设置数据源
        bitmap = retriever.getFrameAtTime(0)
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        retriever.release()
    }

    return bitmap
}
