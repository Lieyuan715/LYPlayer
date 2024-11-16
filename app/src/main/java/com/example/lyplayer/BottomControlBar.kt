package com.example.lyplayer

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

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
    // 是否在拖动
    var isUserDragging by remember { mutableStateOf(false) }
    // 拖动时的临时进度
    var draggingProgress by remember { mutableFloatStateOf(progress) }
    // 上次同步的播放进度，用于延迟更新
    var lastStableProgress by remember { mutableFloatStateOf(progress) }

    // 拖动时显示拖动进度；非拖动时显示实际播放进度
    val displayedProgress = if (isUserDragging) draggingProgress else progress

    // 计算显示的时间
    val displayedTime = (displayedProgress * totalDuration).toLong()
    val currentTimeFormatted = formatTime(displayedTime)
    val totalDurationFormatted = formatTime(totalDuration)

    // 屏幕信息
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    // 延迟更新画面的逻辑
    LaunchedEffect(isUserDragging, draggingProgress) {
        if (isUserDragging) {
            delay(200) // 延迟200ms更新
            if (draggingProgress != lastStableProgress) {
                onProgressChanged(draggingProgress, false)
                lastStableProgress = draggingProgress
            }
        }
    }

    // 主容器
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.03f))
            .height((screenHeight / 3).dp)
    ) {
        // 时间显示部分
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(bottom = (screenHeight * 0.26f).dp)
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
                .padding(bottom = (screenHeight * 0.11f).dp)
        ) {
            Slider(
                value = displayedProgress,
                onValueChange = { newValue ->
                    isUserDragging = true
                    draggingProgress = newValue
                },
                onValueChangeFinished = {
                    // 拖动完成，直接恢复到播放器的实时进度
                    isUserDragging = false
                    lastStableProgress = progress // 防止回跳，更新到实际播放位置
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = (screenWidth / 64).dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFFFF0000),
                    inactiveTrackColor = Color(0xFFB3B3B3)
                )
            )
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
            val pauseButtonSize = screenHeight * 0.12f
            val lastNextButtonSize = screenHeight * 0.1f

            // 上一集按钮
            IconButton(onClick = onPrevious, modifier = Modifier.size(pauseButtonSize.dp)) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(lastNextButtonSize.dp)
                )
            }

            Spacer(modifier = Modifier.width((screenHeight / 32).dp))

            // 播放/暂停按钮
            IconButton(onClick = onPlayPause, modifier = Modifier.size(pauseButtonSize.dp)) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(pauseButtonSize.dp)
                )
            }

            Spacer(modifier = Modifier.width((screenHeight / 32).dp))

            // 下一集按钮
            IconButton(onClick = onNext, modifier = Modifier.size(pauseButtonSize.dp)) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(lastNextButtonSize.dp)
                )
            }
        }
    }
}

// 格式化时间
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
