package com.d104.pnt.data.remote.api

import com.d104.pnt.data.remote.model.request.CreateGameRoomRequest
import com.d104.pnt.data.remote.model.response.BaseResponse
import com.d104.pnt.data.remote.model.response.CreateGameRoomResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GameRoomApiService {

    @POST("rooms")
    suspend fun createGameRoom(
        @Body request: CreateGameRoomRequest
    ): Response<BaseResponse<CreateGameRoomResponse>>

}