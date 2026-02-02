package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.model.response.GameMemberSocketDto
import com.d104.pnt.data.remote.model.response.MissionSocketDto
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface GameSessionRepository {
    val members: StateFlow<List<GameMemberSocketDto>>
    val gameStatus: StateFlow<String>
    val gameId: StateFlow<Long>
    val missions: StateFlow<List<MissionSocketDto>>
    val eventFlow: SharedFlow<GameSessionEvent>
    val isOutOfBoundary: StateFlow<Boolean>
    val myMemberId: StateFlow<Long>
    val myRole: StateFlow<String>

    val chiefMemberId: StateFlow<Long?>


    fun setMemberId(memberId: Long)
    fun setFinalRole(role: String)
    fun connectAndJoin(gameId: Long)
    fun gameInit()
    fun startGameSession()
    fun stopGameSession()
    fun leaveGame()

    fun setChiefMemberId(id: Long?)
}