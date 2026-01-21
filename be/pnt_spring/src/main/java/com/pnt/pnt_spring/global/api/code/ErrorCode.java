package com.pnt.pnt_spring.global.api.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    /*
     * ===== Code Rule
     * 1xxx: AUTH
     * 2xxx: MEMBER
     * 3xxx: ROOM / MATCHING
     * 4xxx: GAME
     * 5xxx: CHAT
     * 9xxx: COMMON / SERVER
     */

    // =========================
    // COMMON / SERVER (9xxx)
    // =========================
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, 9000),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, 9001),
    PARAM_BIND_ERROR(HttpStatus.BAD_REQUEST, 9002),

    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, 9050),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 9999),

    // =========================
    // AUTH (1xxx)
    // =========================
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 1101),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, 1104),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, 1105),

    FORBIDDEN(HttpStatus.FORBIDDEN, 1103),

    // =========================
    // MEMBER (2xxx)
    // =========================
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, 2001),
    DUPLICATE_USER_ID(HttpStatus.CONFLICT, 2002),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, 2003),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, 2004),

    // 프로필/상태
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, 2010),

    // =========================
    // ROOM / MATCHING (3xxx)
    // =========================
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, 3001),
    ROOM_FULL(HttpStatus.CONFLICT, 3401),
    ROOM_ALREADY_JOINED(HttpStatus.CONFLICT, 3402),
    ROOM_NOT_JOINED(HttpStatus.CONFLICT, 3403),

    ROOM_NOT_HOST(HttpStatus.FORBIDDEN, 3301),
    ROOM_ALREADY_STARTED(HttpStatus.CONFLICT, 3404),

    // Ready/Start 흐름에서 자주 씀
    ROOM_NOT_READY(HttpStatus.CONFLICT, 3405),
    ROOM_ALREADY_READY(HttpStatus.CONFLICT, 3406),

    // =========================
    // GAME (4xxx)
    // =========================
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, 4001),
    GAME_ALREADY_STARTED(HttpStatus.CONFLICT, 4401),
    GAME_ALREADY_ENDED(HttpStatus.CONFLICT, 4402),

    // 경찰/도둑 역할 기반 상태 충돌
    INVALID_ROLE_ACTION(HttpStatus.CONFLICT, 4403),     // 역할에 맞지 않는 행동
    INVALID_GAME_STATE(HttpStatus.CONFLICT, 4404),      // 현재 게임 상태에서 불가능한 요청
    TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, 4405),       // 체포 대상 등 타겟 없음(상황에 따라 404/409 조정)

    // =========================
    // CHAT (5xxx)
    // =========================
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, 5001),
    CHAT_FORBIDDEN(HttpStatus.FORBIDDEN, 5301),
    MESSAGE_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5501);

    private final HttpStatus statusCode;
    private final int customCode;

    ErrorCode(HttpStatus statusCode, int customCode) {
        this.statusCode = statusCode;
        this.customCode = customCode;
    }
}
