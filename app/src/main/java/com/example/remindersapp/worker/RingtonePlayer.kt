package com.example.remindersapp.worker

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.remindersapp.R
import com.example.remindersapp.data.UserSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RingtonePlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userSettingsRepository: UserSettingsRepository
) {
    companion object {
        private const val TAG = "RingtonePlayer"
        private const val VOLUME = 0.5f
        private const val DEBUG = false // 通过此开关控制日志
    }

    private var mediaPlayer: MediaPlayer? = null
    private val lock = Any()

    /**
     * 异步启动铃声播放
     * 使用 MediaPlayer 的完整错误处理和生命周期管理
     */
    fun start(callback: ((Boolean) -> Unit)? = null) {
        synchronized(lock) {
            // 检查是否已在播放
            if (mediaPlayer?.isPlaying == true) {
                if (DEBUG) Log.d(TAG, "Already playing, skipping start()")
                callback?.invoke(false)
                return
            }

            // 立即释放旧实例（避免内存泄漏）
            release()

            try {
                // 创建 MediaPlayer 实例
                val player = MediaPlayer.create(context, R.raw.reminder_ringtone)

                if (player != null) {
                    player.setVolume(VOLUME, VOLUME)
                    player.isLooping = true

                    player.setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                        release()
                        callback?.invoke(false)
                        true
                    }

                    player.setOnCompletionListener {
                        if (DEBUG) Log.d(TAG, "Playback completed")
                    }

                    try {
                        player.start()
                        mediaPlayer = player
                        if (DEBUG) Log.d(TAG, "Ringtone started successfully")
                        callback?.invoke(true)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start playback", e)
                        player.release()
                        callback?.invoke(false)
                    }
                } else {
                    Log.e(TAG, "Failed to create MediaPlayer")
                    callback?.invoke(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in start()", e)
                release()
                callback?.invoke(false)
            }
        }
    }

    /**
     * 检查静音状态并根据需要启动
     * 在调用前检查 UserSettings
     */
    suspend fun startIfNotMuted(callback: ((Boolean) -> Unit)? = null) {
        try {
            val isMuted = userSettingsRepository.isMutedFlow
                .onEach { muted ->
                    if (DEBUG) Log.d(TAG, "Mute state: $muted")
                }
                .first()

            if (!isMuted) {
                start(callback)
            } else {
                if (DEBUG) Log.d(TAG, "Skipping playback due to mute setting")
                callback?.invoke(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking mute status", e)
            callback?.invoke(false)
        }
    }

    /**
     * 停止播放并释放资源
     */
    fun stop() {
        synchronized(lock) {
            try {
                mediaPlayer?.apply {
                    if (isPlaying) {
                        stop()
                        if (DEBUG) Log.d(TAG, "Playback stopped")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping playback", e)
            } finally {
                release()
            }
        }
    }

    /**
     * 完全释放 MediaPlayer 资源
     */
    private fun release() {
        try {
            mediaPlayer?.apply {
                reset()
                release()
                if (DEBUG) Log.d(TAG, "MediaPlayer released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }
    }

    /**
     * 查询播放状态
     */
    val isPlaying: Boolean
        get() = synchronized(lock) {
            try {
                mediaPlayer?.isPlaying ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Error checking playback state", e)
                false
            }
        }

    /**
     * 清理资源（应在 Service/Activity 销毁时调用）
     */
    fun onDestroy() {
        synchronized(lock) {
            stop()
        }
    }
}