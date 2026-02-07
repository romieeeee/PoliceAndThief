// util/SoundPlayer.kt
package com.d104.pnt.util

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    fun playSound(@RawRes soundResId: Int) {
        try {
            mediaPlayer?.release()

            mediaPlayer = MediaPlayer.create(context, soundResId).apply {
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                start()
            }
            Timber.d("🔊 효과음 재생: $soundResId")
        } catch (e: Exception) {
            Timber.e(e, "🔊 효과음 재생 실패")
        }
    }

    fun playBgm(@RawRes soundResId: Int, isLooping: Boolean = true) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, soundResId).apply {
            this.isLooping = isLooping
            start()
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}