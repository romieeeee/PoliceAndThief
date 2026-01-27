package com.pnt.pnt_spring.domain.games.game.api.controller;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomCreateRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomCreateResponse;
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
public class GameRoomController {

    private final GameRoomService gameRoomService;

    /**
     * 게임방 생성
     */
    @PostMapping
    public CommonResponse<GameRoomCreateResponse> createRoom(
            @Valid @RequestBody GameRoomCreateRequest req
    ) {
        Long hostMemberId = SecurityUtils.currentMemberId();
        GameRoomCreateResponse data = gameRoomService.createRoom(hostMemberId, req);

        return new CommonResponse<>(
                data,
                "게임방 생성 성공",
                HttpStatus.CREATED
        );
    }
}
