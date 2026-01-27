package com.d104.pnt.domain.model

import androidx.room.Entity

data class ChatsData(
    val id: Int,
    val title: String,
    val description: String,
    val maxMember: Int,
    val currentMember: Int,
//    val createdAt: Instant
)
