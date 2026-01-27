package com.d104.pnt.base

object Constants {

    // API
    const val BASE_URL = "http://i14d104.p.ssafy.io/spring/"
    const val WEBSOCKET_URL = "wss://your-api-server.com/ws"

    const val CONNECT_TIMEOUT = 30L // seconds
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    // DataStore
    const val PREF_NAME = "cop_and_thief_prefs"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_IS_LOGGED_IN = "is_logged_in"

    // Location
    const val LOCATION_UPDATE_INTERVAL = 1000L // 1초
    const val LOCATION_FASTEST_INTERVAL = 500L
    const val MIN_DISTANCE_FOR_UPDATE = 1f // meters

    // Game
    const val MAX_PLAYERS = 30
    const val CAPTURE_DISTANCE_THRESHOLD = 3.0 // meters (체포 거리)
    const val PRISON_RADIUS = 5.0 // meters (감옥 반경)
    const val BOUNDARY_WARNING_TIME = 30 // seconds (이탈 경고 시간)

    // WebSocket Events
    const val WS_EVENT_LOCATION_UPDATE = "location_update"
    const val WS_EVENT_CAPTURE = "capture"
    const val WS_EVENT_MISSION_COMPLETE = "mission_complete"
    const val WS_EVENT_GAME_START = "game_start"
    const val WS_EVENT_GAME_END = "game_end"
    const val WS_EVENT_BOUNDARY_WARNING = "boundary_warning"
    const val WS_EVENT_PLAYER_JOIN = "player_join"
    const val WS_EVENT_PLAYER_LEAVE = "player_leave"

    // Roles
    const val ROLE_POLICE = "POLICE"
    const val ROLE_THIEF = "THIEF"

    // Player Status
    const val STATUS_ACTIVE = "ACTIVE"
    const val STATUS_ARRESTED = "ARRESTED"
    const val STATUS_IN_PRISON = "IN_PRISON"
    const val STATUS_OUT_OF_BOUND = "OUT_OF_BOUND"
    const val STATUS_ESCAPED = "ESCAPED"

    // Mission
    const val MISSION_TYPE_PHOTO = "PHOTO"
    const val MISSION_CONFIDENCE_THRESHOLD = 0.7f // AI 인식 임계값

    // Notification IDs
    const val NOTIFICATION_ID_LOCATION_SERVICE = 1001
    const val NOTIFICATION_ID_GAME_ALERT = 1002
    const val NOTIFICATION_ID_BOUNDARY_WARNING = 1003

    // Camera
    const val IMAGE_MAX_SIZE = 1920 // pixels
    const val IMAGE_QUALITY = 85 // JPEG quality (0-100)

    // Database
    const val DATABASE_NAME = "cop_and_thief_db"
    const val DATABASE_VERSION = 1

    // Error Messages
    const val ERROR_NETWORK = "네트워크 연결을 확인해주세요"
    const val ERROR_GPS = "GPS를 활성화해주세요"
    const val ERROR_PERMISSION = "필요한 권한이 없습니다"
    const val ERROR_GAME_FULL = "게임 방이 가득 찼습니다"
    const val ERROR_SESSION_EXPIRED = "세션이 만료되었습니다. 다시 로그인해주세요"
}