package com.d104.pnt.domain.model

import java.time.Instant

data class RoomData(
    val id: Int,
    val title: String,
    val description: String,
    val maxMember: Int,
    val currentMember: Int,
//    val createdAt: Instant
)
