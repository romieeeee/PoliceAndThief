package com.pnt.pnt_spring.domain.games.game.application;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomJoinRequest;
import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomPositionRequest;
import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomReadyRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomJoinResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomMemberListResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomPositionResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomReadyResponse;

public interface GameRoomMemberService {

    GameRoomJoinResponse joinRoom(Long memberId, GameRoomJoinRequest req);

    void leave(Long memberId, Long roomId);

    GameRoomMemberListResponse getRoomMembers(Long roomId);

    GameRoomReadyResponse updateReady(Long roomId, Long memberId, GameRoomReadyRequest req);

    GameRoomPositionResponse pickPosition(Long actorMemberId, Long roomId, GameRoomPositionRequest req);

    void kick(Long actorId, Long roomId, Long targetMemberId, String reason);
}
