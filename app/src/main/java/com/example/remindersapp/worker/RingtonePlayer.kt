package com.example.remindersapp.worker

import android.content.Context
import android.media.MediaPlayer
import com.example.remindersapp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RingtonePlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    fun start() {
        if (mediaPlayer?.isPlaying == true) {
            return
        }
        stop() // 先停止并释放任何旧的实例

        mediaPlayer = MediaPlayer.create(context, R.raw.reminder_ringtone).apply {
            isLooping = true
            // --- 核心改动：设置音量为 50% ---
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