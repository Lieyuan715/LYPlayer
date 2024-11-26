package com.example.lyplayer

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.io.File

@Composable
fun VideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit,
    onPlaybackStateChanged: (Boolean) -> Unit = {}
) {
    val DRAG_RATIO = 120_000f
    var videoTitle by remember { mutableStateOf("Unknown Video") }
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    val sharedPreferences = context.getSharedPreferences("PlayerPreferences", Context.MODE_PRIVATE)
    var isBackgroundPlayEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("isBackgroundPlayEnabled", false))
    }
    // 使用视频的 URI 字符串作为键值，单独存储每个视频的播放进度
    var lastPlaybackPosition by remember {
        mutableStateOf(sharedPreferences.getLong(videoUri.toString(), 0L))
    }

    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }


    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    var controlsVisible by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isUserInteracting by remember { mutableStateOf(false) }
    var isRightToolbarVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }
    val currentLifecycleObserver = remember { mutableStateOf<ExoPlayerLifecycleObserver?>(null) }

    // 更新 ExoPlayer 生命周期绑定
    fun updatePlayerLifecycle() {
        // 移除当前绑定的观察者（如果存在）
        currentLifecycleObserver.value?.let {
            activity?.lifecycle?.removeObserver(it)
        }

        // 创建新的观察者并绑定
        val newObserver = ExoPlayerLifecycleObserver(exoPlayer) { isBackgroundPlayEnabled }
        currentLifecycleObserver.value = newObserver

        // 添加生命周期观察者
        activity?.lifecycle?.addObserver(newObserver)
    }

    fun toggleBackgroundPlay(enabled: Boolean) {
        isBackgroundPlayEnabled = enabled
        sharedPreferences.edit().putBoolean("isBackgroundPlayEnabled", enabled).apply()
        updatePlayerLifecycle() // 更新生命周期观察者
    }

    LaunchedEffect(isBackgroundPlayEnabled) {
        updatePlayerLifecycle() // 仅更新生命周期绑定
    }

    // 在 DisposableEffect 中保存播放进度
    DisposableEffect(videoUri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
        exoPlayer.prepare()
        exoPlayer.seekTo(lastPlaybackPosition) // 恢复上次播放位置
        exoPlayer.playWhenReady = true // 默认开始播放

        val path = videoUri.path
        videoTitle = if (path != null) File(path).name else "Unknown Video"

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isPlaying = playbackState == Player.STATE_READY && exoPlayer.playWhenReady
                onPlaybackStateChanged(isPlaying)
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            sharedPreferences.edit().putLong(videoUri.toString(), exoPlayer.currentPosition).apply()
            if (!isBackgroundPlayEnabled) {
                exoPlayer.stop()
                exoPlayer.release()
            }
        }
    }

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

    fun startHideTimer() {
        hideJob?.cancel()
        hideJob = coroutineScope.launch {
            delay(10000)
            // 检查右侧工具栏状态，只有当其关闭时才隐藏上下工具栏
            if (!isUserInteracting && !isRightToolbarVisible) {
                controlsVisible = false
            }
        }
    }

    fun showControls() {
        controlsVisible = true
        startHideTimer() // 显示控件后启动计时器
    }


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

    val contextView = LocalView.current
    LaunchedEffect(Unit) {
        hideSystemBars(contextView)
        showControls()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black) // 设置背景颜色为黑色
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        exoPlayer.playWhenReady = !exoPlayer.playWhenReady // 双击切换播放/暂停
                        showControls() // 显示控件
                    },
                    onTap = {
                        controlsVisible = !controlsVisible // 单击显示/隐藏控件
                        if (controlsVisible) showControls()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isUserInteracting = true // 标记用户正在交互
                        controlsVisible = true // 显示控制栏
                    },
                    onDragEnd = {
                        isUserInteracting = false // 交互结束
                        startHideTimer() // 开启隐藏计时器
                    },
                    onDrag = { _, dragAmount ->
                        val totalDuration = exoPlayer.duration // 获取总时长
                        if (totalDuration > 0) {
                            // 根据拖动的距离计算跳转时间
                            val timeChange = (dragAmount.x / size.width) * DRAG_RATIO
                            val targetTime = (exoPlayer.currentPosition + timeChange.toLong())
                                .coerceIn(0, totalDuration) // 限制时间范围在有效区间
                            exoPlayer.seekTo(targetTime) // 跳转到目标时间
                            progress = targetTime.toFloat() / totalDuration // 更新进度条
                        }
                    }
                )
            }
    )
    {
        // 播放器视图
        AndroidView(
            factory = { context ->
                StyledPlayerView(context).apply {
                    player = exoPlayer
                    setUseController(false)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent) // 使播放器透明，避免完全遮挡
        )

        // 中央暂停提示
        if (!exoPlayer.playWhenReady) {
            var showPauseOverlay by remember { mutableStateOf(true) }
            val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

            // 自动隐藏提示框逻辑
            LaunchedEffect(Unit) {
                delay(1500) // 延迟3秒后隐藏提示框
                showPauseOverlay = false
            }

            if (showPauseOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f), // 设置 zIndex，确保提示显示在最顶层
                    contentAlignment = Alignment.Center // 居中对齐
                ) {
                    Box(
                        modifier = Modifier
                            .width(if (isLandscape) (screenWidth / 12).dp else (screenWidth / 6).dp)
                            .height(if (isLandscape) (screenHeight / 10).dp else (screenHeight / 24).dp)
                            .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                            .padding(4.dp), // 内边距
                        contentAlignment = Alignment.Center // 内容居中
                    ) {
                        Text(
                            text = "已暂停",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium // 文字样式
                        )
                    }
                }
            }
        }


        // 顶部工具栏
        if (controlsVisible) {
            TopControlBar(
                title = videoTitle,
                isRightToolbarVisible = isRightToolbarVisible,
                onToggleRightToolbar = { isRightToolbarVisible = !isRightToolbarVisible },
                onBackClicked = {
                    exoPlayer.stop()
                    exoPlayer.release()
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    onBackClicked()
                },
                isBackgroundPlayEnabled = isBackgroundPlayEnabled,
                onBackgroundPlayToggle = { enabled ->
                    toggleBackgroundPlay(enabled) // 动态更新后台播放状态
                },
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        // 下方工具栏仅在没有打开右侧工具栏时显示
        if (controlsVisible && !isRightToolbarVisible) {
            BottomControlBar(
                exoPlayer = exoPlayer, // 传递 exoPlayer 实例
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


