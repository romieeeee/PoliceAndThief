package com.pnt.pnt_spring.domain.games.game.api.controller;

import com.pnt.pnt_spring.domain.chats.utils.SecurityUtils;
import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomCreateRequest;
import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomReadyRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomCreateResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomMemberListResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomReadyResponse;
import com.pnt.pnt_spring.domain.games.game.application.GameRoomService;
import com.pnt.pnt_spring.global.api.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
public class GameRoomController {

    private final GameRoomService gameRoomService;

    @PostMapping
    public CommonResponse<GameRoomCreateResponse> createRoom(
            @RequestBody(required = false) GameRoomCreateRequest req
    ) {
        Long hostMemberId = SecurityUtils.currentMemberId();
        GameRoomCreateResponse data = gameRoomService.createRoom(hostMemberId, req);

        return new CommonResponse<>(
                data,
                "게임방 생성 성공",
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}/members")
    public CommonResponse<GameRoomMemberListResponse> getMembers(@PathVariable Long id) {
        GameRoomMemberListResponse data = gameRoomService.getRoomMembers(id);

        return new CommonResponse<>(
                data,
                "게임방 멤버 목록 조회 성공",
                HttpStatus.OK
        );
    }

    @PatchMapping("/{id}/ready")
    public CommonResponse<GameRoomReadyResponse> ready(
            @PathVariable Long id,
            @RequestBody GameRoomReadyRequest req
    ) {
        Long memberId = SecurityUtils.currentMemberId();

        GameRoomReadyResponse data = gameRoomService.updateReady(id, memberId, req);

        return new CommonResponse<>(
                data,
                "게임 준비 상태 변경 성공",
                HttpStatus.OK
        );
    }

}
