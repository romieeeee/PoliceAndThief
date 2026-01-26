package com.d104.pnt.domain.model

data class PlayerLocation(
    val id: Int,
    val member_id: Int,
    val latitude: Double,
    val longitude: Double,
    val status: Int,
    val position: Int,
)