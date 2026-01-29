package com.d104.pnt.data.remote.api

import com.d104.pnt.data.remote.model.response.BaseResponse
import com.d104.pnt.data.remote.model.request.ChatCreateRequest
import com.d104.pnt.data.remote.model.response.ChatCreateResponse
import com.d104.pnt.data.remote.model.response.ChatRoomResponse
import com.d104.pnt.data.remote.model.response.JoinChatRoomResponse
import com.d104.pnt.data.remote.model.response.ChatSearchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApiService {
    /**
     * 채팅방 생성
     */
    @POST("chats")
    suspend fun createChatRoom(
        @Body request: ChatCreateRequest
    ): Response<BaseResponse<ChatCreateResponse>>

    /**
     * 채팅방 단건 조회
     */
    @GET("chats/{id}")
    suspend fun getChatRoom(
        @Path("id") chatRoomId: Long
    ): Response<BaseResponse<ChatRoomResponse>>

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

    @GET("chats")
    suspend fun getFilteredChatRoom(
        @Query("title") title: String?,
        @Query("regionCode") regionCode: Int?,
    ): Response<BaseResponse<ChatSearchResponse>>

    @GET("chats/me/rooms")
    suspend fun getJoinedChatRoom(): Response<BaseResponse<ChatSearchResponse>>
}
