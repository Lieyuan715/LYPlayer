package com.example.lyplayer

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
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

    // SharedPreferences for saving the background play state
    val sharedPreferences = context.getSharedPreferences("PlayerPreferences", Context.MODE_PRIVATE)
    var isBackgroundPlayEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("isBackgroundPlayEnabled", false))
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
        // 移除当前绑定的观察者（如果有）
        currentLifecycleObserver.value?.let {
            activity?.lifecycle?.removeObserver(it)
        }

        // 创建新的观察者并绑定
        val newObserver = ExoPlayerLifecycleObserver(exoPlayer) {
            isBackgroundPlayEnabled // 动态判断后台播放状态
        }
        currentLifecycleObserver.value = newObserver

        // 如果后台播放未启用，则添加观察者
        if (!isBackgroundPlayEnabled) {
            activity?.lifecycle?.addObserver(newObserver)
        }
    }



    // 切换后台播放状态的逻辑
    fun toggleBackgroundPlay(enabled: Boolean) {
        isBackgroundPlayEnabled = enabled
        sharedPreferences.edit().putBoolean("isBackgroundPlayEnabled", enabled).apply()

        // 重新绑定生命周期观察者
        updatePlayerLifecycle()

        // 如果开启后台播放，确保播放器立即恢复播放
        if (enabled) {
            if (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.prepare()
            }
            exoPlayer.playWhenReady = true
        } else {
            // 如果关闭后台播放，暂停播放器（如果生命周期不在前台）
            if (activity?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) != true) {
                exoPlayer.playWhenReady = false
            }
        }
    }


    LaunchedEffect(isBackgroundPlayEnabled) {
        if (isBackgroundPlayEnabled) {
            // 确保后台播放时播放器正常运行
            if (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.prepare()
            }
            exoPlayer.playWhenReady = true
        }
    }


    DisposableEffect(videoUri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        val path = videoUri.path
        videoTitle = if (path != null) File(path).name else "Unknown Video"

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isPlaying = playbackState == Player.STATE_READY && exoPlayer.playWhenReady
                onPlaybackStateChanged(isPlaying)
            }
        }
        exoPlayer.addListener(listener)

        // 初始绑定生命周期
        updatePlayerLifecycle()

        onDispose {
            exoPlayer.removeListener(listener)

            // 退出时根据后台播放开关停止或继续播放
            if (!isBackgroundPlayEnabled) {
                exoPlayer.stop()
                exoPlayer.release()
            }

            // 恢复竖屏
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
            delay(5000)
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


