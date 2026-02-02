package com.d104.pnt.ui.game.end

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.R
import com.d104.pnt.data.remote.model.response.GameResultResponse
import com.d104.pnt.data.remote.model.response.GameStats
import com.d104.pnt.data.remote.model.response.PlayerResult
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.GameRepository
import com.d104.pnt.data.repository.GameRoomRepository
import com.d104.pnt.data.repository.ProfileRepository
import com.d104.pnt.data.repository.ReportRepository
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.navigation.NavArgs
import com.d104.pnt.util.socket.GameSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GameResultViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val roomRepository: GameRoomRepository,
    private val gameRepository: GameRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val gameSocketManager: GameSocketManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val gameId: Long = savedStateHandle.get<Long>(NavArgs.GAME_ID) ?: 0L
    private val navArgRole: String? = savedStateHandle.get<String>("myRole")
    private val navArgMyStat: String? = savedStateHandle.get<String>("myStat")

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


    private fun fetchGameResult() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            // 내 정보 확인
            val myMemberId = try {
                authRepository.getMemberId().first()
            } catch (e: Exception) {
                0L
            }

            // 실제 게임 결과 API
            when (val result = gameRepository.getGameResult(gameId)) {
                is BaseResult.Success -> {
                    val data = result.data

                    val myRole = navArgRole ?: "THIEF"

                    // 내 등급 및 최고 기록
                    var myTierName = "Unranked"
                    var myBestStat = "기록 없음"

                    if (myMemberId != 0L) {
                        if (myRole == "POLICE") {
                            when (val profileResult = profileRepository.getPoliceStat(myMemberId)) {
                                is BaseResult.Success -> {
                                    val stat = profileResult.data.policeStat
                                    myTierName = stat.grade ?: "Unranked"
                                    myBestStat = "${stat.mostArrestsInGame}명"
                                }
                                is BaseResult.Error -> Timber.e("경찰 스탯 조회 실패")
                            }
                        } else {
                            when (val profileResult = profileRepository.getThiefStat(myMemberId)) {
                                is BaseResult.Success -> {
                                    val stat = profileResult.data.thiefStat
                                    myTierName = stat.grade ?: "Unranked"
                                    val min = stat.longestSurvivalSec / 60
                                    val sec = stat.longestSurvivalSec % 60
                                    myBestStat = String.format(Locale.getDefault(), "%02d:%02d", min, sec)
                                }
                                is BaseResult.Error -> Timber.e("도둑 스탯 조회 실패")
                            }
                        }
                    }

                    val savedStat = gameRepository.myLastGameStat

                    // 이번 판 내 기록
                    val myGameStat = navArgMyStat ?: if (myRole == "POLICE") "0명" else "00:00"

                    // UI 데이터
                    val uiData = mapToUiData(
                        data = data,
                        myRole = myRole,
                        myTierName = myTierName,
                        myBestStat = myBestStat,
                        myGameStat = myGameStat
                    )

                    _uiState.value = UiState.Success(uiData)
                }

                is BaseResult.Error -> {
                    _uiState.value = UiState.Error(result.error.message ?: "결과 조회 실패")
                }
            }
        }
    }

    private fun mapToUiData(
        data: GameResultResponse,
        myRole: String,
        myTierName: String,
        myBestStat: String,
        myGameStat: String
    ): GameResultUiData {

        val amIPolice = (myRole == "POLICE")
        val winnerIsPolice = (data.winner == "POLICE")
        val isWin = (amIPolice && winnerIsPolice) || (!amIPolice && !winnerIsPolice)

        val mvpList = mutableListOf<MvpData>()

        fun getStatLabel(role: String) = if (role == "POLICE") "체포한 도둑 수" else "최장 생존 시간"

        data.mvp?.let { mvpList.add(MvpData("MVP", if (it.role == "POLICE") "경찰" else "도둑", it.nickname, getStatLabel(it.role), it.description, android.R.drawable.star_on)) }
        data.winningSecond?.let { mvpList.add(MvpData("조력자", if (it.role == "POLICE") "경찰" else "도둑", it.nickname, getStatLabel(it.role), it.description, android.R.drawable.ic_menu_myplaces)) }
        data.losingFirst?.let { mvpList.add(MvpData("ACE", if (it.role == "POLICE") "경찰" else "도둑", it.nickname, getStatLabel(it.role), it.description, android.R.drawable.ic_menu_mylocation)) }

        val tierIcon = when (myTierName) {
            "순경", "바늘도둑" -> if (amIPolice) R.drawable.police_lv1 else R.drawable.thief_lv1
            "경장", "좀도둑" -> if (amIPolice) R.drawable.police_lv2 else R.drawable.thief_lv2
            "경사", "소매치기" -> if (amIPolice) R.drawable.police_lv3 else R.drawable.thief_lv3
            "경위", "빈집털이" -> if (amIPolice) R.drawable.police_lv4 else R.drawable.thief_lv4
            "경감", "소도둑" -> if (amIPolice) R.drawable.police_lv5 else R.drawable.thief_lv5
            "경정", "금고털이" -> if (amIPolice) R.drawable.police_lv6 else R.drawable.thief_lv6
            "총경", "은행털이" -> if (amIPolice) R.drawable.police_lv7 else R.drawable.thief_lv7
            "경무관", "홍길동" -> if (amIPolice) R.drawable.police_lv8 else R.drawable.thief_lv8
            "치안감", "인비저블" -> if (amIPolice) R.drawable.police_lv9 else R.drawable.thief_lv9
            "치안정감", "괴도" -> if (amIPolice) R.drawable.police_lv10 else R.drawable.thief_lv10
            "치안총감", "대도" -> if (amIPolice) R.drawable.police_lv11 else R.drawable.thief_lv11

            // 기본값
            else -> if (amIPolice) R.drawable.police_lv1 else R.drawable.thief_lv1
        }

        return GameResultUiData(
            isPolice = amIPolice,
            isWin = isWin,
            mvpList = mvpList,
            myTierIconRes = tierIcon,
            myGameStat = myGameStat,
            myBestStat = myBestStat
        )
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
