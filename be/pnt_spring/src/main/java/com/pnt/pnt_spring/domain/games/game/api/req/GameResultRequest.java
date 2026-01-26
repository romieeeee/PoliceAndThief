package com.pnt.pnt_spring.domain.games.game.api.req;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GameResultRequest {
    private Long gameId;
    private String winner; // "POLICE" | "THIEF"
}