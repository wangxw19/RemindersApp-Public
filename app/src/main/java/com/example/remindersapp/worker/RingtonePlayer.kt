package com.example.remindersapp.worker

import android.content.Context
import android.media.MediaPlayer
import com.example.remindersapp.R
import com.example.remindersapp.data.UserSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RingtonePlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userSettingsRepository: UserSettingsRepository // 注入用户设置仓库
) {
    private var mediaPlayer: MediaPlayer? = null

    fun start() {
        // --- 核心改动：在播放前检查静音状态 ---
        // 使用 runBlocking 是因为此方法可能从非协程环境（如 Service）调用，
        // 且我们需要同步获取结果。对于快速的 DataStore 读取，这是可接受的。
        val isMuted = runBlocking { userSettingsRepository.isMutedFlow.first() }
        if (isMuted) {
            return // 如果是静音状态，则不执行任何操作
        }

        if (mediaPlayer?.isPlaying == true) {
            return
        }
        stop() // 先停止并释放任何旧的实例

        mediaPlayer = MediaPlayer.create(context, R.raw.reminder_ringtone).apply {
            isLooping = true
            setVolume(0.5f, 0.5f)
            start()
        }
    }

    fun stop() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying ?: false
}