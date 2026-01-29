package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.model.request.ChatCreateRequest
import com.d104.pnt.data.remote.model.response.ChatCreateResponse
import com.d104.pnt.data.remote.model.response.ChatSearchResponse
import com.d104.pnt.domain.model.common.BaseResult
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    // 나중에 이거 지우고 구현부에서 private로 만들기 기왕이면 데이터 클래스 하나로 관리가능 하면 더 좋고
    var currentChatRoom: Long?
    var currentChatRoomTitle: String?
    var currentChatRoomDescription: String?
    var currentChatRoomRegionCode: Int?
    var currentChatRoomMaxMember: Int?
    val currentChatRoomMember: StateFlow<Int?>

    suspend fun createChatRoom(
        title: String,
        regionCode: Int,
        description: String,
        maxMembers: Int
    ): BaseResult<ChatCreateResponse>

    suspend fun searchChatRoom(
        title: String?,
        regionCode: Int?
    ): BaseResult<ChatSearchResponse>

    suspend fun joinChatRoom(
        chatRoomId: Long,
        memberId: Long,
        title: String,
        description: String,
        regionCode: Int,
        maxMembers: Int
    )

    suspend fun leaveChatRoom()
}