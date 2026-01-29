package com.pnt.pnt_spring.domain.games.game.api.controller;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomCreateRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomCreateResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomStartableResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameStartResponse;
import com.pnt.pnt_spring.domain.games.game.application.GameRoomService;
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
@Tag(name = "Game Room", description = "게임방 생성/시작 관련 API")
public class GameRoomController {

    private final GameRoomService gameRoomService;

    /**
     * 게임방 생성
     */
    @Operation(summary = "게임방 생성")
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

    @Operation(summary = "게임 시작")
    @PostMapping("/{roomId}/start")
    public CommonResponse<GameStartResponse> start(@PathVariable Long roomId) {
        Long actorId = SecurityUtils.currentMemberId();
        GameStartResponse data = gameRoomService.start(actorId, roomId);

        return new CommonResponse<>(
                data,
                "게임 시작 성공",
                HttpStatus.OK
        );
    }

    @Operation(summary = "게임 시작 가능 여부 조회")
    @GetMapping("/{roomId}/startable")
    public CommonResponse<GameRoomStartableResponse> startable(@PathVariable Long roomId) {

        Long actorId = SecurityUtils.currentMemberId();
        GameRoomStartableResponse data =
                gameRoomService.getStartable(actorId, roomId);

        return new CommonResponse<>(
                data,
                "게임 시작 가능 여부 조회 성공",
                HttpStatus.OK
        );
    }
}
