package com.pnt.pnt_spring.domain.games.game.application.impl;

import com.pnt.pnt_spring.domain.games.game.api.resp.GameResultResponse;
import com.pnt.pnt_spring.domain.games.game.application.GameResultService; // 인터페이스 import
import com.pnt.pnt_spring.domain.games.game.entity.Game;
import com.pnt.pnt_spring.domain.games.game.entity.GameMember;
import com.pnt.pnt_spring.domain.games.game.entity.GameMemberStat;
import com.pnt.pnt_spring.domain.games.game.entity.*;
import com.pnt.pnt_spring.domain.games.game.repository.GameMemberRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameMemberStatRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameRepository;
import com.pnt.pnt_spring.domain.games.mission.repository.GameMissionRepository;
import com.pnt.pnt_spring.global.api.code.ErrorCode;
import com.pnt.pnt_spring.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GameResultServiceImpl implements GameResultService { // 클래스명 수정

    private final GameRepository gameRepository;
    private final GameMemberRepository gameMemberRepository;
    private final GameMemberStatRepository gameMemberStatRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final GameMissionRepository gameMissionRepository;

    @Override
    @Transactional
    public GameResultResponse processGameEnd(Long gameId, String winner) {
        // 게임 조회
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        // 이미 끝났다면
        if (game.getStatus() == GameStatus.FINISHED) {
            throw new BusinessException(ErrorCode.GAME_ALREADY_ENDED);
        }

        // 게임 종료 상태 저장 (winner: "POLICE" or "THIEF")
        game.finish(winner);

        // MVP 조회 및 선정
        GameMemberStat mvpStat = null;
        String mvpDescription = "";

        if ("POLICE".equals(winner)) { // 요청받은 winner 기준 (또는 game.getWinTeam())
            mvpStat = gameMemberStatRepository.findPoliceMvp(gameId).orElse(null);
            if (mvpStat != null) {
                mvpDescription = mvpStat.getArrestCount() + "회 체포 달성";
            }
        } else {
            mvpStat = gameMemberStatRepository.findThiefMvp(gameId).orElse(null);
            if (mvpStat != null) {
                mvpDescription = mvpStat.getLongestSurvived() + "초 생존";
            }
        }

        // 5. MVP 응답 객체 조립
        GameResultResponse.MvpResponse mvpResp = null;
        if (mvpStat != null) {
            var mvpMember = mvpStat.getGameMember().getMember(); // 변수명 충돌 방지를 위해 mvpMember로 변경
            var profile = mvpMember.getMemberProfile();

            mvpResp = GameResultResponse.MvpResponse.builder()
                    .memberId(mvpMember.getId())
                    .nickname(profile.getNickname())
                    .role(mvpStat.getGameMember().getGivenPosition().name())
                    .description(mvpDescription)
                    .build();
        }

        // 6. 전체 통계(TotalStats) 계산
        List<GameMember> participants = gameMemberRepository.findAllByGameId(gameId);
        int totalArrests = 0;
        // int totalMissions = 0; // 필요하다면 추가

        for (GameMember participant : participants) { // 변수명을 participant로 변경하여 충돌 해결
            // 각 참여자의 스탯 조회 (없으면 생성만 하고 값은 0)
            GameMemberStat stat = gameMemberStatRepository.findByGameMemberId(participant.getId())
                    .orElseGet(() -> gameMemberStatRepository.save(new GameMemberStat(participant)));

            // 경찰인 경우 체포 횟수 합산
            if (stat.getArrestCount() != null) {
                totalArrests += stat.getArrestCount();
            }
        }

        // 7. 최종 결과 반환
        return GameResultResponse.builder()
                .gameId(game.getId())
                .winner(game.getWinTeam())
                .mvp(mvpResp)
                .endedAt(game.getEndTime())
                .stats(GameResultResponse.TotalStats.builder()
                        .arrests(totalArrests)
                        .missionsCleared(0) // 미션 로직이 있다면 여기서 합산값 넣기
                        .durationSec((int) Duration.between(game.getStartTime(), game.getEndTime()).toSeconds())
                        .build())
                .build();
    }

    @Override
    public Long calculateSurvivalTime(Game game, GameMember member) {
        return 0L;
    }
}