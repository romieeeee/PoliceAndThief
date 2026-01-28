package com.pnt.pnt_spring.domain.games.game.application;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomCreateRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomCreateResponse;

public interface GameRoomService {
    GameRoomCreateResponse createRoom(Long hostMemberId, GameRoomCreateRequest req);
}
