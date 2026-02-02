package com.d104.pnt.ui.game.end

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.GameNewsResponse
import com.d104.pnt.data.repository.GameRepository
import com.d104.pnt.data.repository.ReportRepository
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.navigation.NavArgs
import com.d104.pnt.util.socket.GameSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GameResultViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val gameRepository: GameRepository,
    private val gameSocketManager: GameSocketManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val gameId: Long = savedStateHandle.get<Long>(NavArgs.GAME_ID) ?: 0L
    private val newsId: Long = savedStateHandle.get<Long>(NavArgs.NEWS_ID) ?: 0L

    // 뉴스 전송 상태
    var newsState by mutableStateOf<UiState<GameNewsResponse>>(UiState.Idle)
        private set

    var reportStep by mutableStateOf(ReportStep.NONE)
        private set

    var draft by mutableStateOf(ReportDraft())
        private set

    // 신고 전송 상태 (로딩/에러 표시용)
    var reportSendState by mutableStateOf<UiState<Unit>>(UiState.Idle)
        private set


    init {
        fetchNewsData(gameId)
    }


    private fun fetchNewsData(gId: Long) {
        if (gId == 0L) return

        viewModelScope.launch {
            newsState = UiState.Loading
            when (val result = gameRepository.getGameNews(gId)) {
                is BaseResult.Success -> {
                    newsState = UiState.Success(result.data)
                }

                is BaseResult.Error -> {
                    newsState = UiState.Error(result.error.message ?: "데이터 호출 실패")
                }
            }
        }
    }

    fun openReportDialog() {
        reportStep = ReportStep.INPUT
    }

    fun closeReportDialog() {
        draft = ReportDraft()
        reportSendState = UiState.Idle
        reportStep = ReportStep.NONE
    }

    fun onDraftSubmitted(nickname: String, reasonKr: String, detail: String) {
        draft = ReportDraft(nickname, reasonKr, detail)
        reportStep = ReportStep.CONFIRM
    }

    fun backToInput() {
        reportSendState = UiState.Idle
        reportStep = ReportStep.INPUT
    }

    fun confirmReport() {
        if (reportSendState is UiState.Loading) return

        viewModelScope.launch {
            reportSendState = UiState.Loading

            val reasonEnum = mapReasonKrToServerEnum(draft.reasonKr)

            when (val result =
                reportRepository.createReport(draft.nickname, reasonEnum, draft.detail)) {
                is BaseResult.Success -> {
                    reportSendState = UiState.Success(Unit)
                    draft = ReportDraft()
                    reportStep = ReportStep.SUCCESS
                }

                is BaseResult.Error -> {
                    reportSendState = UiState.Error(result.error.message ?: "신고 실패")
                    // CONFIRM 유지하면서 에러 보여주기 추천
                }
            }
        }
    }

    private fun mapReasonKrToServerEnum(reasonKr: String): String =
        when (reasonKr) {
            "욕설" -> "ABUSE"
            "폭행" -> "ASSAULT"
            "비매너" -> "BAD_MANNER"
            "구역 이탈" -> "OUT_OF_AREA"
            else -> "ETC"
        }

    fun cleanupGameSocket() {
        viewModelScope.launch {
            Timber.d("📡 뉴스 화면 종료 - 게임 소켓 정리")
            gameSocketManager.disconnect()
        }
    }
}


data class ReportDraft(
    val nickname: String = "",
    val reasonKr: String = "욕설",
    val detail: String = ""
)
