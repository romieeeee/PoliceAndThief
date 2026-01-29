package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.model.response.ChatCreateResponse
import com.d104.pnt.data.remote.model.response.JoinChatRoomResponse
import com.d104.pnt.domain.model.common.BaseResult
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    var currentChatRoom: Long?
    var currentChatRoomTitle: String?
    var currentChatRoomDescription: String?
    var currentChatRoomRegionCode: Long?
    var currentChatRoomMaxMember: Int?
    val currentChatRoomMember: StateFlow<Int?>

    suspend fun createChatRoom(
        title: String,
        regionCode: Long,
        description: String,
        maxMembers: Int
    ): BaseResult<ChatCreateResponse>

    suspend fun joinChatRoom(chatRoomId: Long): BaseResult<JoinChatRoomResponse>

    suspend fun connectChatRoom(chatRoomId: Long): BaseResult<Unit>

    suspend fun leaveChatRoom()
}