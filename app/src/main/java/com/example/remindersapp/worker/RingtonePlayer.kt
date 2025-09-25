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
        mediaPlayer?.release() // Release any previous instance
        mediaPlayer = MediaPlayer.create(context, R.raw.reminder_ringtone).apply {
            isLooping = true
            start()
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying ?: false
}