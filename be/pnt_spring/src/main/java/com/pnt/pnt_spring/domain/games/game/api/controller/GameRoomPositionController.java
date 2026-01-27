package com.pnt.pnt_spring.domain.games.game.api.controller;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomPositionRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomPositionResponse;
import com.pnt.pnt_spring.domain.games.game.application.GameRoomService;
import com.pnt.pnt_spring.global.api.response.CommonResponse;
import com.pnt.pnt_spring.global.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
public class GameRoomPositionController {

    private final GameRoomService gameRoomService;

    @PostMapping("/{roomId}/position")
    public CommonResponse<GameRoomPositionResponse> pickPosition(
            @PathVariable Long roomId,
            @Valid @RequestBody GameRoomPositionRequest req
    ) {
        Long memberId = SecurityUtils.currentMemberId();

        GameRoomPositionResponse data = gameRoomService.pickPosition(memberId, roomId, req);

        return new CommonResponse<>(
                data,
                "플레이 역할 선택 성공",
                HttpStatus.OK
        );
    }
}
