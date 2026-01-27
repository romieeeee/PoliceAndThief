package com.pnt.pnt_spring.domain.games.game.api.controller;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomSettingUpdateRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomSettingUpdateResponse;
import com.pnt.pnt_spring.domain.games.game.application.GameRoomService;
import com.pnt.pnt_spring.global.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
public class GameRoomSettingController {

    private final GameRoomService gameRoomService;

    @PatchMapping("/{roomId}/settings")
    public GameRoomSettingUpdateResponse updateSettings(
            @PathVariable Long roomId,
            @Valid @RequestBody GameRoomSettingUpdateRequest req
    ) {
        Long actorMemberId = SecurityUtils.currentMemberId();
        return gameRoomService.updateSettings(actorMemberId, roomId, req);
    }
}
