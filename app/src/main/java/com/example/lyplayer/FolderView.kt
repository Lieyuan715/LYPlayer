/* 
 * 占位符注释 - FolderView.kt
 * 此文件包含文件夹视图相关功能
 */
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import android.util.LruCache
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color


// 文件缓存管理对象
object FileCache {
    // 缓存视频的第一帧，最多缓存1000个
    private val frameCache = LruCache<String, Bitmap>(1000)
    private val sizeCache = LruCache<String, String>(1000)  // 缓存文件大小
    private val dateCache = LruCache<String, String>(1000)  // 缓存文件修改时间

    // 缓存视频的第一帧
    fun getFrame(videoUri: String): Bitmap? = frameCache.get(videoUri)

    fun putFrame(videoUri: String, frame: Bitmap) {
        frameCache.put(videoUri, frame)
    }

    // 缓存文件大小
    fun getSize(fileUri: String): String? = sizeCache.get(fileUri)

    fun putSize(fileUri: String, size: String) {
        sizeCache.put(fileUri, size)
    }

    // 缓存文件修改时间
    fun getDate(fileUri: String): String? = dateCache.get(fileUri)

    fun putDate(fileUri: String, date: String) {
        dateCache.put(fileUri, date)
    }

    // 清除所有缓存
    fun clear() {
        frameCache.evictAll()
        sizeCache.evictAll()
        dateCache.evictAll()
    }
}

// 排序选项
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
    val containerHeight = (screenHeight / 10).dp  // 每个容器的高度为屏幕高度的十分之一

    // 用来管理当前文件夹路径
    var currentFolderUri by remember { mutableStateOf(selectedFolderUri) }
    var folderHistory by remember { mutableStateOf(listOf(selectedFolderUri)) }

    // 更新 currentFolderUri，当 selectedFolderUri 改变时同步更新
    LaunchedEffect(selectedFolderUri) {
        currentFolderUri = selectedFolderUri
        folderHistory = listOf(selectedFolderUri) // 每次选择新文件夹时重置历史记录，只保留当前文件夹
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

    // 排序文件夹内的文件
    val folderItems = currentFolder?.listFiles()?.sortedWith { file1, file2 ->
        when (sortOption) {
            SortOption.NAME -> file1.name?.compareTo(file2.name ?: "") ?: 0
            SortOption.DATE -> file1.lastModified().compareTo(file2.lastModified())
            SortOption.SIZE -> file1.length().compareTo(file2.length())
            SortOption.TYPE -> {
                val type1 = file1.name?.substringAfterLast('.', "")
                val type2 = file2.name?.substringAfterLast('.', "")

                // 先按类型排序，如果类型相同再按名称排序
                val typeComparison = type1?.compareTo(type2 ?: "") ?: 0
                if (typeComparison != 0) {
                    typeComparison // 如果类型不同，返回按类型的比较结果
                } else {
                    // 类型相同则按名称排序
                    file1.name?.compareTo(file2.name ?: "") ?: 0
                }
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
        // 显示文件夹内容列表
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // 如果文件夹历史记录中有上一级文件夹，显示返回按钮
                    if (folderHistory.size > 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart) // 按钮左对齐
                                .size(28.dp) // 设置按钮的大小
                        ) {
                            IconButton(
                                onClick = {
                                    folderHistory = folderHistory.dropLast(1)  // 返回上一级文件夹
                                },
                                modifier = Modifier.fillMaxSize() // 确保按钮填满整个区域
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "返回上级文件夹",
                                    tint = Color.Black // 设置按钮颜色为黑色
                                )
                            }
                        }
                    }

                    // 文件夹标题居中显示，超出部分显示省略号
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center) // 居中显示
                            .fillMaxWidth(0.8f) // 最大宽度为屏幕的80%
                    ) {
                        Text(
                            text = currentFolderTitle,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.align(Alignment.Center),  // 居中显示文本
                            maxLines = 1, // 限制为一行显示
                            overflow = TextOverflow.Ellipsis, // 超出部分使用省略号显示
                            color = Color.Black // 设置字体颜色为黑色
                        )
                    }
                }
            }

            // 分隔线
            item {
                Divider(thickness = 1.dp)
            }

            // 显示文件夹或文件项
            items(folderItems) { file ->
                // 获取文件的修改日期
                val fileDate = getFileDate(file)

                if (file.isDirectory) {
                    // 如果是文件夹，显示文件夹项，大小不计算
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
                            // 进入文件夹时将当前文件夹的 URI 添加到历史记录
                            folderHistory = folderHistory + file.uri
                        },
                        containerHeight = containerHeight,
                        fileDate = fileDate
                    )
                } else {
                    val fileExtension = file.name?.substringAfterLast('.', "")?.lowercase() ?: ""
                    if (fileExtension in listOf("mp4", "avi", "mkv", "mov", "flv", "webm", "wmv", "mpeg", "mpg")) {
                        // 如果是视频文件，显示视频项
                        VideoItemWithFirstFrame(
                            name = file.name ?: "未知文件",
                            videoUri = file.uri,
                            onClick = {
                                selectedVideoUri = file.uri
                                isPlaying = true
                                onVideoClicked(file.uri)
                            },
                            containerHeight = containerHeight,
                            fileDate = fileDate
                        )
                    }
                }
            }
        }
    }
}

// 文件夹项组件
@Composable
fun FolderItemWithIcon(
    name: String,
    isFolder: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    containerHeight: Dp,
    fileDate: String? = null
) {
    var fileDateState by remember { mutableStateOf(fileDate) }

    val context = LocalContext.current
    val fileUri = remember { Uri.parse(name) }

    // 使用 LaunchedEffect 来异步加载文件的大小和日期
    LaunchedEffect(fileUri) {
        if (fileDateState == null) {
            fileDateState = FileCache.getDate(fileUri.toString()) ?: getFileDateFromUri(context, fileUri)
            fileDateState?.let { FileCache.putDate(fileUri.toString(), it) }
        }
    }

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .height(containerHeight)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp)) {
                icon()
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    fileDateState?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Divider(modifier = Modifier.padding(top = 1.dp), thickness = 1.dp)
    }
}

@Composable
fun VideoItemWithFirstFrame(
    name: String,
    videoUri: Uri,
    onClick: () -> Unit,
    containerHeight: Dp,
    fileDate: String? = null,
    fileSize: String? = null
) {
    // 缓存第一帧、文件大小和日期
    var firstFrame by remember(videoUri) { mutableStateOf(FileCache.getFrame(videoUri.toString())) }
    var fileDateState by remember { mutableStateOf(fileDate) }
    var fileSizeState by remember { mutableStateOf(fileSize) }
    var fileExtension by remember { mutableStateOf("") } // 新增：用于存储文件扩展名

    val context = LocalContext.current
    val videoUriStr = videoUri.toString()

    // 使用 LaunchedEffect 来加载视频的第一帧
    LaunchedEffect(videoUriStr) {
        if (firstFrame == null) {
            firstFrame = getFirstFrameOfVideo(context, videoUri)
            firstFrame?.let {
                FileCache.putFrame(videoUriStr, it)
            }
        }

        if (fileSizeState == null) {
            fileSizeState = FileCache.getSize(videoUriStr) ?: formatSize(getFileSizeFromUri(context, videoUri))
            fileSizeState?.let { FileCache.putSize(videoUriStr, it) }
        }

        if (fileDateState == null) {
            fileDateState = FileCache.getDate(videoUriStr) ?: getFileDateFromUri(context, videoUri)
            fileDateState?.let { FileCache.putDate(videoUriStr, it) }
        }

        val extension = name.substringAfterLast('.', "").lowercase()
        fileExtension = extension
    }

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .height(containerHeight)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            firstFrame?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "视频封面",
                    modifier = Modifier
                        .size(48.dp)  // 设置为正方形
                        .padding(2.dp)
                        .clip(RoundedCornerShape(8.dp)),  // 圆角（可选）
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                // 如果没有解析到第一帧，则显示占位图
                Image(
                    painter = painterResource(id = R.drawable.ic_video_placeholder),
                    contentDescription = "视频占位图",
                    modifier = Modifier
                        .size(48.dp)
                        .padding(2.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    fileDateState?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(text = " - ", style = MaterialTheme.typography.bodySmall)
                    fileSizeState?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(text = " - ", style = MaterialTheme.typography.bodySmall)
                    Text(text = fileExtension.uppercase(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Divider(modifier = Modifier.padding(top = 1.dp), thickness = 1.dp)
    }
}


// 格式化大小为可读字符串
private fun formatSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1048576 -> String.format("%.2f KB", size / 1024.0)
        size < 1073741824 -> String.format("%.2f MB", size / 1048576.0)
        else -> String.format("%.2f GB", size / 1073741824.0)
    }
}

// 获取文件的修改时间
private fun getFileDate(file: DocumentFile): String? {
    return file.lastModified()?.let { formatDate(it) }
}

// 格式化日期
private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

// 异步获取文件的大小
private suspend fun getFileSizeFromUri(context: Context, uri: Uri): Long {
    return withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 0
        } catch (e: IOException) {
            0L
        }
    }
}

// 异步获取文件的修改日期
private suspend fun getFileDateFromUri(context: Context, uri: Uri): String? {
    return withContext(Dispatchers.IO) {
        try {
            val cursor = context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.MediaColumns.DATE_MODIFIED), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val timestamp = it.getLong(it.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATE_MODIFIED))
                    formatDate(timestamp * 1000)  // Unix 时间戳是秒，转换为毫秒
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

// 异步获取视频的第一帧
private suspend fun getFirstFrameOfVideo(context: Context, videoUri: Uri): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            retriever.frameAtTime
        } catch (e: Exception) {
            null
        }
    }
}
