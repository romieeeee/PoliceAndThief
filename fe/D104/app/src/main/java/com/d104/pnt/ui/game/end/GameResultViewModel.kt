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
import com.d104.pnt.data.repository.GameSessionRepository
import com.d104.pnt.data.repository.ReportRepository
import com.d104.pnt.domain.model.GameRole
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
    private val gameSessionRepository: GameSessionRepository,
    private val gameSocketManager: GameSocketManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val gameId: Long = savedStateHandle.get<Long>(NavArgs.GAME_ID) ?: 0L

    private val _uiState = MutableStateFlow<UiState<GameResultUiData>>(UiState.Loading)
    val uiState: StateFlow<UiState<GameResultUiData>> = _uiState.asStateFlow()

    private val _rejoinState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val rejoinState: StateFlow<UiState<Unit>> = _rejoinState.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    // 신고 관련 상태
    var reportStep by mutableStateOf(ReportStep.NONE)
        private set

    var draft by mutableStateOf(ReportDraft())
        private set

    var reportSendState by mutableStateOf<UiState<Unit>>(UiState.Idle)
        private set

    init {
        fetchGameResult()
        startCountdown()
    }

    private fun startCountdown() {
        viewModelScope.launch {
            // 30초부터 0초까지 1초씩 감소
            while (_remainingSeconds.value > 0) {
                delay(1000L)
                _remainingSeconds.value -= 1
            }
        }
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
        val savedNicknames = gameSessionRepository.getPlayerNicknames()

        fun getStatLabel(role: String) = if (role == "POLICE") "체포한 도둑 수" else "최장 생존 시간"

        data.mvp?.let            { mvpList.add(MvpData("MVP",   if (it.role == "POLICE") "경찰" else "도둑", it.nickname, getStatLabel(it.role), if (it.role == "POLICE") it.arrestCount.toString() else formatSeconds(it.longestSurvived), android.R.drawable.star_on)) }
        data.winningSecond?.let  { mvpList.add(MvpData("조력자", if (it.role == "POLICE") "경찰" else "도둑", it.nickname, getStatLabel(it.role), if (it.role == "POLICE") it.arrestCount.toString() else formatSeconds(it.longestSurvived), android.R.drawable.ic_menu_myplaces)) }
        data.losingFirst?.let    { mvpList.add(MvpData("ACE",   if (it.role == "POLICE") "경찰" else "도둑", it.nickname, getStatLabel(it.role), if (it.role == "POLICE") it.arrestCount.toString() else formatSeconds(it.longestSurvived), android.R.drawable.ic_menu_mylocation)) }

        val tierIcon = when (myTierName) {
            "순경", "바늘도둑"   -> if (amIPolice) R.drawable.police_lv1  else R.drawable.thief_lv1
            "경장", "좀도둑"    -> if (amIPolice) R.drawable.police_lv2  else R.drawable.thief_lv2
            "경사", "소매치기"   -> if (amIPolice) R.drawable.police_lv3  else R.drawable.thief_lv3
            "경위", "빈집털이"   -> if (amIPolice) R.drawable.police_lv4  else R.drawable.thief_lv4
            "경감", "소도둑"    -> if (amIPolice) R.drawable.police_lv5  else R.drawable.thief_lv5
            "경정", "금고털이"   -> if (amIPolice) R.drawable.police_lv6  else R.drawable.thief_lv6
            "총경", "은행털이"   -> if (amIPolice) R.drawable.police_lv7  else R.drawable.thief_lv7
            "경무관", "홍길동"   -> if (amIPolice) R.drawable.police_lv8  else R.drawable.thief_lv8
            "치안감", "인비져블"  -> if (amIPolice) R.drawable.police_lv9  else R.drawable.thief_lv9
            "치안정감", "괴도"   -> if (amIPolice) R.drawable.police_lv10 else R.drawable.thief_lv10
            "치안총감", "대도"   -> if (amIPolice) R.drawable.police_lv11 else R.drawable.thief_lv11
            else               -> if (amIPolice) R.drawable.police_lv1  else R.drawable.thief_lv1
        }
        Timber.d("mvpList: $mvpList")

        val allNicknames = if (savedNicknames.isNotEmpty()) {
            savedNicknames
        } else {
            listOfNotNull(
                data.mvp?.nickname,
                data.winningSecond?.nickname,
                data.losingFirst?.nickname,
                data.myStat.nickname
            ).distinct()
        }

        val myNickname = data.myStat.nickname
        val reportableNicknames = allNicknames.filter { it != myNickname }

        return GameResultUiData(
            isPolice      = amIPolice,
            isWin         = isWin,
            mvpList       = mvpList,
            myTierIconRes = tierIcon,
            myGameStat    = myGameStat,
            myBestStat    = myBestStat,
            allPlayerNicknames = reportableNicknames
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

    fun backToLobby(onSuccess: (Long) -> Unit) {
        // 카운트다운 완료 체크
        if (_remainingSeconds.value > 0) {
            Timber.w("⚠️ 아직 대기방 입장 불가 (${_remainingSeconds.value}초 남음)")
            return
        }

        if (_rejoinState.value is UiState.Loading) {
            Timber.w("⚠️ 이미 대기방 입장 처리 중")
            return
        }

        viewModelScope.launch {
            try {
                _rejoinState.value = UiState.Loading
                Timber.d("📍 대기방 재입장 시도 시작 (gameId: $gameId)")

                // 1. 게임 소켓 정리
                cleanupGameSocket()
                delay(500) // 소켓 완전히 끊기까지 대기

                // 2. roomCode 가져오기
                val roomCode = gameSessionRepository.roomCode.first()
                if (roomCode.isBlank()) {
                    _rejoinState.value = UiState.Error("방 코드를 찾을 수 없습니다")
                    Timber.e("❌ roomCode 없음 - 대기방 입장 불가")
                    return@launch
                }

                Timber.d("✅ roomCode 확인: $roomCode")

                // 3. 서버에 재입장 요청 (최대 3번 재시도)
                var retryCount = 0
                var joinResult: BaseResult<*>? = null

                while (retryCount < 3) {
                    joinResult = roomRepository.joinGameRoom(roomCode)

                    when (joinResult) {
                        is BaseResult.Success -> {
                            Timber.d("✅ 방 재입장 성공 (시도 ${retryCount + 1})")
                            break
                        }
                        is BaseResult.Error -> {
                            retryCount++
                            val errorMsg = joinResult.error.message ?: "알 수 없는 오류"

                            if (errorMsg.contains("아직 초기화되지 않았습니다") ||
                                errorMsg.contains("not ready") ||
                                errorMsg.contains("waiting")) {
                                Timber.w("⏳ 서버 초기화 대기 중... (${retryCount}/3)")
                                delay(2000) // 2초 대기 후 재시도
                            } else {
                                // 다른 에러는 즉시 실패 처리
                                Timber.e("❌ 재입장 실패: $errorMsg")
                                _rejoinState.value = UiState.Error(errorMsg)
                                return@launch
                            }
                        }
                    }
                }

                // 4. 최종 결과 확인
                when (joinResult) {
                    is BaseResult.Success -> {
                        val roomId = (joinResult as BaseResult.Success<*>).data
                        val actualRoomId = when (roomId) {
                            is Long -> roomId
                            is Map<*, *> -> (roomId["roomId"] as? Number)?.toLong() ?: gameId
                            else -> gameId
                        }

                        // 5. 역할 초기화
                        when (val positionResult = roomRepository.changePosition(actualRoomId, GameRole.ANY.roleNameEn)) {
                            is BaseResult.Success -> {
                                Timber.d("✅ 역할 ANY로 초기화 완료")
                                _rejoinState.value = UiState.Success(Unit)
                                onSuccess(actualRoomId)
                            }
                            is BaseResult.Error -> {
                                Timber.e("❌ 역할 초기화 실패: ${positionResult.error.message}")
                                _rejoinState.value = UiState.Error("역할 초기화 실패")
                            }
                        }
                    }
                    is BaseResult.Error -> {
                        val errorMsg = (joinResult as BaseResult.Error).error.message ?: "재입장 실패"
                        Timber.e("❌ 최종 재입장 실패: $errorMsg")
                        _rejoinState.value = UiState.Error("방이 아직 준비되지 않았습니다.\n잠시 후 다시 시도해주세요.")
                    }
                    null -> {
                        _rejoinState.value = UiState.Error("알 수 없는 오류")
                    }
                }

            } catch (e: Exception) {
                Timber.e(e, "❌ 대기방 입장 중 예외 발생")
                _rejoinState.value = UiState.Error("오류가 발생했습니다: ${e.message}")
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