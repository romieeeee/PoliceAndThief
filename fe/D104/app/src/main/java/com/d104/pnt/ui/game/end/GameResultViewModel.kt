package com.d104.pnt.ui.game.end

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.repository.ReportRepository
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class GameResultViewModel @Inject constructor(
    private val reportRepository: ReportRepository
) : ViewModel() {

    var reportStep by mutableStateOf(ReportStep.NONE)
        private set

    var draft by mutableStateOf(ReportDraft())
        private set

    // 신고 전송 상태 (로딩/에러 표시용)
    var reportSendState by mutableStateOf<UiState<Unit>>(UiState.Idle)
        private set

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

            when (val result = reportRepository.createReport(draft.nickname, reasonEnum, draft.detail)) {
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
}

data class ReportDraft(
    val nickname: String = "",
    val reasonKr: String = "욕설",
    val detail: String = ""
)
