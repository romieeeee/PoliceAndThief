package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.model.response.GameNewsResponse
import com.d104.pnt.data.remote.model.response.GameResultResponse
import com.d104.pnt.data.remote.model.response.MissionResponse
import com.d104.pnt.domain.model.common.BaseResult

interface GameRepository {

    suspend fun getMissionList(gameId: Long): BaseResult<List<MissionResponse>>

    suspend fun getMissionDetail(gameId: Long, missionsId: Long): BaseResult<MissionResponse>

    suspend fun gameHardDelete(gameId: Long): BaseResult<Unit>

    suspend fun getGameNews(gameId: Long): BaseResult<GameNewsResponse>

    suspend fun getGameResult(gameId: Long): BaseResult<GameResultResponse>

    var myLastGameStat: String?
}