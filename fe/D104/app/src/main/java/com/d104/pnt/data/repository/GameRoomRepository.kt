package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.model.request.Location
import com.d104.pnt.data.remote.model.response.CreateGameRoomResponse
import com.d104.pnt.data.remote.model.response.GameMemberListResponse
import com.d104.pnt.data.remote.model.response.GameRoomSettingsResponse
import com.d104.pnt.domain.model.CurrentGameRoomData
import com.d104.pnt.domain.model.common.BaseResult
import kotlinx.coroutines.flow.StateFlow

interface GameRoomRepository{

    fun getCurrentGameRoom(): StateFlow<CurrentGameRoomData?>

    suspend fun createGameRoom(
        playerCount: Int,
        timeLimit: Int,
        cctvInterval: Int,
        missionCount: Int,
        policeCount: Int,
        thiefCount: Int,
        prison: Location,
        polygon: List<Location>
    ): BaseResult<CreateGameRoomResponse>

    suspend fun updateLocalGameRoom(
        roomId: Long,
        roomCode: String,
        status: String,
        prison: Location? = null,
        polygon: List<Location>? = null
    )

    suspend fun joinGameRoom(roomCode: String): BaseResult<CreateGameRoomResponse>

    suspend fun getRoomMembers(roomId: Long): BaseResult<GameMemberListResponse>
    suspend fun getRoomSettings(roomId: Long): BaseResult<GameRoomSettingsResponse>
    suspend fun toggleReady(roomId: Long, isReady: Boolean): BaseResult<Unit>
    suspend fun changePosition(roomId: Long, position: String): BaseResult<Unit>
    suspend fun startGame(roomId: Long): BaseResult<Unit>
    suspend fun leaveRoom(roomId: Long): BaseResult<Unit>

    suspend fun updateRoomSettings(
        roomId: Long,
        playerCount: Int,
        timeLimit: Int,
        cctvInterval: Int,
        policeCount: Int,
        thiefCount: Int,
        missionCount: Int,
        prison: Location?,
        polygon: List<Location>?
    ): BaseResult<Unit>
}