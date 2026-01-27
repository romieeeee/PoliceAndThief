package com.pnt.pnt_spring.domain.games.game.application;

import com.pnt.pnt_spring.domain.games.game.api.req.*;
import com.pnt.pnt_spring.domain.games.game.api.resp.*;

public interface GameRoomService {

    GameRoomCreateResponse createRoom(Long hostMemberId, GameRoomCreateRequest req);

    GameRoomMemberListResponse getRoomMembers(Long roomId);

    GameRoomReadyResponse updateReady(Long roomId, Long memberId, GameRoomReadyRequest req);

    GameRoomSettingUpdateResponse updateSettings(Long actorMemberId, Long roomId, GameRoomSettingUpdateRequest req);

    GameRoomPositionResponse pickPosition(Long actorMemberId, Long roomId, GameRoomPositionRequest req);

    GameRoomJoinResponse joinRoom(Long memberId, GameRoomJoinRequest req);

    // void startGame(Long roomId, Long hostMemberId);
}
