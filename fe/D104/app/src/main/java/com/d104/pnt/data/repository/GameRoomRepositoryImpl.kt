package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.api.GameRoomApiService
import com.d104.pnt.data.remote.model.request.CreateGameRoomRequest
import com.d104.pnt.data.remote.model.request.Location
import com.d104.pnt.data.remote.model.response.CreateGameRoomResponse
import com.d104.pnt.domain.model.CurrentGameRoomData
import com.d104.pnt.domain.model.common.BaseResult
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class GameRoomRepositoryImpl @Inject constructor(
    private val apiService: GameRoomApiService,
) : GameRoomRepository, BaseRepository() {
    private val _currentGameRoom = MutableStateFlow<CurrentGameRoomData?>(null)

    override fun getCurrentGameRoom(): StateFlow<CurrentGameRoomData?> {
        return _currentGameRoom.asStateFlow()
    }

    override suspend fun createGameRoom(
        playerCount: Int,
        timeLimit: Int,
        policeCount: Int,
        thiefCount: Int,
        prison: Location,
        polygon: List<Location>
    ): BaseResult<CreateGameRoomResponse> {
        return safeApiCall(
            onSuccess = { createGameRoomResponse ->
                updateLocalGameRoom(
                    roomId = createGameRoomResponse.roomId,
                    roomCode = createGameRoomResponse.roomCode,
                    status = createGameRoomResponse.status,
                )
            }
        ) {
            apiService.createGameRoom(
                CreateGameRoomRequest(
                    playerCount,
                    timeLimit,
                    policeCount,
                    thiefCount,
                    prison,
                    polygon
                )
            )
        }
    }

    override suspend fun updateLocalGameRoom(
        roomId: Long,
        roomCode: String,
        status: String
    ) {
        val roomData = CurrentGameRoomData(
            roomId = roomId,
            roomCode = roomCode,
            status = status,
            timeLimit = 0,
            cctvFrequency = 0,
            missionCount = 0,
            playerCount = 0,
            maxPlayerCount = 0,
            myMemberId = 0,
            roomManagerId = 0,
            policeCount = 0,
            thiefCount = 0,
            missionCompleteCount = 0,
            preferPosition = "",
            prison = LatLng(0.0, 0.0),
            polygon = emptyList(),
        )
        _currentGameRoom.value = roomData
    }
}