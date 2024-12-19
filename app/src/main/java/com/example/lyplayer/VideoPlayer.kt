package com.example.lyplayer

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import java.io.File
import kotlin.math.absoluteValue


@Composable
fun VideoPlayer(
    videoUri: Uri,
    onBackClicked: () -> Unit,
    onPlaybackStateChanged: (Boolean) -> Unit = {}
) {
    // 设置拖动快进的比例
    val DRAG_RATIO = 120_000f

    var videoTitle by remember { mutableStateOf("Unknown Video") } // 视频标题
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // 获取 SharedPreferences 用于保存后台播放设置和播放位置
    val sharedPreferences = context.getSharedPreferences("PlayerPreferences", Context.MODE_PRIVATE)
    var isBackgroundPlayEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("isBackgroundPlayEnabled", false))
    }

    var lastPlaybackPosition by remember {
        mutableStateOf(sharedPreferences.getLong(videoUri.toString(), 0L))
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

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
    var isLongPressing by remember { mutableStateOf(false) } // 标记是否在长按
    var speedIndicatorVisible by remember { mutableStateOf(false) }// 用于控制是否显示速度提示。
    var currentSpeed by remember { mutableStateOf(1.0f) } // 记录当前倍速
    var isDragging by remember { mutableStateOf(false) } // 标记是否正在拖动
    var isUserDragging by remember { mutableStateOf(false) } // 是否正在拖动进度条
    var draggingProgress by remember { mutableFloatStateOf(0f) } // 拖动中的进度
    var wasControlsVisible by remember { mutableStateOf(false) } // 标记拖动开始时工具栏是否显示
    var isAdjustingBrightness by remember { mutableStateOf(false) } // 正在调节亮度
    var isAdjustingVolume by remember { mutableStateOf(false) } // 正在调节音量
    var currentBrightness by remember { mutableStateOf(0.5f) } // 当前亮度（范围 0f ~ 1f）
    var currentVolume by remember { mutableStateOf(0.5f) } // 当前音量（范围 0f ~ 1f）


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

    fun adjustBrightness(activity: Activity, value: Float) {
        val layoutParams = activity.window?.attributes
        layoutParams?.screenBrightness = value
        activity.window?.attributes = layoutParams
    }

    fun adjustVolume(context: Context, value: Float) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = (value * maxVolume).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
    }

    // 主布局：播放器视图 + 控制栏
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!isLongPressing && !isDragging) { // 确保不是长按或拖动触发的
                            controlsVisible = !controlsVisible
                            if (controlsVisible) showControls()
                        }
                    },
                    onDoubleTap = {
                        if (!isLongPressing) {
                            exoPlayer.playWhenReady = !exoPlayer.playWhenReady
                            if (controlsVisible) showControls()
                        }
                    },
                    onLongPress = {
                        isLongPressing = true
                        speedIndicatorVisible = true
                        currentSpeed = exoPlayer.playbackParameters.speed // 保存当前倍速
                        exoPlayer.setPlaybackSpeed(4.0f) // 长按切换到四倍速
                    }
                )
            }
            .pointerInput(Unit) {
                coroutineScope {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val startX = offset.x
                            val startY = offset.y

                            // 初始化状态
                            isAdjustingBrightness = false
                            isAdjustingVolume = false
                            isDragging = false
                            wasControlsVisible = controlsVisible // 记录拖动开始时工具栏的可见状态
                            if (controlsVisible) { // 如果工具栏可见，则隐藏
                                controlsVisible = false
                            }

                            // 在拖动事件触发的前10ms内，判断垂直变动是否大于水平变动
                            launch {
                                delay(10) // 等待 10ms
                                val event = awaitPointerEventScope { awaitPointerEvent() }
                                val changes = event.changes.firstOrNull()
                                changes?.let {
                                    val deltaX = (it.position.x - startX).absoluteValue
                                    val deltaY = (it.position.y - startY).absoluteValue

                                    // 判断是否为亮度或音量调节手势
                                    if (startX < size.width / 3 && deltaY > deltaX) {
                                        isAdjustingBrightness = true // 左侧区域，优先垂直滑动
                                    } else if (startX > size.width * 2 / 3 && deltaY > deltaX) {
                                        isAdjustingVolume = true // 右侧区域，优先垂直滑动
                                    } else {
                                        isDragging = true // 其他区域水平滑动，触发播放进度调整
                                        isUserDragging = true // 拖动开始，标记为拖动状态
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            // 恢复工具栏状态（仅当拖动前工具栏是可见的）
                            if (wasControlsVisible) {
                                controlsVisible = true
                                startHideTimer() // 恢复隐藏计时器
                            }

                            // 重置所有手势状态
                            isAdjustingBrightness = false
                            isAdjustingVolume = false
                            isDragging = false
                            isUserDragging = false
                            if (isLongPressing) {
                                isLongPressing = false
                                speedIndicatorVisible = false
                                exoPlayer.setPlaybackSpeed(currentSpeed) // 恢复到原来的倍速
                            }
                        },
                        onDrag = { change, dragAmount ->
                            val activity = context as? Activity

                            when {
                                isAdjustingBrightness -> {
                                    // 调节亮度
                                    currentBrightness = (currentBrightness - dragAmount.y / size.height).coerceIn(0f, 1f)
                                    activity?.let { adjustBrightness(it, currentBrightness) }
                                }
                                isAdjustingVolume -> {
                                    // 调节音量
                                    currentVolume = (currentVolume - dragAmount.y / size.height).coerceIn(0f, 1f)
                                    adjustVolume(context, currentVolume)
                                }
                                isDragging -> {
                                    // 调整播放进度
                                    val totalDuration = exoPlayer.duration
                                    if (totalDuration > 0) {
                                        val timeChange = (dragAmount.x / size.width) * DRAG_RATIO
                                        val targetTime = (exoPlayer.currentPosition + timeChange.toLong())
                                            .coerceIn(0, totalDuration)
                                        exoPlayer.seekTo(targetTime) // 快进或快退
                                        progress = targetTime.toFloat() / totalDuration
                                        draggingProgress = targetTime.toFloat() / totalDuration
                                    }
                                }
                            }
                        }
                    )
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Release && isLongPressing) { // 捕获松手事件
                            isLongPressing = false
                            speedIndicatorVisible = false
                            exoPlayer.setPlaybackSpeed(currentSpeed) // 恢复到原来的倍速
                        }
                    }
                }
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
        // 调节亮度/音量提示
        if (isAdjustingBrightness || isAdjustingVolume) {

            // 提示框的宽高和内边距
            val boxWidth = if (isLandscape) (screenWidth * 0.4).dp else (screenWidth * 0.6).dp
            val boxHeight = if (isLandscape) (screenHeight * 0.1).dp else (screenHeight * 0.12).dp
            val iconSize = if (isLandscape) (screenHeight * 0.05).dp else (screenHeight * 0.07).dp
            val progressBarHeight = if (isLandscape) (screenHeight * 0.02).dp else (screenHeight * 0.025).dp
            val textPadding = if (isLandscape) (screenHeight * 0.02).dp else (screenHeight * 0.03).dp

            val icon = if (isAdjustingBrightness) Icons.Default.BrightnessMedium else Icons.Default.VolumeUp
            val progress = if (isAdjustingBrightness) currentBrightness else currentVolume

            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(boxWidth)
                    .height(boxHeight)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(textPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(modifier = Modifier.width(8.dp))

                // 自定义进度条
                Box(
                    modifier = Modifier
                        .height(progressBarHeight) // 通过 progressBarHeight 调整粗细
                        .weight(1f) // 动态填充剩余空间
                        .background(Color(0xFFB3B3B3), RoundedCornerShape(progressBarHeight / 2)) // 背景颜色和圆角
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress) // 动态宽度根据进度调整
                            .background(Color(0xFFFF0000), RoundedCornerShape(progressBarHeight / 2)) // 前景颜色和圆角
                    )
                }
            }
        }

        if (isDragging) {
            // 拖动提示框的宽高和内边距动态设置
            val dragBoxWidth = if (isLandscape) (screenWidth * 0.3).dp else (screenWidth * 0.5).dp
            val dragBoxHeight = if (isLandscape) (screenHeight * 0.1).dp else (screenHeight * 0.15).dp
            val dragPadding = if (isLandscape) (screenHeight * 0.02).dp else (screenHeight * 0.03).dp

            AnimatedVisibility(
                visible = isDragging,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(dragBoxWidth)
                    .height(dragBoxHeight)
            ) {
                val currentTime = formatTime((draggingProgress * exoPlayer.duration).toLong())
                val totalTime = formatTime(exoPlayer.duration)

                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(8.dp))
                        .padding(dragPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$currentTime / $totalTime",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else if (speedIndicatorVisible) {
            // 4倍速提示框的宽高和内边距动态设置
            val speedBoxWidth = if (isLandscape) (screenWidth * 0.25).dp else (screenWidth * 0.4).dp
            val speedBoxHeight = if (isLandscape) (screenHeight * 0.1).dp else (screenHeight * 0.12).dp
            val speedPadding = if (isLandscape) (screenHeight * 0.015).dp else (screenHeight * 0.02).dp

            AnimatedVisibility(
                visible = speedIndicatorVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(speedBoxWidth)
                    .height(speedBoxHeight)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(8.dp))
                        .padding(speedPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "4倍速播放",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // 顶部工具栏
        if (controlsVisible && !isDragging) {
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

        // 下方工具栏
        if (controlsVisible || isDragging)BottomControlBar(
            isPlaying = exoPlayer.playWhenReady,
            onPlayPause = { /* 播放暂停逻辑 */ },
            onNext = { /* 下一集逻辑 */ },
            onPrevious = { /* 上一集逻辑 */ },
            progress = progress,
            totalDuration = exoPlayer.duration,
            onProgressChanged = { newProgress, isUserDragging ->
                draggingProgress = newProgress
                if (!isUserDragging) {
                    exoPlayer.seekTo((exoPlayer.duration * newProgress).toLong())
                }
            },
            exoPlayer = exoPlayer,
            isDragging = isDragging, // 根据拖动状态动态显示
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}



