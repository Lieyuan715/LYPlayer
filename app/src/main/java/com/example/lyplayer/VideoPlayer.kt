package com.example.lyplayer

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView

/**
 * VideoPlayer 组件，用于显示视频播放界面并提供返回按钮。
 * @param videoUri 视频文件的 URI，用于加载视频内容
 * @param modifier 用于自定义外部布局修饰
 * @param onBackClicked 点击返回按钮时的回调函数
 * @param onPlaybackStateChanged 播放状态变化的回调函数，参数为是否正在播放
 */
@Composable
fun VideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit,
    onPlaybackStateChanged: (Boolean) -> Unit
) {
    // 获取当前上下文
    val context = androidx.compose.ui.platform.LocalContext.current

    // 创建 ExoPlayer 实例
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // 加载视频媒体项
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare() // 准备播放器
            playWhenReady = true // 自动开始播放
        }
    }

    // DisposableEffect 确保组件销毁时释放资源
    DisposableEffect(key1 = videoUri) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                // 根据播放器状态触发回调
                onPlaybackStateChanged(playbackState == Player.STATE_READY && exoPlayer.playWhenReady)
            }
        }
        // 注册监听器
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener) // 移除监听器
            exoPlayer.release() // 释放资源
        }
    }

    // 布局内容
    Box(modifier = modifier) {
        // 返回按钮，位于左上角
        IconButton(
            onClick = {
                exoPlayer.pause() // 暂停播放
                onPlaybackStateChanged(false) // 通知主界面停止播放
                onBackClicked() // 触发返回逻辑
            },
            modifier = Modifier
                .align(Alignment.TopStart) // 对齐到左上角
                .padding(16.dp) // 添加内边距
                .zIndex(1f) // 设置按钮在最上层
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        // 视频播放器，嵌入到 Compose 中
        AndroidView(
            factory = {
                StyledPlayerView(it).apply {
                    player = exoPlayer // 绑定播放器
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    ) // 全屏显示
                }
            },
            modifier = modifier.zIndex(0f) // 设置播放器在底层显示
        )
    }
}
