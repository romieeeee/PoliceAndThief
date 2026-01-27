package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.model.response.ChatCreateRequest
import com.d104.pnt.data.remote.model.response.ChatCreateResponse
import com.d104.pnt.domain.model.common.BaseResult
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    var currentChatRoom: Int?
    var currentChatRoomTitle: String?
    var currentChatRoomDescription: String?
    var currentChatRoomRegionCode: Int?
    var currentChatRoomMaxMember: Int?
    val currentChatRoomMember: StateFlow<Int?>

    suspend fun createChatRoom(
        title: String,
        regionCode: Int,
        description: String,
        maxMember: Int
    ): BaseResult<ChatCreateResponse>

    suspend fun joinChatRoom(
        chatRoomId: Int,
        memberId: Int,
        title: String,
        description: String,
        regionCode: Int,
        maxMember: Int
    )

    suspend fun leaveChatRoom()
}