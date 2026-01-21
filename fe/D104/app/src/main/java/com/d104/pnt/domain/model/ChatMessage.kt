package com.d104.pnt.domain.model

import java.time.Instant

data class ChatMessage (
    val id: Int,
    val chatRoomId: Int,
    val memberId: Int,
    val senderNickname: String,
    val avataUrl: String,
    val content: String,
//    val createdAt: Instant,
) {

}