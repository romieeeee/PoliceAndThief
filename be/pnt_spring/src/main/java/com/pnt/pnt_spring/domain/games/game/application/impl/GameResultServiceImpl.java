package com.pnt.pnt_spring.domain.games.game.application.impl;

import com.pnt.pnt_spring.domain.games.game.api.resp.GameResultResponse;
import com.pnt.pnt_spring.domain.games.game.entity.*;
import com.pnt.pnt_spring.domain.games.game.repository.GameMemberRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameMemberStatRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameRepository;
import com.pnt.pnt_spring.domain.games.mission.repository.GameMissionRepository;
import com.pnt.pnt_spring.global.api.code.ErrorCode;
import com.pnt.pnt_spring.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GameResultServiceImpl {

    private final GameRepository gameRepository;
    private final GameMemberRepository gameMemberRepository;
    private final GameMemberStatRepository gameMemberStatRepository;
    private final GameMissionRepository gameMissionRepository;

    public GameResultResponse processGameEnd(Long gameId, String winner) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        // 이미 끝났다면
        if (game.getStatus() == GameStatus.FINISHED) {
            throw new BusinessException(ErrorCode.GAME_ALREADY_ENDED);
        }

        // 게임 종료 상태 저장 (winner: "POLICE" or "THIEF")
        game.finish(winner);

        // 참여자들의 인게임 통계 확정 (이미 Node/App에서 업데이트된 데이터를 조회하거나, 최종 확인만 함)
        List<GameMember> participants = gameMemberRepository.findAllByGameId(gameId);
        int totalArrests = 0;

        for (GameMember member : participants) {
            // GameMemberStat은 이미 인게임 중에 Node.js에 의해 수치가 업데이트되어 있을 것입니다.
            // 여기서는 최종 결과 응답을 위해 합계만 구합니다.
            GameMemberStat stat = gameMemberStatRepository.findByGameMemberId(member.getId())
                    .orElseGet(() -> gameMemberStatRepository.save(new GameMemberStat(member)));

            if (member.getGivenPosition() == GameMemberPosition.POLICE) {
                totalArrests += (stat.getArrestCount() != null) ? stat.getArrestCount() : 0;
            }
        }

        // 결과 응답 생성 (클라이언트에 종료를 알림)
        return GameResultResponse.builder()
                .gameId(game.getId())
                .winner(game.getWinTeam())
                .endedAt(game.getEndTime())
                .stats(GameResultResponse.TotalStats.builder()
                        .arrests(totalArrests)
                        .durationSec((int) Duration.between(game.getStartTime(), game.getEndTime()).toSeconds())
                        .build())
                .build();
    }
}