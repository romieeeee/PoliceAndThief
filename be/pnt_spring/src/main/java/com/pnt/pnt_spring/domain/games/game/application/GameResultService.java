package com.pnt.pnt_spring.domain.games.game.application;

import com.pnt.pnt_spring.domain.games.game.api.req.GameResultRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameResultResponse;
import com.pnt.pnt_spring.domain.games.game.entity.Game;
import com.pnt.pnt_spring.domain.games.game.entity.GameMember;

public interface GameResultService {

    void saveGameResult(GameResultRequest request);

    GameResultResponse getGameResult(Long gameId);

    Long calculateSurvivalTime(Game game, GameMember member);

}
