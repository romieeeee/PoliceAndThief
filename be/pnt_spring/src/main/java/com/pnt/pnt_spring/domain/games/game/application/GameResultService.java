package com.pnt.pnt_spring.domain.games.game.application;

import com.pnt.pnt_spring.domain.games.game.api.req.GameResultRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameResultResponse;
import com.pnt.pnt_spring.domain.games.game.entity.Game;
import com.pnt.pnt_spring.domain.games.game.entity.GameMember;

public interface GameResultService {

    // 게임 결과 저장
    void saveGameResult(GameResultRequest request);

    // 게임 결과 조회
    GameResultResponse getGameResult(Long gameId);

    // 생존 시간 계산 (필요한 경우 유지)
    Long calculateSurvivalTime(Game game, GameMember member);

}
