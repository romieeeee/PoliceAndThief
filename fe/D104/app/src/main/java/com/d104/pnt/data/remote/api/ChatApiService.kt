package com.d104.pnt.data.remote.api

import com.d104.pnt.data.remote.model.response.BaseResponse
import com.d104.pnt.data.remote.model.request.ChatCreateRequest
import com.d104.pnt.data.remote.model.response.ChatCreateResponse
import com.d104.pnt.data.remote.model.response.ChatSearchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ChatApiService {
    @POST("chats")
    suspend fun createChatRoom(
        @Body request: ChatCreateRequest
    ): Response<BaseResponse<ChatCreateResponse>>

    @GET("chats")
    suspend fun getFilteredChatRoom(
        @Query("title") title: String?,
        @Query("regionCode") regionCode: Int?,
    ): Response<BaseResponse<ChatSearchResponse>>

    @GET("chats/me/rooms")
    suspend fun getJoinedChatRoom(): Response<BaseResponse<ChatSearchResponse>>
}