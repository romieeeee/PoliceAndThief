package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.api.GameApiService
import com.d104.pnt.data.remote.model.response.MissionResponse
import com.d104.pnt.domain.model.common.BaseResult
import javax.inject.Inject

class GameRepositoryImpl @Inject constructor(
    private val gameApiService: GameApiService
) : GameRepository, BaseRepository() {

    override suspend fun getMissionList(gameId: Long): BaseResult<List<MissionResponse>> {
        return safeApiCall { gameApiService.getMissionList(gameId) }
    }

    override suspend fun getMissionDetail(
        gameId: Long,
        missionsId: Long
    ): BaseResult<MissionResponse> {
        return safeApiCall { gameApiService.getMissionDetail(gameId, missionsId) }
    }

    override suspend fun gameHardDelete(gameId: Long): BaseResult<Unit> {
        return safeApiCall { gameApiService.deleteGame(gameId) }
    }
}