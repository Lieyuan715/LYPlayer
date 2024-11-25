package com.example.lyplayer

import android.content.pm.ActivityInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext

@Composable
fun BottomControlBar(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    progress: Float,
    totalDuration: Long,
    onProgressChanged: (Float, Boolean) -> Unit,
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

    LaunchedEffect(isUserDragging, draggingProgress) {
        if (isUserDragging) {
            delay(50)
            onProgressChanged(draggingProgress, false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.01f))
            .height(if (isLandscape) (screenHeight / 3).dp else (screenHeight / 4).dp) // 高度根据横竖屏调整
    ) {
        // 时间显示部分
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(bottom = if (isLandscape) (screenHeight * 0.26f).dp else (screenHeight * 0.12f).dp) // 时间部分下边距调整
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
                    start = (screenWidth / 64).dp,
                    end = (screenWidth / 64).dp,
                    bottom = if (isLandscape) (screenHeight * 0.14f).dp else (screenHeight * 0.06f).dp // 进度条下边距调整
                )
                .height(if (isLandscape) (screenHeight * 0.1f).dp else (screenHeight * 0.06f).dp) // 进度条高度调整
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val clickedPercentage = (offset.x / size.width).coerceIn(0f, 1f)
                            draggingProgress = clickedPercentage
                            onProgressChanged(draggingProgress, true)
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
                    size = androidx.compose.ui.geometry.Size(size.width - activeWidth, barHeight),
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
                    bottom = (screenHeight * 0.01f).dp
                )
        ) {
            val pauseButtonSize = if (isLandscape) screenHeight * 0.12f else screenHeight * 0.06f
            val lastNextButtonSize = if (isLandscape) screenHeight * 0.1f else screenHeight * 0.04f

            IconButton(onClick = onPrevious, modifier = Modifier.size(pauseButtonSize.dp)) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(lastNextButtonSize.dp)
                )
            }

            Spacer(modifier = Modifier.width((screenHeight / 32).dp))

            IconButton(onClick = onPlayPause, modifier = Modifier.size(pauseButtonSize.dp)) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(pauseButtonSize.dp)
                )
            }

            Spacer(modifier = Modifier.width((screenHeight / 32).dp))

            IconButton(onClick = onNext, modifier = Modifier.size(pauseButtonSize.dp)) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(lastNextButtonSize.dp)
                )
            }
        }

        // 横竖屏切换按钮
        IconButton(
            onClick = {
                activity?.requestedOrientation =
                    if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = (screenWidth / 64).dp, bottom = (screenHeight * 0.01f).dp)
                .size(if (isLandscape) (screenHeight * 0.16f).dp else (screenHeight * 0.08f).dp) // 按钮大小调整
        ) {
            Icon(
                imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (isLandscape) "Exit Fullscreen" else "Fullscreen",
                tint = Color.White,
                modifier = Modifier.size(if (isLandscape) (screenHeight * 0.12f).dp else (screenHeight * 0.06f).dp) // 图标大小调整
            )
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
