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
class AiNewsLoadingViewModel @Inject constructor(
    private val gameSessionRepository: GameSessionRepository,
    private val gameRepository: GameRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val gameId: Long = savedStateHandle.get<Long>(NavArgs.GAME_ID) ?: 0L
    private val _uiEvent = MutableSharedFlow<NewsLoadingUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        observeNewsSignal()
    }

    private fun observeNewsSignal() {
        viewModelScope.launch {
            gameSessionRepository.eventFlow.collect { event ->
                if (event is GameSessionEvent.NavigateToNews) {
                    Timber.d("📺 뉴스 생성 신호 수신! (gameId: ${event.gameId})")
                    // 소켓으로 받은 gameId가 0이라면 세이프하게 현재 들고있는 gameId 사용
                    val targetGameId = if (event.gameId != 0L) event.gameId else gameId
                    fetchNewsContent(targetGameId, event.newsId)
                }
            }
        }
    }

    private suspend fun fetchNewsContent(gId: Long, nId: Long) {
        // 서버 DB 반영 시간을 위해 최대 3번 재시도
        repeat(3) { attempt ->
            when (val result = gameRepository.getGameNews(gId)) {
                is BaseResult.Success -> {
                    Timber.d("✅ 뉴스 데이터 가져오기 성공!")
                    _uiEvent.emit(NewsLoadingUiEvent.NavigateToActualNews(gId, nId))
                    return@fetchNewsContent // 성공 시 탈출
                }
                is BaseResult.Error -> {
                    Timber.e("❌ 뉴스 호출 실패 (시도 ${attempt + 1}): ${result.error.message}")
                    delay(1000) // 1초 대기 후 재시도
                }
            }
        }
    }

}

sealed class NewsLoadingUiEvent {
    data class NavigateToActualNews(val gameId: Long, val newsId: Long) : NewsLoadingUiEvent()
}
