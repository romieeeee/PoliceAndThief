package com.d104.pnt.data.remote.api

import com.d104.pnt.data.remote.model.response.BaseResponse
import com.d104.pnt.data.remote.model.response.ChatCreateRequest
import com.d104.pnt.data.remote.model.response.ChatCreateResponse
import com.d104.pnt.data.remote.model.response.JoinChatRoomResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatApiService {
    /**
     * 게임 생성 API
     */
    @POST("chats")
    suspend fun createChatRoom(
        @Body request: ChatCreateRequest
    ): Response<BaseResponse<ChatCreateResponse>>

    /**
     * 채팅방 참여
     */
    @POST("chats/{id}/join")
    suspend fun joinChatRoom(
        @Path("id") chatRoomId: Long
    ): Response<BaseResponse<JoinChatRoomResponse>>

    /**
     * 채팅방 연결
     */
    // 🔥 채팅방 연결
    @POST("chats/{id}/connect")
    suspend fun connectChatRoom(
        @Path("id") chatRoomId: Long
    ): Response<BaseResponse<Unit>>
}
