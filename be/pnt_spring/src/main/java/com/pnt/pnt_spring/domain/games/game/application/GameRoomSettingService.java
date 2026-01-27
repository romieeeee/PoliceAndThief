package com.pnt.pnt_spring.domain.games.game.application;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomSettingUpdateRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomSettingUpdateResponse;

public interface GameRoomSettingService {
    GameRoomSettingUpdateResponse updateSettings(Long actorMemberId, Long roomId, GameRoomSettingUpdateRequest req);
}
