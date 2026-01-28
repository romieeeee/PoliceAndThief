package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.api.GameRoomApiService
import com.d104.pnt.data.remote.model.request.Location
import com.d104.pnt.data.remote.model.response.CreateGameRoomResponse
import com.d104.pnt.domain.model.common.BaseResult
import javax.inject.Inject

class GameRoomRepositoryImpl @Inject constructor(
    private val apiService: GameRoomApiService,
) : GameRoomRepository, BaseRepository() {
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

            }
        )
    }


}