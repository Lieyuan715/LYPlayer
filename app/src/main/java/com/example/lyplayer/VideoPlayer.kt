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
import kotlinx.coroutines.delay

@Composable
fun VideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit, // 返回按钮的回调
    onPlaybackStateChanged: (Boolean) -> Unit // 播放状态变化回调
) {
    val context = LocalContext.current

    // 创建并管理 ExoPlayer 实例
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    // 控制栏可见性
    var controlsVisible by remember { mutableStateOf(true) }

    // 播放进度
    var progress by remember { mutableFloatStateOf(0f) }

    // 用户交互状态
    var isUserInteracting by remember { mutableStateOf(false) }

    // 协程作用域
    val coroutineScope = rememberCoroutineScope()

    // 播放器初始化与清理
    DisposableEffect(videoUri) {
        // 设置媒体资源并准备播放器
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        // 添加播放状态监听器
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                // 播放状态发生变化时，调用回调
                onPlaybackStateChanged(playbackState == Player.STATE_READY && exoPlayer.playWhenReady)
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            // 移除监听器并释放播放器资源
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // 更新播放进度，每500毫秒刷新一次
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

    // 显示控制栏并设置自动隐藏逻辑
    fun showControls() {
        controlsVisible = true
        coroutineScope.launch {
            delay(5000)
            if (!isUserInteracting) { // 仅在用户未交互时隐藏
                controlsVisible = false
            }
        }
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

    // 在组件加载时隐藏系统栏
    val contextView = LocalView.current
    LaunchedEffect(true) {
        hideSystemBars(contextView)
    }

    // 主界面布局
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                // 点击切换控制栏显示状态
                controlsVisible = !controlsVisible
                if (controlsVisible) {
                    showControls()
                }
            }
    ) {
        // 使用 AndroidView 嵌入 ExoPlayer 的 StyledPlayerView
        AndroidView(
            factory = { context ->
                StyledPlayerView(context).apply {
                    player = exoPlayer
                    setUseController(false) // 禁用默认控制器
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 顶部控制栏，显示时可见
        if (controlsVisible) {
            TopControlBar(
                title = "Playing Video",
                onBackClicked = onBackClicked,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        // 底部控制栏，显示时可见
        if (controlsVisible) {
            BottomControlBar(
                isPlaying = exoPlayer.playWhenReady,
                onPlayPause = {
                    // 切换播放/暂停状态
                    exoPlayer.playWhenReady = !exoPlayer.playWhenReady
                    showControls() // 显示控制栏
                },
                onNext = { /* 下一集逻辑（未实现） */ },
                onPrevious = { /* 上一集逻辑（未实现） */ },
                progress = progress,
                totalDuration = exoPlayer.duration,
                onProgressChanged = { newProgress, isUserDragging ->
                    isUserInteracting = isUserDragging // 更新用户交互状态
                    if (!isUserDragging) {
                        // 用户完成拖动时更新播放位置
                        exoPlayer.seekTo((exoPlayer.duration * newProgress).toLong())
                        isUserInteracting = false // 交互结束后重置
                    }
                },
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}
