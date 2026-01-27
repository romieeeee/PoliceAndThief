package com.pnt.pnt_spring.domain.games.game.application;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomCreateRequest;
import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomPositionRequest;
import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomReadyRequest;
import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomSettingUpdateRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.*;

public interface GameRoomService {

    GameRoomCreateResponse createRoom(Long hostMemberId, GameRoomCreateRequest req);

    GameRoomMemberListResponse getRoomMembers(Long roomId);

    GameRoomReadyResponse updateReady(Long roomId, Long memberId, GameRoomReadyRequest req);

    GameRoomSettingUpdateResponse updateSettings(Long actorMemberId, Long roomId, GameRoomSettingUpdateRequest req);

    GameRoomPositionResponse pickPosition(Long actorMemberId, Long roomId, GameRoomPositionRequest req);

    // void startGame(Long roomId, Long hostMemberId);
}
