package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.api.GameApiService
import com.d104.pnt.data.remote.model.response.GameNewsResponse
import com.d104.pnt.data.remote.model.response.GameResultResponse
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

    override suspend fun getGameNews(gameId: Long): BaseResult<GameNewsResponse> {
        return safeApiCall { gameApiService.getGameNews(gameId) }
    }

    override suspend fun getGameResult(gameId: Long): BaseResult<GameResultResponse> {

        return safeApiCall { gameApiService.getGameResult(gameId) }
    }

    override var myLastGameStat: String? = null
}