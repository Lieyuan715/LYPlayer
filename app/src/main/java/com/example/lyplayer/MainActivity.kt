package com.example.lyplayer
//库
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lyplayer.ui.theme.LYPlayerTheme
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ui.StyledPlayerView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex


//MainActivity 是应用的主要入口，继承自 ComponentActivity，用于管理 UI 和交互逻辑。
class MainActivity : ComponentActivity() {

    private var videoUri: Uri? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LYPlayerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (videoUri != null) {
                        VideoPlayer(
                            videoUri = videoUri!!,
                            modifier = Modifier.padding(innerPadding),
                            onBackClicked = {
                                videoUri = null // 重置 videoUri，返回选择视频文件界面
                            }
                        )
                    } else {
                        Button(
                            onClick = { selectVideoFile() },
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            Text(text = "选择视频文件")
                        }
                    }
                }
            }
        }
    }

    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            videoUri = uri
        } else {
            Toast.makeText(this, "未选择文件", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectVideoFile() {
        selectFileLauncher.launch(arrayOf("video/*"))
    }

    override fun onBackPressed() {
        if (videoUri != null) {
            videoUri = null  // 返回选择视频文件界面
        } else {
            super.onBackPressed()  // 确保调用父类的返回逻辑，退出应用
        }
    }

}


//这是用于播放视频的 Composable 函数。使用 ExoPlayer 播放选定的视频文件。
//DisposableEffect 确保在播放结束时释放播放器资源。通过 AndroidView 将 StyledPlayerView 嵌入到 Compose UI 中，展示视频播放界面。
@Composable
fun VideoPlayer(videoUri: Uri, modifier: Modifier = Modifier, onBackClicked: () -> Unit) {
    // 使用 ExoPlayer 播放器
    val context = androidx.compose.ui.platform.LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUri)
            setMediaItem(mediaItem)
            prepare()  // 确保已准备好
            playWhenReady = true  // 确保可以播放
        }
    }

    DisposableEffect(key1 = videoUri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
        exoPlayer.prepare()
        onDispose {
            exoPlayer.release()
        }
    }

    // 使用 Box 布局，放置返回按钮和视频播放器
    Box(modifier = modifier) {
        // 返回按钮，点击后会触发 onBackClicked
        IconButton(
            onClick = onBackClicked,
            modifier = Modifier
                .align(Alignment.TopStart) // 确保按钮在左上角
                .padding(16.dp)
                .zIndex(1f) // 设置按钮在上层显示，避免被视频播放器遮挡
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        // 使用 AndroidView 将 StyledPlayerView 嵌入到 Compose UI 中
        AndroidView(
            factory = {
                StyledPlayerView(it).apply {
                    player = exoPlayer
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = modifier
                .zIndex(0f) // 设置视频播放器在下层显示
        )
    }
}


//提供界面预览功能，方便在 Android Studio 中查看布局效果。在此代码中，仅显示了“选择一个视频文件”文本，实际上可以加入更多的 UI 组件和样式。
@Preview(showBackground = true)
@Composable
fun VideoPlayerPreview() {
    LYPlayerTheme {
        Text(text = "选择一个视频文件")
    }
}
