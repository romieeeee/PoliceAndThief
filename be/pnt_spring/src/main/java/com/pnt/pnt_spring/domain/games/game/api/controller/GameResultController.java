package com.pnt.pnt_spring.domain.games.game.api.controller;

import com.pnt.pnt_spring.domain.games.game.api.req.GameResultRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameResultResponse;
import com.pnt.pnt_spring.domain.games.game.application.impl.GameResultServiceImpl;
import com.pnt.pnt_spring.global.api.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Game Result", description = "게임 종료 및 결과 관리 API")
@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameResultController {

    private final GameResultServiceImpl gameResultService;

    @Operation(summary = "게임 결과 저장", description = "게임 종료 시 승패 및 통계를 저장합니다.")
    @PostMapping("/result")
    public CommonResponse<GameResultResponse> saveGameResult(@RequestBody GameResultRequest request) {
        // Node.js 서버에서 게임 종료 조건 달성 시 호출
        GameResultResponse response = gameResultService.processGameEnd(request.getGameId(), request.getWinner());
        return new CommonResponse<>(response, "게임 결과 저장 완료", HttpStatus.OK);
    }

}
