package com.example.lyplayer

import android.content.pm.ActivityInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackParameters
import androidx.compose.ui.platform.LocalDensity

@Composable
fun BottomControlBar(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    progress: Float,
    totalDuration: Long,
    onProgressChanged: (Float, Boolean) -> Unit,
    exoPlayer: ExoPlayer,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    var isUserDragging by remember { mutableStateOf(false) }
    var draggingProgress by remember { mutableFloatStateOf(progress) }
    val displayedProgress = if (isUserDragging) draggingProgress else progress
    val displayedTime = (displayedProgress * totalDuration).toLong()
    val currentTimeFormatted = formatTime(displayedTime)
    val totalDurationFormatted = formatTime(totalDuration)

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    val activity = context as? android.app.Activity
    // 倍速调节逻辑相关变量
    var isSpeedMenuVisible by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1.0f) } // 默认倍速


    LaunchedEffect(isUserDragging, draggingProgress) {
        if (isUserDragging) {
            delay(50)
            onProgressChanged(draggingProgress, false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.0f), // 上方颜色
                        Color.Black.copy(alpha = 0.7f)  // 下方颜色
                    )
                )
            )
            .height(if (isLandscape) (screenHeight / 3).dp else (screenHeight / 4).dp) // 高度根据横竖屏调整
    ) {
        if (isDragging) {
            // 仅显示进度条
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(
                        start = (screenWidth * 0.025).dp,
                        end = (screenWidth * 0.025).dp,
                        bottom = if (isLandscape) (screenHeight * 0.18).dp else (screenHeight * 0.085).dp
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                    val activeWidth = size.width * displayedProgress
                    drawRoundRect(
                        color = Color(0xFFFF0000),
                        size = androidx.compose.ui.geometry.Size(activeWidth, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFFB3B3B3),
                        size = androidx.compose.ui.geometry.Size(
                            size.width - activeWidth,
                            size.height
                        ),
                        topLeft = Offset(activeWidth, 0f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                }
            }
        } else {
            // 显示完整控制栏
            // 时间显示部分
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(bottom = if (isLandscape) (screenHeight * 0.26).dp else (screenHeight * 0.12).dp) // 时间部分下边距调整
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (screenWidth / 36).dp)
                ) {
                    Text(
                        text = currentTimeFormatted,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                    )
                    Text(
                        text = "/",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        modifier = Modifier.padding(horizontal = (screenWidth / 128).dp)
                    )
                    Text(
                        text = totalDurationFormatted,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                    )
                }
            }

            // 进度条部分
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(
                        start = (screenWidth * 0.025).dp,
                        end = (screenWidth * 0.025).dp,
                        bottom = if (isLandscape) (screenHeight * 0.14).dp else (screenHeight * 0.06).dp // 进度条下边距调整
                    )
                    .height(if (isLandscape) (screenHeight * 0.1).dp else (screenHeight * 0.06).dp) // 进度条高度调整
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                isUserDragging = false // 点击时明确不是拖动
                                val clickedProgress = (offset.x / size.width).coerceIn(0f, 1f)
                                draggingProgress = clickedProgress
                                onProgressChanged(clickedProgress, false) // 点击跳转
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isUserDragging = true },
                            onDragEnd = {
                                isUserDragging = false
                                onProgressChanged(draggingProgress, true)
                            },
                            onDragCancel = { isUserDragging = false },
                            onDrag = { _, dragAmount ->
                                val delta = dragAmount.x / size.width
                                draggingProgress = (draggingProgress + delta).coerceIn(0f, 1f)
                                onProgressChanged(draggingProgress, false)
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val activeWidth = size.width * displayedProgress
                    val barHeight = size.height / 5
                    val verticalOffset = (size.height - barHeight) / 2

                    drawRoundRect(
                        color = Color(0xFFFF0000),
                        size = androidx.compose.ui.geometry.Size(activeWidth, barHeight),
                        topLeft = Offset(0f, verticalOffset),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )

                    drawRoundRect(
                        color = Color(0xFFB3B3B3),
                        size = androidx.compose.ui.geometry.Size(
                            size.width - activeWidth,
                            barHeight
                        ),
                        topLeft = Offset(activeWidth, verticalOffset),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )

                    drawCircle(
                        color = Color.White,
                        radius = barHeight,
                        center = Offset(activeWidth, size.height / 2)
                    )
                }
            }

            // 播放控制按钮部分
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = (screenWidth / 64).dp,
                        bottom = (screenHeight * 0.02).dp
                    )
            ) {
                val pauseButtonSize =
                    if (isLandscape) screenHeight * 0.12 else screenHeight * 0.06
                val lastNextButtonSize =
                    if (isLandscape) screenHeight * 0.1 else screenHeight * 0.04

                IconButton(onClick = onPrevious, modifier = Modifier.size(pauseButtonSize.dp)) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(lastNextButtonSize.dp)
                    )
                }

                Spacer(modifier = Modifier.width((screenWidth / 32).dp))

                IconButton(onClick = onPlayPause, modifier = Modifier.size(pauseButtonSize.dp)) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(pauseButtonSize.dp)
                    )
                }

                Spacer(modifier = Modifier.width((screenWidth / 32).dp))

                IconButton(onClick = onNext, modifier = Modifier.size(pauseButtonSize.dp)) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(lastNextButtonSize.dp)
                    )
                }
            }

            // 倍速按钮和菜单
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = if (isLandscape) (screenWidth * 0.06).dp else (screenWidth * 0.12).dp,
                        bottom = if (isLandscape) (screenHeight * 0.01).dp else (screenHeight * 0.025).dp
                    )
            ) {
                val buttonWidth =
                    if (isLandscape) (screenWidth * 0.12).dp else (screenWidth * 0.2).dp
                val buttonHeight =
                    if (isLandscape) (screenHeight * 0.16).dp else (screenHeight * 0.05).dp

                Box(
                    modifier = Modifier
                        .padding(end = (screenWidth / 64).dp)
                        .width(buttonWidth)
                        .height(buttonHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isSpeedMenuVisible = !isSpeedMenuVisible // 点击切换菜单状态
                            playbackSpeed = exoPlayer.playbackParameters.speed}, // 同步播放器实际倍速
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "倍速",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                    )
                }

                // 自定义菜单
                if (isSpeedMenuVisible) {
                    val menuBottomPadding =
                        if (isLandscape) (screenHeight * 0.75).dp else (screenHeight * 0.475).dp // 自定义顶部到底部间距
                    val density = LocalDensity.current // 获取当前的 Density 环境

                    Popup(
                        alignment = Alignment.TopStart, // Popup 从按钮顶部对齐
                        offset = with(density) {
                            IntOffset(
                                0,
                                -menuBottomPadding.toPx().toInt()
                            )
                        }, // 控制距离底部的偏移量
                    ) {
                        val highlightColor = Color(0xFFFF0000) // 自定义高亮颜色

                        Column(
                            modifier = Modifier
                                .width(buttonWidth) // 菜单宽度与按钮一致
                                .background(
                                    color = Color.Black.copy(alpha = 0.9f), // 菜单背景颜色
                                    shape = RoundedCornerShape(8.dp) // 设置菜单圆角
                                )
                                .padding((screenHeight * 0.01f).dp) // 内边距
                        ) {
                            listOf(4.0f, 2.0f, 1.5f, 1.0f, 0.5f, 0.1f).forEach { speed ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = (screenHeight * 0.02).dp,
                                            horizontal = (screenHeight * 0.01).dp
                                        ) // 调整选项的内边距
                                        .clickable {
                                            playbackSpeed = speed // 更新倍速
                                            setPlaybackSpeed(exoPlayer, speed) // 同步播放器倍速
                                            isSpeedMenuVisible = false // 关闭菜单
                                        }
                                ) {
                                    // 使用 Box 来对齐文字
                                    Text(
                                        text = "$speed x",
                                        color = if (speed == playbackSpeed) highlightColor else Color.White, // 仅文字高亮
                                        modifier = Modifier.align(Alignment.Center) // 在 Box 内居中对齐
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 横竖屏切换按钮
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd) // 对齐到右下角
                    .padding(
                        end = (screenWidth / 64).dp,
                        bottom = (screenHeight * 0.01).dp
                    ), // 外边距调整
                verticalAlignment = Alignment.CenterVertically, // 垂直对齐方式
                horizontalArrangement = Arrangement.End // 水平方向靠右对齐
            ) {
                // 横竖屏切换按钮
                IconButton(
                    onClick = {
                        activity?.requestedOrientation =
                            if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    },
                    modifier = Modifier
                        .size(if (isLandscape) (screenHeight * 0.16).dp else (screenHeight * 0.08).dp) // 按钮大小调整
                ) {
                    // 图标
                    Icon(
                        imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, // 图标切换
                        contentDescription = if (isLandscape) "Exit Fullscreen" else "Fullscreen", // 描述文本
                        tint = Color.White, // 图标颜色
                        modifier = Modifier.size(if (isLandscape) (screenHeight * 0.12).dp else (screenHeight * 0.06).dp) // 图标大小调整
                    )
                }
            }
        }
    }
}

// 格式化时间函数
fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    val hours = (milliseconds / (1000 * 60 * 60)) % 24
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

// 倍速设置函数
fun setPlaybackSpeed(exoPlayer: ExoPlayer, speed: Float) {
    exoPlayer.setPlaybackParameters(PlaybackParameters(speed))
    println("ExoPlayer 播放速度已设置为：$speed 倍速")
}