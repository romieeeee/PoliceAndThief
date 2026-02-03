package com.d104.pnt.data.remote.api

import com.d104.pnt.data.remote.model.request.LiveKitTokenRequest
import com.d104.pnt.data.remote.model.response.BaseResponse
import com.d104.pnt.data.remote.model.response.GameNewsResponse
import com.d104.pnt.data.remote.model.response.GameResultResponse
import com.d104.pnt.data.remote.model.response.LiveKitTokenResponse
import com.d104.pnt.data.remote.model.response.MissionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
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

    @DELETE("dev/games/{gameId}")
    suspend fun deleteGame(
        @Path("gameId") gameId: Long
    ): Response<BaseResponse<Unit>>

    @GET("games/news/{gameId}")
    suspend fun getGameNews(
        @Path("gameId") gameId: Long
    ): Response<BaseResponse<GameNewsResponse>>

    @GET("games/{gameId}/result")
    suspend fun getGameResult(
        @Path("gameId") gameId: Long
    ): Response<BaseResponse<GameResultResponse>>

    @POST("api/livekit/token")
    suspend fun getLiveKitToken(
        @Body request: LiveKitTokenRequest
    ): Response<BaseResponse<LiveKitTokenResponse>>
}