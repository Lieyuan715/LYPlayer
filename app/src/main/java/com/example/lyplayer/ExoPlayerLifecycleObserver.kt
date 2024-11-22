package com.example.lyplayer

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.ExoPlayer

class ExoPlayerLifecycleObserver(
    private val exoPlayer: ExoPlayer,
    private val isBackgroundPlayEnabled: () -> Boolean // 使用函数动态判断状态
) : DefaultLifecycleObserver {

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        if (!isBackgroundPlayEnabled()) {
            exoPlayer.playWhenReady = false // 暂停播放
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        if (!isBackgroundPlayEnabled()) {
            exoPlayer.playWhenReady = true // 恢复播放
        }
    }
}

