package com.example.lyplayer

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.ExoPlayer

class ExoPlayerLifecycleObserver(
    private val exoPlayer: ExoPlayer,
    private val isBackgroundPlayEnabled: () -> Boolean // 使用函数动态判断后台播放状态
) : DefaultLifecycleObserver {

    private var wasPlayingBeforePause = false

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        // 进入后台时，记录播放状态，只有在没有启用后台播放时才暂停视频
        if (exoPlayer.playWhenReady) {
            wasPlayingBeforePause = true
        }
        if (!isBackgroundPlayEnabled()) {
            exoPlayer.playWhenReady = false // 暂停播放
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        // 如果后台播放未启用并且之前是播放状态，恢复播放
        if (!isBackgroundPlayEnabled()) {
            if (wasPlayingBeforePause) {
                exoPlayer.playWhenReady = true // 恢复播放
            }
        } else {
            // 如果启用了后台播放，保持视频状态
            exoPlayer.playWhenReady = exoPlayer.playWhenReady
        }

        // 重置状态
        wasPlayingBeforePause = false
    }
}



