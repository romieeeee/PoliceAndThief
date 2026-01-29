package com.pnt.pnt_spring.domain.games.game.api.controller;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomSettingUpdateRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomSettingGetResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomSettingUpdateResponse;
import com.pnt.pnt_spring.domain.games.game.application.GameRoomSettingService;
import com.pnt.pnt_spring.global.api.response.CommonResponse;
import com.pnt.pnt_spring.global.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
@Tag(name = "Game Room Setting", description = "게임방 설정 조회/변경 API")
public class GameRoomSettingController {

    private final GameRoomSettingService gameRoomSettingService;

    @Operation(summary = "게임방 설정 조회")
    @GetMapping("/{roomId}/settings")
    public CommonResponse<GameRoomSettingGetResponse> getSettings(@PathVariable Long roomId) {

        Long actorMemberId = SecurityUtils.currentMemberId();

        GameRoomSettingGetResponse data =
                gameRoomSettingService.getSettings(actorMemberId, roomId);

        return new CommonResponse<>(
                data,
                "게임방 설정 조회 성공",
                HttpStatus.OK
        );
    }

    @Operation(summary = "게임방 설정 변경")
    @PatchMapping("/{roomId}/settings")
    public CommonResponse<GameRoomSettingUpdateResponse> updateSettings(
            @PathVariable Long roomId,
            @Valid @RequestBody GameRoomSettingUpdateRequest req
    ) {
        Long actorMemberId = SecurityUtils.currentMemberId();
        GameRoomSettingUpdateResponse data =
                gameRoomSettingService.updateSettings(actorMemberId, roomId, req);

        return new CommonResponse<>(
                data,
                "게임방 설정 변경 성공",
                HttpStatus.OK
        );
    }
}
