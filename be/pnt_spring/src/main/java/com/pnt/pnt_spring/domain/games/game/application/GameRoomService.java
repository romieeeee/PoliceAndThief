package com.pnt.pnt_spring.domain.games.game.application;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomCreateRequest;
import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomReadyRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomCreateResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomMemberListResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomReadyResponse;

public interface GameRoomService {

    GameRoomCreateResponse createRoom(Long hostMemberId, GameRoomCreateRequest req);

    GameRoomMemberListResponse getRoomMembers(Long roomId);

    GameRoomReadyResponse updateReady(Long roomId, Long memberId, GameRoomReadyRequest req);

    // void startGame(Long roomId, Long hostMemberId);
}
