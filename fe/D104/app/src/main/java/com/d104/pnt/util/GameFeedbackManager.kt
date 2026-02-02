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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameFeedbackManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val appContext = context.applicationContext

    // ==================== VIBRATOR ====================
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // ==================== TONE (fallback / 간단 버전) ====================
    // ⚠️ ToneGenerator를 "매번 new" 하면 AudioTrack 리소스가 쌓여 -12 터질 수 있어서 1개만 유지합니다.
    private val toneGenerator: ToneGenerator by lazy {
        ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    }

    // ==================== SOUND POOL (실제 사운드 파일 버전) ====================
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundMap = mutableMapOf<SoundType, Int>()

    enum class SoundType {
        BEEP_NEAR_POLICE,
    }

    init {
        loadSounds()
    }

    // ==================== 내부 코루틴 / 동시성 제어 ====================
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val playMutex = Mutex()
    private var playJob: Job? = null

    // 너무 자주 오면 무시(서버가 초당 여러 번 보내도 안전)
    private var lastPlayAtMs: Long = 0L
    private val minIntervalMs = 450L // 필요하면 300~700ms 사이로 조절

    // ==================== PUBLIC API ====================

    /**
     * 🚨 경찰 근접 경보
     * distance(m)에 따라 강도 변화
     *
     * - far   : 삑 1~2회 + 약진동
     * - mid   : 삑삑삑 + 중진동
     * - near  : 삑삑삑삑 + 길게 삐—— + 강진동
     */
    fun playBeepAlert(distance: Double?) {
        // 외부(Compose collect)에서 그냥 호출하기 쉽게 non-suspend로 제공
        scope.launch {
            playEmergencyBeepAlertInternal(distance)
        }
    }

    /**
     * (원하면 화면 테스트 버튼에서 이걸 바로 호출해도 됨)
     */
    fun playEmergencyTestAlert() {
        playBeepAlert(distance = 10.0)
    }

    /**
     * 앱 종료/테스트 종료 시 리소스 정리 (필수는 아니지만 안전)
     */
    fun release() {
        try {
            playJob?.cancel()
            soundPool.release()
            toneGenerator.release()
            scope.cancel()
        } catch (e: Exception) {
            Timber.e(e, "GameFeedbackManager release 실패")
        }
    }

    // ==================== INTERNAL ====================

    private suspend fun playEmergencyBeepAlertInternal(distance: Double?) {
        val now = System.currentTimeMillis()
        if (now - lastPlayAtMs < minIntervalMs) {
            // 너무 잦은 호출은 무시
            return
        }
        lastPlayAtMs = now

        playMutex.withLock {
            // 이전 패턴 재생 중이면 끊고 새 패턴으로 갱신
            playJob?.cancel()

            val pattern = buildPattern(distance)

            playJob = scope.launch {
                try {
                    // 진동은 패턴 시작에 한 번(또는 단계별) 주는 게 안정적
                    playVibration(pattern.vibrationType)

                    // 삑 N번
                    repeat(pattern.beepCount) { idx ->
                        playBeepOnce(pattern.beepDurationMs)
                        // 마지막 beep 후엔 간격 주지 않음
                        if (idx != pattern.beepCount - 1) {
                            delay(pattern.beepGapMs)
                        }
                    }

                    // 마지막에 길게 “삐——” (near일 때만)
                    if (pattern.longToneMs > 0) {
                        delay(pattern.longToneDelayMs)
                        playBeepOnce(pattern.longToneMs)
                    }
                } catch (_: Exception) {
                    // cancel 포함해서 조용히 무시
                }
            }
        }
    }

    /**
     * distance에 따른 난이도(강도) 패턴 설계
     */
    private fun buildPattern(distance: Double?): BeepPattern {
        val d = distance ?: 9999.0

        return when {
            d <= 10.0 -> {
                // 매우 가까움: 삑삑삑삑 + 삐——
                BeepPattern(
                    beepCount = 4,
                    beepDurationMs = 120,
                    beepGapMs = 90,
                    longToneMs = 450,
                    longToneDelayMs = 120,
                    vibrationType = VibrationType.STRONG
                )
            }
            d <= 20.0 -> {
                // 가까움: 삑삑삑 + 짧은 삐
                BeepPattern(
                    beepCount = 3,
                    beepDurationMs = 110,
                    beepGapMs = 120,
                    longToneMs = 220,
                    longToneDelayMs = 120,
                    vibrationType = VibrationType.MEDIUM
                )
            }
            d <= 35.0 -> {
                // 중간: 삑삑
                BeepPattern(
                    beepCount = 2,
                    beepDurationMs = 110,
                    beepGapMs = 160,
                    longToneMs = 0,
                    longToneDelayMs = 0,
                    vibrationType = VibrationType.LIGHT
                )
            }
            else -> {
                // 멀다: 삑 1회
                BeepPattern(
                    beepCount = 1,
                    beepDurationMs = 120,
                    beepGapMs = 0,
                    longToneMs = 0,
                    longToneDelayMs = 0,
                    vibrationType = VibrationType.LIGHT
                )
            }
        }
    }

    private fun playBeepOnce(durationMs: Int) {
        // 1) soundPool로 로드된 사운드가 있으면 그걸 우선 사용
        val soundId = soundMap[SoundType.BEEP_NEAR_POLICE]
        if (soundId != null) {
            soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f)
            // SoundPool은 duration을 직접 제어하기 힘드니, 파일 자체를 짧게 만든다고 생각하면 됨
            return
        }

        // 2) fallback: ToneGenerator
        // ToneGenerator는 내부적으로 AudioTrack을 만들기 때문에, "생성 남발"만 안 하면 안정적
        try {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs)
        } catch (e: Exception) {
            Timber.e(e, "ToneGenerator beep 실패")
        }
    }

    private fun playVibration(type: VibrationType) {
        if (!vibrator.hasVibrator()) return

        try {
            when (type) {
                VibrationType.LIGHT -> vibrateOneShot(40L, 90)
                VibrationType.MEDIUM -> vibrateOneShot(70L, 140)
                VibrationType.STRONG -> {
                    // 강한 패턴(짧게 2번)
                    vibrateOneShot(70L, 200)
                    // 연속 진동 느낌을 주려고 약간 텀
                    scope.launch {
                        delay(90)
                        vibrateOneShot(60L, 200)
                    }
                }
                VibrationType.NONE -> Unit
            }
        } catch (e: Exception) {
            Timber.e(e, "진동 실행 실패")
        }
    }

    private fun vibrateOneShot(durationMillis: Long, amplitude: Int) {
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(durationMillis, amplitude)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMillis)
        }
    }

    private fun loadSounds() {
        try {
            // ✅ "실제 사운드 파일 버전"은 여기서 raw 리소스를 로드하는 걸 말해요.
            // 예) app/src/main/res/raw/beep_alert.wav 를 넣고 아래 주석 해제
            //
            // soundMap[SoundType.BEEP_NEAR_POLICE] =
            //     soundPool.load(appContext, R.raw.beep_alert, 1)

            Timber.d("🎵 사운드 리소스 로딩 완료(현재: Tone fallback)")
        } catch (e: Exception) {
            Timber.e(e, "사운드 로딩 실패")
        }
    }

    // ==================== DATA ====================

    private data class BeepPattern(
        val beepCount: Int,
        val beepDurationMs: Int,
        val beepGapMs: Long,
        val longToneMs: Int,
        val longToneDelayMs: Long,
        val vibrationType: VibrationType
    )

    private enum class VibrationType {
        NONE, LIGHT, MEDIUM, STRONG
    }
}
