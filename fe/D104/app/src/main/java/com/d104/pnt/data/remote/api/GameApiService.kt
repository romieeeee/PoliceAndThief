package com.d104.pnt.data.remote.api

import com.d104.pnt.data.remote.model.response.BaseResponse
import com.d104.pnt.data.remote.model.response.MissionResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface GameApiService {

    @GET("games/{gameId}/missions")
    suspend fun getMissionList(
        @Path("gameId") gameId: Long,
    ): Response<BaseResponse<List<MissionResponse>>>

    @GET("games/{gameId}/missions/{missionsId}")
    suspend fun getMissionDetail(
        @Path("gameId") gameId: Long,
        @Path("missionsId") missionsId: Long,
    ): Response<BaseResponse<MissionResponse>>
}