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
    // 设置拖动快进的比例
    val DRAG_RATIO = 120_000f
    var videoTitle by remember { mutableStateOf("Unknown Video") } // 视频标题
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp // 屏幕宽度
    val screenHeight = configuration.screenHeightDp // 屏幕高度

    // 获取 SharedPreferences 用于保存后台播放设置和播放位置
    val sharedPreferences = context.getSharedPreferences("PlayerPreferences", Context.MODE_PRIVATE)
    var isBackgroundPlayEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("isBackgroundPlayEnabled", false))
    }

    var lastPlaybackPosition by remember {
        mutableStateOf(sharedPreferences.getLong(videoUri.toString(), 0L))
    }

    // 设置横屏显示
    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    val exoPlayer = remember { ExoPlayer.Builder(context).build() } // 创建 ExoPlayer 实例
    var controlsVisible by remember { mutableStateOf(true) } // 控制栏是否可见
    var progress by remember { mutableFloatStateOf(0f) } // 播放进度
    var isUserInteracting by remember { mutableStateOf(false) } // 用户是否交互
    var isRightToolbarVisible by remember { mutableStateOf(false) } // 右侧工具栏是否可见
    val coroutineScope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) } // 隐藏控制栏的定时任务
    val currentLifecycleObserver = remember { mutableStateOf<ExoPlayerLifecycleObserver?>(null) } // 监听生命周期变化

    // 更新 ExoPlayer 的生命周期观察者
    fun updatePlayerLifecycle() {
        currentLifecycleObserver.value?.let {
            activity?.lifecycle?.removeObserver(it) // 移除旧的观察者
        }

        val newObserver = ExoPlayerLifecycleObserver(exoPlayer) { isBackgroundPlayEnabled } // 创建新的观察者
        currentLifecycleObserver.value = newObserver

        activity?.lifecycle?.addObserver(newObserver) // 添加新的生命周期观察者
    }

    // 切换后台播放设置
    fun toggleBackgroundPlay(enabled: Boolean) {
        isBackgroundPlayEnabled = enabled
        sharedPreferences.edit().putBoolean("isBackgroundPlayEnabled", enabled).apply()
        updatePlayerLifecycle() // 更新生命周期观察者
    }

    LaunchedEffect(isBackgroundPlayEnabled) {
        updatePlayerLifecycle() // 每次设置改变时更新观察者
    }

    // 初始化播放器并设置监听
    DisposableEffect(videoUri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri)) // 设置媒体文件
        exoPlayer.prepare() // 准备播放
        exoPlayer.seekTo(lastPlaybackPosition) // 设置播放位置
        exoPlayer.playWhenReady = true // 自动播放

        // 获取视频文件名
        val path = videoUri.path
        videoTitle = if (path != null) File(path).name else "Unknown Video"

        // 监听播放状态变化
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isPlaying = playbackState == Player.STATE_READY && exoPlayer.playWhenReady
                onPlaybackStateChanged(isPlaying) // 传递播放状态变化
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener) // 释放监听器
            sharedPreferences.edit().putLong(videoUri.toString(), exoPlayer.currentPosition).apply() // 保存播放进度
            if (!isBackgroundPlayEnabled) {
                exoPlayer.stop() // 停止播放器
                exoPlayer.release() // 释放资源
            }
        }
    }

    // 定期更新播放进度
    LaunchedEffect(exoPlayer) {
        while (true) {
            progress = if (exoPlayer.duration > 0) {
                exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat() // 计算播放进度
            } else {
                0f
            }
            delay(500) // 每500毫秒更新一次进度
        }
    }

    // 启动隐藏控制栏的定时器
    fun startHideTimer() {
        hideJob?.cancel() // 取消上一个定时任务
        hideJob = coroutineScope.launch {
            delay(10000) // 10秒后隐藏控制栏
            if (!isUserInteracting && !isRightToolbarVisible) {
                controlsVisible = false // 用户没有交互且右侧工具栏不可见时，隐藏控制栏
            }
        }
    }

    // 显示控制栏
    fun showControls() {
        controlsVisible = true
        startHideTimer() // 显示控制栏后启动隐藏计时器
    }

    // 隐藏系统状态栏
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

    // 隐藏系统状态栏并显示控制栏
    val contextView = LocalView.current
    LaunchedEffect(Unit) {
        hideSystemBars(contextView)
        showControls()
    }

    // 主布局：播放器视图 + 控制栏
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        exoPlayer.playWhenReady = !exoPlayer.playWhenReady // 双击切换播放/暂停
                        showControls()
                    },
                    onTap = {
                        controlsVisible = !controlsVisible // 单击切换控制栏可见性
                        if (controlsVisible) showControls()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isUserInteracting = true
                        controlsVisible = true // 开始拖动时显示控制栏
                    },
                    onDragEnd = {
                        isUserInteracting = false
                        startHideTimer() // 拖动结束后启动隐藏控制栏计时器
                    },
                    onDrag = { _, dragAmount ->
                        val totalDuration = exoPlayer.duration
                        if (totalDuration > 0) {
                            val timeChange = (dragAmount.x / size.width) * DRAG_RATIO
                            val targetTime = (exoPlayer.currentPosition + timeChange.toLong())
                                .coerceIn(0, totalDuration)
                            exoPlayer.seekTo(targetTime) // 快进或快退
                            progress = targetTime.toFloat() / totalDuration
                        }
                    }
                )
            }
    ) {
        // 播放器视图
        AndroidView(
            factory = { context ->
                StyledPlayerView(context).apply {
                    player = exoPlayer
                    setUseController(false) // 不显示默认控制器
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        )

        // 中央暂停提示
        if (!exoPlayer.playWhenReady) {
            var showPauseOverlay by remember { mutableStateOf(true) }
            val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

            LaunchedEffect(Unit) {
                delay(1500)
                showPauseOverlay = false // 1.5秒后隐藏暂停提示
            }

            if (showPauseOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(if (isLandscape) (screenWidth / 12).dp else (screenWidth / 6).dp)
                            .height(if (isLandscape) (screenHeight / 10).dp else (screenHeight / 24).dp)
                            .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "已暂停",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
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
                    exoPlayer.stop() // 停止播放并释放播放器资源
                    exoPlayer.release()
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED // 恢复屏幕方向
                    onBackClicked() // 返回时触发的回调
                },
                isBackgroundPlayEnabled = isBackgroundPlayEnabled, // 是否启用后台播放
                onBackgroundPlayToggle = { enabled -> toggleBackgroundPlay(enabled) }, // 切换后台播放状态
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        // 下方工具栏，仅在没有打开右侧工具栏时显示
        if (controlsVisible && !isRightToolbarVisible) {
            BottomControlBar(
                exoPlayer = exoPlayer,
                isPlaying = exoPlayer.playWhenReady, // 播放状态
                onPlayPause = {
                    exoPlayer.playWhenReady = !exoPlayer.playWhenReady // 播放/暂停切换
                    showControls()
                },
                onNext = { /* 下一集逻辑 */ },
                onPrevious = { /* 上一集逻辑 */ },
                progress = progress, // 当前进度
                totalDuration = exoPlayer.duration, // 视频总时长
                onProgressChanged = { newProgress, isUserDragging ->
                    val targetTime = (exoPlayer.duration * newProgress).toLong() // 根据拖动的进度更新播放时间
                    exoPlayer.seekTo(targetTime) // 跳转到新的播放位置
                    isUserInteracting = isUserDragging // 标记用户是否正在拖动进度条
                    if (!isUserInteracting) {
                        startHideTimer() // 拖动结束后启动隐藏控制栏计时器
                    }
                },
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}




