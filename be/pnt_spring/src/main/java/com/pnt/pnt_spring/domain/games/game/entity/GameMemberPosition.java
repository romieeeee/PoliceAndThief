package com.pnt.pnt_spring.domain.games.game.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor // 생성자 누락으로 인한 빌드 오류 해결
public enum GameMemberPosition {
    POLICE("POSITION_POLICE", "경찰"),
    THIEF("POSITION_THIEF", "도둑");

    private final String key;
    private final String value;
}