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
    private var sfxPlayer: MediaPlayer? = null      // 효과음 전용
    private var bgmPlayer: MediaPlayer? = null      // BGM 전용
    private var currentBgmTag: String? = null

    // 효과음 재생 (무전기 소리 등)
    fun playSound(@RawRes soundResId: Int) {
        try {
            sfxPlayer?.release()

            sfxPlayer = MediaPlayer.create(context, soundResId).apply {
                setOnCompletionListener {
                    it.release()
                    sfxPlayer = null
                }
                start()
            }
            Timber.d("🔊 효과음 재생: $soundResId")
        } catch (e: Exception) {
            Timber.e(e, "🔊 효과음 재생 실패: $soundResId")
        }
    }

    // BGM 재생 (태그 시스템)
    fun playBgm(@RawRes soundResId: Int, tag: String, isLooping: Boolean = true) {
        try {
            Timber.d("🔊 BGM 재생 시도: tag=$tag, resId=$soundResId, looping=$isLooping")

            // 이미 같은 태그로 재생 중이면 무시
            if (currentBgmTag == tag && bgmPlayer?.isPlaying == true) {
                Timber.d("🔊 이미 같은 BGM 재생 중: $tag")
                return
            }

            // 기존 BGM 정리
            bgmPlayer?.let {
                Timber.d("🔊 기존 BGM 정지: $currentBgmTag")
                if (it.isPlaying) it.stop()
                it.release()
            }

            bgmPlayer = MediaPlayer.create(context, soundResId)

            if (bgmPlayer == null) {
                Timber.e("🔊 MediaPlayer 생성 실패! soundResId: $soundResId")
                return
            }

            currentBgmTag = tag

            bgmPlayer?.apply {
                this.isLooping = isLooping
                setOnCompletionListener {
                    Timber.d("🔊 BGM 재생 완료: $tag")
                    if (!isLooping) {
                        it.release()
                        bgmPlayer = null
                        currentBgmTag = null
                    }
                }
                setOnErrorListener { mp, what, extra ->
                    Timber.e("🔊 BGM 재생 중 에러: tag=$tag, what=$what, extra=$extra")
                    mp.release()
                    bgmPlayer = null
                    currentBgmTag = null
                    true
                }
                start()
                Timber.d("🔊 BGM 재생 시작 성공: $tag")
            }
        } catch (e: Exception) {
            Timber.e(e, "🔊 BGM 재생 실패: tag=$tag, resId=$soundResId")
            currentBgmTag = null
        }
    }

    // 반복 효과음 (헬리콥터 등)
    fun playSoundWithRepeat(@RawRes soundResId: Int, count: Int) {
        var remaining = count

        try {
            sfxPlayer?.release()

            sfxPlayer = MediaPlayer.create(context, soundResId).apply {
                setOnCompletionListener {
                    remaining--
                    Timber.d("🔊 반복 효과음: $remaining 남음")
                    if (remaining > 0) {
                        it.start()
                    } else {
                        it.release()
                        sfxPlayer = null
                    }
                }
                start()
            }
            Timber.d("🔊 반복 효과음 재생: $soundResId, count: $count")
        } catch (e: Exception) {
            Timber.e(e, "🔊 반복 효과음 재생 실패")
        }
    }

    // 특정 태그의 BGM만 정지
    fun stopBgm(tag: String? = null) {
        if (tag != null && currentBgmTag != tag) {
            Timber.d("🔊 BGM 태그 불일치, 정지 안 함: current=$currentBgmTag, requested=$tag")
            return
        }

        bgmPlayer?.let {
            Timber.d("🔊 BGM 정지: $tag")
            if (it.isPlaying) it.stop()
            it.release()
        }
        bgmPlayer = null
        currentBgmTag = null
    }

    // 모든 사운드 정지 (게임 종료 시에만 사용)
    fun release() {
        Timber.d("🔊 모든 사운드 해제")

        sfxPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        sfxPlayer = null

        bgmPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        bgmPlayer = null
        currentBgmTag = null
    }
}