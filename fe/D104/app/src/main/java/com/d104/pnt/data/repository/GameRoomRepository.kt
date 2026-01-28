package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.model.request.Location
import com.d104.pnt.data.remote.model.response.CreateGameRoomResponse
import com.d104.pnt.domain.model.common.BaseResult

interface GameRoomRepository{

//    fun getCurrentGameRoom(): GameRoom? TODO: 데이터 클래스 만들기

    // 게임 방 생성
    suspend fun createGameRoom(
        playerCount: Int,
        timeLimit: Int,
        policeCount: Int,
        thiefCount: Int,
        prison: Location,
        polygon: List<Location>
    ): BaseResult<CreateGameRoomResponse>

    suspend fun joinCreatedGameRoom(
        roomId: Long,
        roomCode: String,
        status: String
    )
}