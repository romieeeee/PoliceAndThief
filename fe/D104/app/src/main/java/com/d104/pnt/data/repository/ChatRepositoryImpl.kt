package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.api.ChatApiService
import com.d104.pnt.data.remote.model.response.ChatCreateRequest
import com.d104.pnt.data.remote.model.response.ChatCreateResponse
import com.d104.pnt.data.remote.model.response.ChatRoomResponse
import com.d104.pnt.data.remote.model.response.JoinChatRoomResponse
import com.d104.pnt.domain.model.common.BaseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatApiService: ChatApiService
) : ChatRepository, BaseRepository() {

    private val _currentChatRoomMember = MutableStateFlow<Int?>(null)

    override var currentChatRoom: Long? = null
    override var currentChatRoomTitle: String? = null
    override var currentChatRoomDescription: String? = null
    override var currentChatRoomRegionCode: Int? = null
    override var currentChatRoomMaxMember: Int? = null
    override val currentChatRoomMember = _currentChatRoomMember.asStateFlow()

    /**
     * 채팅방 단건 조회
     */
    override suspend fun getChatRoomInfo(chatRoomId: Long): BaseResult<ChatRoomResponse> {
        return safeApiCall(
            onSuccess = { chatRoomResponse ->
                // 정보를 성공적으로 가져왔을 때 Repository의 상태값들을 업데이트합니다.
                currentChatRoom = chatRoomResponse.chatRoomId
                currentChatRoomTitle = chatRoomResponse.title
                currentChatRoomDescription = chatRoomResponse.description
                currentChatRoomRegionCode = chatRoomResponse.regionCode
                currentChatRoomMaxMember = chatRoomResponse.maxMembers
                _currentChatRoomMember.value = chatRoomResponse.currentMembers
            }
        ) {
            chatApiService.getChatRoom(chatRoomId)
        }
    }

    /**
     * 채팅방 생성
     */
    override suspend fun createChatRoom(
        title: String,
        regionCode: Long,
        description: String,
        maxMembers: Int
    ): BaseResult<ChatCreateResponse> {
        return safeApiCall(
            onSuccess = { chatCreateResponse ->
                joinChatRoom(
                    chatRoomId = chatCreateResponse.chatRoomId
                )
            }
        ) {
            chatApiService.createChatRoom(
                ChatCreateRequest(
                    title,
                    description,
                    regionCode,
                    maxMembers
                )
            )
        }
    }

    /**
     * 채팅방 참여
     */
    override suspend fun joinChatRoom(chatRoomId: Long): BaseResult<JoinChatRoomResponse> {
        return safeApiCall(
            onSuccess = { joinResponse ->
                // 참여 성공 시 상태 저장
                currentChatRoom = joinResponse.chatRoomId
                _currentChatRoomMember.value = null // connect에서 업데이트될 예정
            }
        ) {
            chatApiService.joinChatRoom(chatRoomId)
        }
    }

    /**
     * 채팅방 연결
     */
    override suspend fun connectChatRoom(chatRoomId: Long): BaseResult<Unit> {
        return safeApiCall {
            chatApiService.connectChatRoom(chatRoomId)
        }
    }

    override suspend fun leaveChatRoom() {
        _currentChatRoomMember.value = null
        currentChatRoom = null
        currentChatRoomTitle = null
        currentChatRoomDescription = null
        currentChatRoomRegionCode = null
        currentChatRoomMaxMember = null
    }
}