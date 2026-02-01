package com.d104.pnt.navigation

import com.d104.pnt.domain.model.GameRole


/**
 * 앱 전체 라우트 정의
 * 파라미터가 필요한 경우 buildXxx 함수 제공
 */
object Routes {
    // ===== 로그인 전 화면 =====
    const val INTRO = "intro"
    const val LOGIN = "login"
    const val SIGNUP = "signup"

    // ===== BottomNav 탭 =====
    const val HOME = "home"
    const val CHAT = "chat"
    const val PROFILE = "profile"

    // ===== 채팅 =====
    const val CHAT_CREATE = "chat_create"                // 채팅방 생성

    const val CHAT_ROOM = "chat_room"                    // 채팅방
    fun buildChatRoom(chatId: Long) = "$CHAT_ROOM/$chatId"

    // ===== 게임 생성 =====
    const val GAME_CREATE = "game_create"

    const val ROLE_SELECT = "role_select"
    fun buildRoleSelect(roomId: Long) = "$ROLE_SELECT/$roomId"

    // ===== 게임 대기방 =====
    const val GAME_ROOM = "game_room"
    fun buildGameRoom(roomId: Long, role: String) = "$GAME_ROOM/$roomId/$role"

    const val GAME_ROOM_SETTINGS = "game_room_settings"  // 게임 설정
    fun buildGameRoomSettings(roomId: Long) = "$GAME_ROOM_SETTINGS/$roomId"

    // ===== 게임 플로우 =====
    const val GAME_ROLE = "game_role"                  // 역할 안내
    fun buildGameIntro(roomId: Long, role: String) = "$GAME_ROLE/$roomId/$role"

    const val GAME_LOADING = "game_loading"              // 카운트다운
    // 로딩 화면에서도 방 번호를 유지해야 한다면 수정
    fun buildGameLoading(roomId: Long, role: String) = "$GAME_LOADING/$roomId/$role"

    const val GAME_PLAY = "game_play"                    // 게임 플레이
    fun buildGamePlay(gameId: Long, role: String): String {
        return "$GAME_PLAY/$gameId/$role"
    }

    const val MISSION_CAMERA = "mission_camera"

    const val GAME_RESULT = "game_result"                // 게임 결과
    fun buildGameResult(gameId: Long) = "$GAME_RESULT/$gameId"

    const val GAME_NEWS = "game_news"                    // 결과 뉴스
    fun buildGameNews(newsId: Long) = "$GAME_NEWS/$newsId"

    // ===== 프로필 =====
    const val PROFILE_DETAIL = "profile_detail"          // 다른 유저 프로필
    fun buildProfileDetail(memberId: String) = "$PROFILE_DETAIL/$memberId"

    const val PROFILE_EDIT = "profile_edit"              // 내 프로필 수정

    // ===== 기타 =====
    const val REPORT = "report"                          // 신고하기
    fun buildReport(nickname: String) = "$REPORT/$nickname"

    // 게임 플레이 경로 생성
    fun buildGamePlay(gameId: Long, role: GameRole): String {
        return "$GAME_PLAY/$gameId/${role.roleNameEn}"
    }
}