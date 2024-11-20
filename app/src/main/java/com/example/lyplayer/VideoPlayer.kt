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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import java.io.File

@Composable
fun VideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit,
    onPlaybackStateChanged: (Boolean) -> Unit = {}
) {
    // 拖动比例系数：100%的屏幕等于两分钟
    val DRAG_RATIO = 120_000f

    var videoTitle by remember { mutableStateOf("Unknown Video") } // 视频名称

    val context = LocalContext.current
    val activity = context as? Activity

    // 设置横屏
    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    // 创建并管理 ExoPlayer 实例
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    // 控制栏可见性状态
    var controlsVisible by remember { mutableStateOf(true) }

    // 播放进度状态
    var progress by remember { mutableFloatStateOf(0f) }

    // 用户交互状态
    var isUserInteracting by remember { mutableStateOf(false) }

    // 协程作用域与隐藏任务句柄
    val coroutineScope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }

    // 初始化播放器并监听状态变化
    DisposableEffect(videoUri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        // 获取文件名作为视频标题
        val path = videoUri.path
        videoTitle = if (path != null) {
            File(path).name // 提取文件名
        } else {
            "Unknown Video"
        }

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isPlaying = playbackState == Player.STATE_READY && exoPlayer.playWhenReady
                onPlaybackStateChanged(isPlaying)
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()

            // 恢复竖屏
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // 更新播放进度逻辑
    LaunchedEffect(exoPlayer) {
        while (true) {
            progress = if (exoPlayer.duration > 0) {
                exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat()
            } else {
                0f
            }
            delay(500)
        }
    }

    // 自动隐藏逻辑
    fun startHideTimer() {
        hideJob?.cancel()
        hideJob = coroutineScope.launch {
            delay(5000)
            if (!isUserInteracting) {
                controlsVisible = false
            }
        }
    }

    // 显示控制栏并重启隐藏逻辑
    fun showControls() {
        controlsVisible = true
        startHideTimer()
    }

    // 隐藏系统状态栏和导航栏
    fun hideSystemBars(view: View) {
        val activity = view.context as? Activity ?: return
        val window = activity.window

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    // 隐藏系统栏
    val contextView = LocalView.current
    LaunchedEffect(Unit) {
        hideSystemBars(contextView)
        showControls()
    }

    // 视频播放器主布局
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isUserInteracting = true
                        controlsVisible = true
                    },
                    onDragEnd = {
                        isUserInteracting = false
                        startHideTimer()
                    },
                    onDrag = { _, dragAmount ->
                        val totalDuration = exoPlayer.duration
                        if (totalDuration > 0) {
                            val timeChange = (dragAmount.x / size.width) * DRAG_RATIO
                            val targetTime = (exoPlayer.currentPosition + timeChange.toLong())
                                .coerceIn(0, totalDuration)
                            exoPlayer.seekTo(targetTime)
                            progress = targetTime.toFloat() / totalDuration
                        }
                    }
                )
            }
            .clickable {
                controlsVisible = !controlsVisible
                if (controlsVisible) showControls()
            }
    ) {
        AndroidView(
            factory = { context ->
                StyledPlayerView(context).apply {
                    player = exoPlayer
                    setUseController(false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (controlsVisible) {
            TopControlBar(
                title = videoTitle,
                onBackClicked = {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    onBackClicked()
                },
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        if (controlsVisible) {
            BottomControlBar(
                isPlaying = exoPlayer.playWhenReady,
                onPlayPause = {
                    exoPlayer.playWhenReady = !exoPlayer.playWhenReady
                    showControls()
                },
                onNext = { /* 下一集逻辑 */ },
                onPrevious = { /* 上一集逻辑 */ },
                progress = progress,
                totalDuration = exoPlayer.duration,
                onProgressChanged = { newProgress, isUserDragging ->
                    val targetTime = (exoPlayer.duration * newProgress).toLong()
                    exoPlayer.seekTo(targetTime)
                    isUserInteracting = isUserDragging
                    if (!isUserInteracting) {
                        startHideTimer()
                    }
                },
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}
