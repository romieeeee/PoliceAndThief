package com.d104.pnt.domain.model

import com.google.android.gms.maps.model.LatLng

data class CurrentGameRoomData(
    // Room ID
    val roomId: Long,
    val roomCode: String?,

    // Info
    val status: String?,
    val timeLimit: Int?,
//    val timeLeft: Int, // 남은시간...?
    val cctvFrequency: Int?,
    val missionCount: Int?,
    val playerCount: Int?,
    val maxPlayerCount: Int?,

    // Member
    val myMemberId: Long?,
    val roomManagerId: Long?,

    val policeCount: Int?,
    val thiefCount: Int?,
//    val survivedThiefCount: Int?,
    val missionCompleteCount: Int?,

    val preferPosition: String?,
//    val position: String?,
    val prison: LatLng?, // 저장할 때 LatLng 객체로 저장
    val polygon: List<LatLng?>,
    )
