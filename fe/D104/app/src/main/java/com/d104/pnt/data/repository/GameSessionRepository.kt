package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.model.response.BeepUseResponse
import com.d104.pnt.data.remote.model.response.GameMemberSocketDto
import com.d104.pnt.data.remote.model.response.MemberLocationSocketDto
import com.d104.pnt.data.remote.model.response.MissionSocketDto
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface GameSessionRepository {
    val members: StateFlow<List<GameMemberSocketDto>>
    val gameStatus: StateFlow<String>
    val gameId: StateFlow<Long>
    val gameTime: StateFlow<Int>
    val missions: StateFlow<List<MissionSocketDto>>
    val memberLocation: StateFlow<List<MemberLocationSocketDto>>
    val eventFlow: SharedFlow<GameSessionEvent>
    val isOutOfBoundary: StateFlow<Boolean>
    val myMemberId: StateFlow<Long>
    val myRole: StateFlow<String>
    val thiefMembers: StateFlow<List<GameMemberSocketDto>>
    val escapeQueue: StateFlow<List<String>>
    val beepEvent: SharedFlow<BeepUseResponse>
    val longestSurvivalTime: StateFlow<Int>
    val survivalTime: StateFlow<Int>
    val missionState: StateFlow<MissionStatus>
    val missionFailReason: StateFlow<String>

    fun setMemberId(memberId: Long)
    fun setFinalRole(role: String)
    fun connectAndJoin(gameId: Long)
    fun gameInit()
    fun uploadMissionImage(image: File, missionId: Long)
    fun missionInit()
    fun dequeEscape()
    fun startGameSession()
    fun stopGameSession()
    fun leaveGame()
}