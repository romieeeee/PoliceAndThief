package com.d104.pnt.data.repository

import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.GameMemberSocketDto
import com.d104.pnt.data.remote.model.response.MissionSocketDto
import com.d104.pnt.util.StepSensorManager
import com.d104.pnt.util.socket.GameSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameSessionRepositoryImpl @Inject constructor(
    private val gameSocketManager: GameSocketManager,
    private val stepSensorManager: StepSensorManager,
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository
): GameSessionRepository {
    // 실시간 데이터를 저장할 메모리 공간
    private val _gameId = MutableStateFlow(0L)
    override val gameId = _gameId.asStateFlow()
    private val _gameStatus = MutableStateFlow("")
    override val gameStatus = _gameStatus.asStateFlow()
    private val _missions = MutableStateFlow<List<MissionSocketDto>>(emptyList())
    override val missions = _missions.asStateFlow()
    private val _members = MutableStateFlow<List<GameMemberSocketDto>>(emptyList())
    override val members = _members.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var gpsJob: Job? = null

    private var gameStartTime: Long = 0L

    override fun gameInit(){
        // 1. 기존 리스너 정리 (혹시 남아있을 수 있으므로)
        gameSocketManager.removeAllListeners()
        // 2. 리스너 등록
        setupSocketListeners()
        // 3. 정보 동기화 요청 (약간의 딜레이 후)
        repositoryScope.launch {
            delay(500L)
            gameSocketManager.syncGameInfo()
        }
    }

    private fun setupSocketListeners() {
        gameSocketManager.setOnGameInfoSynced { data ->
            try {
                val membersArray = data.optJSONArray("members")
                if (membersArray != null) {
                    val newMembers = mutableListOf<GameMemberSocketDto>()
                    for (i in 0 until membersArray.length()) {
                        val memberJson = membersArray.getJSONObject(i)
                        newMembers.add(GameMemberSocketDto.fromJson(memberJson))
                    }

                    _members.value = newMembers
                    Timber.d("GamePlayViewModel: 전체 멤버 동기화 완료 (${newMembers.size}명)")
                }
            } catch (e: Exception) {
                Timber.e(e, "GamePlayViewModel: 게임 정보 파싱 실패")
            }
        }

        // 실시간 상태 변경
        gameSocketManager.setOnMemberStatusChanged { gameId, thiefId, status, arrestedAt ->
            _members.update { currentList ->
                // currentList는 현재 시점의 최신 데이터임이 보장됨
                currentList.map { member ->
                    if (member.memberId == thiefId) {
                        member.copy(rawStatus = status)
                    } else {
                        member
                    }
                }
            }
        }

    }

    override fun startGameSession(){
        if (gpsJob?.isActive == true) return

        gpsJob?.cancel()

        // 걸음 수 리셋 (이전에 만든 기능 활용)
        stepSensorManager.resetGameSteps()
        gameStartTime = System.currentTimeMillis()

        gpsJob = repositoryScope.launch {
            Timber.d("🚀 레포지토리: GPS 전송 시작")
            while (isActive) {
                val location = locationRepository.currentLocation.value
                // 리셋된 걸음 수를 가져옴 (StepSensorManager에 gameStepCountFlow가 있다고 가정)
                val steps = stepSensorManager.stepCountFlow.value

                if (location != null) {
                    val longestSurvived =
                        ((System.currentTimeMillis() - gameStartTime) / 1000).toInt()

                    gameSocketManager.sendGPS(
                        lat = location.latitude,
                        lng = location.longitude,
                        walk = steps,
                        longestSurvived = longestSurvived
                    )
                }
                delay(1000L)
            }
        }
    }

    // 서비스 종료 시 호출할 함수
    override fun stopGameSession() {
        gpsJob?.cancel()
        gpsJob = null
        gameStartTime = 0L
        Timber.d("레포지토리: GPS 전송 중단")
    }

    override fun leaveGame() {
        stopGameSession() // 안전하게 트래킹 종료
        gameSocketManager.leaveGame()
        _members.value = emptyList()
    }
}