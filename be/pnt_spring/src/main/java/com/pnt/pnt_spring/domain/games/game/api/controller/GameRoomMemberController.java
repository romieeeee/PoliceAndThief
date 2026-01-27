package com.pnt.pnt_spring.domain.games.game.api.controller;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomPositionRequest;
import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomReadyRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomMemberListResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomPositionResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomReadyResponse;
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
public class GameRoomMemberController {

    private final GameRoomService gameRoomService;

    /**
     * 게임방 멤버 목록 조회
     */
    @GetMapping("/{roomId}/members")
    public CommonResponse<GameRoomMemberListResponse> getMembers(@PathVariable Long roomId) {
        GameRoomMemberListResponse data = gameRoomService.getRoomMembers(roomId);

        return new CommonResponse<>(
                data,
                "게임방 멤버 목록 조회 성공",
                HttpStatus.OK
        );
    }

    /**
     * 준비 상태 변경
     */
    @PatchMapping("/{roomId}/ready")
    public CommonResponse<GameRoomReadyResponse> ready(
            @PathVariable Long roomId,
            @Valid @RequestBody GameRoomReadyRequest req
    ) {
        Long memberId = SecurityUtils.currentMemberId();
        GameRoomReadyResponse data = gameRoomService.updateReady(roomId, memberId, req);

        return new CommonResponse<>(
                data,
                "게임 준비 상태 변경 성공",
                HttpStatus.OK
        );
    }

    /**
     * 플레이 역할(포지션) 선택
     */
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
