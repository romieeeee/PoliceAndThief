package com.d104.pnt.ui.game.end

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.R
import com.d104.pnt.data.remote.model.response.GameResultResponse
import com.d104.pnt.data.repository.GameRepository
import com.d104.pnt.data.repository.GameRoomRepository
import com.d104.pnt.data.repository.ReportRepository
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.navigation.NavArgs
import com.d104.pnt.util.socket.GameSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GameResultViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val roomRepository: GameRoomRepository,
    private val gameRepository: GameRepository,
    private val gameSocketManager: GameSocketManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val gameId: Long = savedStateHandle.get<Long>(NavArgs.GAME_ID) ?: 0L

    private val _uiState = MutableStateFlow<UiState<GameResultUiData>>(UiState.Loading)
    val uiState: StateFlow<UiState<GameResultUiData>> = _uiState.asStateFlow()

    // 신고 관련 상태
    var reportStep by mutableStateOf(ReportStep.NONE)
        private set

    var draft by mutableStateOf(ReportDraft())
        private set

    var reportSendState by mutableStateOf<UiState<Unit>>(UiState.Idle)
        private set

    init {
        fetchGameResult()
    }

    // fetch
    private fun fetchGameResult() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val data = when (val result = gameRepository.getGameResult(gameId)) {
                is BaseResult.Success -> result.data
                is BaseResult.Error -> {
                    _uiState.value = UiState.Error(result.error.message ?: "결과 조회 실패")
                    return@launch
                }
            }
            Timber.d("data: $data")

            val myStat   = data.myStat
            val myRole   = myStat.role
            val isPolice = myRole == "POLICE"

            val myGameStat = if (isPolice) "${myStat.arrestCount}명"
            else formatSeconds(myStat.longestSurvived)

            val myBestStat = if (isPolice) "${myStat.maxArrestCount}명"
            else formatSeconds(myStat.maxSurvivalTime)

            _uiState.value = UiState.Success(
                mapToUiData(
                    data       = data,
                    myRole     = myRole,
                    myTierName = myStat.rank,
                    myBestStat = myBestStat,
                    myGameStat = myGameStat
                )
            )
        }
    }

    // mapToUiData

    private fun mapToUiData(
        data: GameResultResponse,
        myRole: String,
        myTierName: String,
        myBestStat: String,
        myGameStat: String
    ): GameResultUiData {

        val amIPolice      = (myRole == "POLICE")
        val winnerIsPolice = (data.winner == "POLICE")
        val isWin          = (amIPolice && winnerIsPolice) || (!amIPolice && !winnerIsPolice)

        val mvpList = mutableListOf<MvpData>()

        fun getStatLabel(role: String) = if (role == "POLICE") "체포한 도둑 수" else "최장 생존 시간"

        data.mvp?.let            { mvpList.add(MvpData("MVP",   if (it.role == "POLICE") "경찰" else "도둑", it.nickname, getStatLabel(it.role), if (it.role == "Police") it.arrestCount.toString() else formatSeconds(it.longestSurvived), android.R.drawable.star_on)) }
        data.winningSecond?.let  { mvpList.add(MvpData("조력자", if (it.role == "POLICE") "경찰" else "도둑", it.nickname, getStatLabel(it.role), if (it.role == "Police") it.arrestCount.toString() else formatSeconds(it.longestSurvived), android.R.drawable.ic_menu_myplaces)) }
        data.losingFirst?.let    { mvpList.add(MvpData("ACE",   if (it.role == "POLICE") "경찰" else "도둑", it.nickname, getStatLabel(it.role), if (it.role == "Police") it.arrestCount.toString() else formatSeconds(it.longestSurvived), android.R.drawable.ic_menu_mylocation)) }

        val tierIcon = when (myTierName) {
            "순경", "바늘도둑"   -> if (amIPolice) R.drawable.police_lv1  else R.drawable.thief_lv1
            "경장", "좀도둑"    -> if (amIPolice) R.drawable.police_lv2  else R.drawable.thief_lv2
            "경사", "소매치기"   -> if (amIPolice) R.drawable.police_lv3  else R.drawable.thief_lv3
            "경위", "빈집털이"   -> if (amIPolice) R.drawable.police_lv4  else R.drawable.thief_lv4
            "경감", "소도둑"    -> if (amIPolice) R.drawable.police_lv5  else R.drawable.thief_lv5
            "경정", "금고털이"   -> if (amIPolice) R.drawable.police_lv6  else R.drawable.thief_lv6
            "총경", "은행털이"   -> if (amIPolice) R.drawable.police_lv7  else R.drawable.thief_lv7
            "경무관", "홍길동"   -> if (amIPolice) R.drawable.police_lv8  else R.drawable.thief_lv8
            "치안감", "인비저블"  -> if (amIPolice) R.drawable.police_lv9  else R.drawable.thief_lv9
            "치안정감", "괴도"   -> if (amIPolice) R.drawable.police_lv10 else R.drawable.thief_lv10
            "치안총감", "대도"   -> if (amIPolice) R.drawable.police_lv11 else R.drawable.thief_lv11
            else               -> if (amIPolice) R.drawable.police_lv1  else R.drawable.thief_lv1
        }
        Timber.d("mvpList: $mvpList")

        return GameResultUiData(
            isPolice      = amIPolice,
            isWin         = isWin,
            mvpList       = mvpList,
            myTierIconRes = tierIcon,
            myGameStat    = myGameStat,
            myBestStat    = myBestStat
        )
    }

    private fun formatSeconds(totalSec: Int): String {
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    // 신고

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
                }
            }
        }
    }

    private fun mapReasonKrToServerEnum(reasonKr: String): String =
        when (reasonKr) {
            "욕설"      -> "ABUSE"
            "폭행"      -> "ASSAULT"
            "비매너"    -> "BAD_MANNER"
            "구역 이탈" -> "OUT_OF_AREA"
            else        -> "ETC"
        }

    // 네비게이션

    fun backToLobby(roomId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = roomRepository.changePosition(roomId, "ANY")
            if (result is BaseResult.Success) {
                cleanupGameSocket()
                onSuccess()
            } else {
                Timber.e("역할 리셋 실패: 대기방 진입 중단")
            }
        }
    }

    fun cleanupGameSocket() {
        viewModelScope.launch {
            Timber.d("📡 결과 화면 종료 - 게임 소켓 정리")
            gameSocketManager.disconnect()
        }
    }
}


data class ReportDraft(
    val nickname: String = "",
    val reasonKr: String = "욕설",
    val detail: String = ""
)