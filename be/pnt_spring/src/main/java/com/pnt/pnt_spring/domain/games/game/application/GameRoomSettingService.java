package com.pnt.pnt_spring.domain.games.game.application;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomSettingUpdateRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomSettingGetResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomSettingUpdateResponse;

public interface GameRoomSettingService {

	GameRoomSettingGetResponse getSettings(Long actorMemberId, Long roomId);

	GameRoomSettingUpdateResponse updateSettings(Long actorMemberId, Long roomId, GameRoomSettingUpdateRequest req);
}
