package com.d104.pnt.domain.model

import com.google.gson.annotations.SerializedName

data class RoomInfoResponse(
    @SerializedName("room") val room: RoomData,
    @SerializedName("roomSetting") val roomSetting: RoomSetting,
    @SerializedName("members") val members: List<RoomMember>
)

data class RoomData(
    val id: Long,
    val hostMemberId: Long,
    val roomCode: String,
    val status: String,
    val startTime: String?,
    val endTime: String?,
    val winTeam: String?,
    val caughtCount: Int,
    val isDeleted: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class RoomSetting(
    val gameId: Long,
    val boundaryGeo: Any?, // 구체적인 구조에 따라 별도 클래스 생성 권장
    val prisonLat: Double,
    val prisonLng: Double,
    val timeLimit: Int,
    val policeCount: Int,
    val thiefCount: Int,
    val playerCount: Int
)

data class RoomMember(
    val id: Long,
    val memberId: Long,
    val serialCode: String,
    val givenPosition: String?,
    val ready: Boolean,
    val status: String?, // "null", "PRIZON", "FREE", "TRANSFER"
    val preferPosition: String,
    val inGameConnected: Boolean,
    @SerializedName("Member") val memberDetail: MemberDetail
)

data class MemberDetail(
    val id: Long,
    val loginId: String,
    val email: String,
    val birth: String,
    val role: String,
    @SerializedName("MemberProfile") val profile: MemberProfile
)

data class MemberProfile(
    val nickname: String,
    val avatarUrl: String?
)