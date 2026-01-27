package com.pnt.pnt_spring.domain.games.game.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameStatus {

    RUNNING("GAME_RUNNING", "게임 진행 중"),
    FINISHED("GAME_FINISHED", "게임 종료");

    private final String key;
    private final String title;

}
