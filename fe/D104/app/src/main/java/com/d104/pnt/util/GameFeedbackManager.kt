package com.d104.pnt.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class GameFeedbackManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12 (API 31) 이상
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        // 그 이하 버전
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5) // 동시에 재생 가능한 소리 개수
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME) // 용도: 게임
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundMap = mutableMapOf<SoundType, Int>()

    enum class SoundType {

    }

    init {
        loadSounds()
    }

    fun playHapticFeedback() {
        vibrate(30L, 100)
    }

    private fun vibrate(durationMillis: Long, amplitude: Int) {
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API 26 (Oreo) 이상: 세기(Amplitude) 조절 가능
            // amplitude가 -1이면 기본 세기
            try {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(durationMillis, amplitude)
                )
            } catch (e: Exception) {
                Timber.e(e, "진동 실행 실패")
            }
        } else {
            // API 26 미만: 시간만 조절 가능
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMillis)
        }
    }

    private fun loadSounds() {
        try {
            // soundMap[SoundType.ALERT_HIGH] = soundPool.load(context, R.raw.alert_high, 1)

            Timber.d("사운드 리소스 로딩 완료")
        } catch (e: Exception) {
            Timber.e(e, "사운드 로딩 실패")
        }
    }

    private fun playSound(type: SoundType, rate: Float = 1.0f) {
        val soundId = soundMap[type] ?: return // 로드 안 됐으면 무시

        // play(soundId, leftVol, rightVol, priority, loop, rate)
        soundPool.play(soundId, 1.0f, 1.0f, 0, 0, rate)
    }
}