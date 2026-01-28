package com.pnt.pnt_spring.domain.games.game.api.resp;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GameRoomMemberListResponse {
    private Long roomId;
    private List<GameRoomMemberItem> items;
}
