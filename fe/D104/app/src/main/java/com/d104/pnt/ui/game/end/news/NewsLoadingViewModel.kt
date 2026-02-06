package com.d104.pnt.ui.game.end.news

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.repository.GameRepository
import com.d104.pnt.data.repository.GameSessionEvent
import com.d104.pnt.data.repository.GameSessionRepository
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NewsLoadingViewModel @Inject constructor(
    private val gameSessionRepository: GameSessionRepository,
    private val gameRepository: GameRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val gameId: Long = savedStateHandle.get<Long>(NavArgs.GAME_ID) ?: 0L
    private val _uiEvent = MutableSharedFlow<NewsLoadingUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var newsReceived = false

    init {
        observeNewsSignal()
        startFallbackTimer()
    }

    private fun observeNewsSignal() {
        viewModelScope.launch {
            gameSessionRepository.eventFlow.collect { event ->
                if (event is GameSessionEvent.NavigateToNews) {
                    Timber.d("📺 뉴스 생성 신호 수신! (gameId: ${event.gameId})")
                    val targetGameId = if (event.gameId != 0L) event.gameId else gameId
                    fetchNewsContent(targetGameId, event.newsId)
                }
            }
        }
    }

    private fun startFallbackTimer() {
        viewModelScope.launch {
            delay(10000)
            if (!newsReceived) {
                Timber.w("⚠️ 뉴스 신호 타임아웃 - 강제 이동")
                newsReceived = true
                fetchNewsContent(gameId, 0L)
            }
        }
    }

    private suspend fun fetchNewsContent(gId: Long, nId: Long) {
        repeat(5) { attempt ->
            when (val result = gameRepository.getGameNews(gId)) {
                is BaseResult.Success -> {
                    _uiEvent.emit(NewsLoadingUiEvent.NavigateToActualNews(gId, nId))
                    return@fetchNewsContent
                }
                is BaseResult.Error -> {
                    Timber.e("❌ 뉴스 호출 실패 (시도 ${attempt + 1}): ${result.error.message}")
                    delay(2000)
                }
            }
        }
        _uiEvent.emit(NewsLoadingUiEvent.NavigateToActualNews(gId, nId))
    }

}

sealed class NewsLoadingUiEvent {
    data class NavigateToActualNews(val gameId: Long, val newsId: Long) : NewsLoadingUiEvent()
}
