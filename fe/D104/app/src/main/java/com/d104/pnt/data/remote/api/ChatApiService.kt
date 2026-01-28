package com.d104.pnt.data.remote.api

import com.d104.pnt.data.remote.model.response.BaseResponse
import com.d104.pnt.data.remote.model.request.ChatCreateRequest
import com.d104.pnt.data.remote.model.response.ChatCreateResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatApiService {
    @POST("chats")
    suspend fun createChatRoom(
        @Body request: ChatCreateRequest
    ): Response<BaseResponse<ChatCreateResponse>>
}