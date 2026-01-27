package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.api.ChatApiService
import com.d104.pnt.data.remote.model.response.ChatCreateRequest
import com.d104.pnt.data.remote.model.response.ChatCreateResponse
import com.d104.pnt.domain.model.common.BaseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor (
    private val chatApiService: ChatApiService
): ChatRepository, BaseRepository() {

    private val _currentChatRoomMember = MutableStateFlow<Int?>(null)

    override var currentChatRoom: Int? = null
    override var currentChatRoomTitle: String? = null
    override var currentChatRoomDescription: String? = null
    override var currentChatRoomRegionCode: Int? = null
    override var currentChatRoomMaxMember: Int? = null
    override val currentChatRoomMember = _currentChatRoomMember.asStateFlow()

    override suspend fun createChatRoom(
        title: String,
        regionCode: Int,
        description: String,
        maxMember: Int
    ): BaseResult<ChatCreateResponse> {
        return safeApiCall (
            onSuccess = { chatCreateResponse ->
                joinChatRoom(
                    chatRoomId = chatCreateResponse.chatRoomId,
                    memberId = chatCreateResponse.ownerId,
                    title = chatCreateResponse.title,
                    description = chatCreateResponse.description,
                    regionCode = chatCreateResponse.regionCode,
                    maxMember = chatCreateResponse.maxMember
                )
            }
        ) {
            chatApiService.createChatRoom(ChatCreateRequest(title, description, regionCode, maxMember))
        }
    }
//    override suspend fun login(id: String, password: String): BaseResult<LoginResponse> {
//        return safeApiCall(
//            onSuccess = { loginResponse ->
//                saveLoginData(
//                    accessToken = loginResponse.accessToken,
//                    refreshToken = loginResponse.refreshToken,
//                    userId = loginResponse.member.id
//                )
//            }
//        ) {
//            apiService.login(LoginRequest(id, password))
//        }
//    }
    override suspend fun joinChatRoom(
        chatRoomId: Int,
        memberId: Int,
        title: String,
        description: String,
        regionCode: Int,
        maxMember: Int
    ) {

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