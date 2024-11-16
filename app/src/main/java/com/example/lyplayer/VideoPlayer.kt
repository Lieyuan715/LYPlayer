package com.example.lyplayer

import android.net.Uri
import android.os.Build
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.launch
import android.app.Activity
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.foundation.background

@Composable
fun VideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit,
    onPlaybackStateChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current

    // 创建 ExoPlayer 实例
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    // 控制条显示状态
    var controlsVisible by remember { mutableStateOf(true) }

    // 进度状态
    var progress by remember { mutableFloatStateOf(0f) }

    // 自动隐藏协程
    val coroutineScope = rememberCoroutineScope()

    // 每次 videoUri 更新时重新加载媒体
    DisposableEffect(videoUri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                onPlaybackStateChanged(playbackState == Player.STATE_READY && exoPlayer.playWhenReady)
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // 定时更新播放进度
    LaunchedEffect(exoPlayer) {
        while (true) {
            progress = if (exoPlayer.duration > 0) {
                exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat()
            } else {
                0f
            }
            kotlinx.coroutines.delay(500) // 每 500 毫秒更新一次进度
        }
    }

    // 控制条自动隐藏逻辑
    fun showControls() {
        controlsVisible = true
        coroutineScope.launch {
            kotlinx.coroutines.delay(5000) // 5秒后隐藏
            controlsVisible = false
        }
    }

    // 永久隐藏状态栏和导航栏，设置为更快的动画
    fun hideSystemBars(view: View) {
        val activity = view.context as? Activity ?: return
        val window = activity.window

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 及以上使用 WindowInsetsController
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars()) // 直接隐藏状态栏和导航栏
                // 设置为更快的行为以减少渐变效果的时间
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 及以下使用系统 UI flags
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // 直接隐藏状态栏和导航栏
                    )
        }
    }

    // 获取当前视图并更新状态栏显示状态
    val contextView = LocalView.current
    LaunchedEffect(true) {
        hideSystemBars(contextView)  // 立即隐藏
    }

    // 布局
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                // 切换控制条显示状态
                controlsVisible = !controlsVisible
                if (controlsVisible) {
                    showControls()
                }
            }
    ) {
        // 视频播放器
        AndroidView(
            factory = { context ->
                StyledPlayerView(context).apply {
                    player = exoPlayer
                    setUseController(false)
                }
            },
            modifier = Modifier.fillMaxSize()  // 确保播放器填充整个屏幕
        )

        // 顶部工具栏
        if (controlsVisible) {
            TopControlBar(
                title = "Playing Video",
                onBackClicked = onBackClicked,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        // 底部控制栏
        if (controlsVisible) {
            BottomControlBar(
                isPlaying = exoPlayer.playWhenReady,
                onPlayPause = { exoPlayer.playWhenReady = !exoPlayer.playWhenReady },
                onNext = { /* 下一集逻辑 */ },
                onPrevious = { /* 上一集逻辑 */ },
                progress = { progress },
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}
