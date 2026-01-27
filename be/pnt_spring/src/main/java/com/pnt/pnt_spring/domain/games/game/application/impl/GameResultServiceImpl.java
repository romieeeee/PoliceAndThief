package com.pnt.pnt_spring.domain.games.game.application.impl;

import com.pnt.pnt_spring.domain.games.game.api.req.GameResultRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameResultResponse;
import com.pnt.pnt_spring.domain.games.game.application.GameResultService;
import com.pnt.pnt_spring.domain.games.game.entity.*;
import com.pnt.pnt_spring.domain.games.game.repository.GameMemberRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameMemberStatRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameRepository;
import com.pnt.pnt_spring.domain.games.news.api.req.AiNewsRequest;
import com.pnt.pnt_spring.global.api.code.ErrorCode;
import com.pnt.pnt_spring.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GameResultServiceImpl implements GameResultService {

    private final GameRepository gameRepository;
    private final GameMemberRepository gameMemberRepository;
    private final GameMemberStatRepository gameMemberStatRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 1. 게임 결과 저장 및 AI 뉴스 요청 (Command)
     */
    @Override
    public void saveGameResult(GameResultRequest request) {
        log.info("게임 결과 저장 시작: GameId={}", request.getGameId());

        Game game = gameRepository.findById(request.getGameId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        if (game.getStatus() == GameStatus.FINISHED) {
            log.warn("이미 종료된 게임입니다.");
            return;
        }

        // 1-1. 게임 상태 업데이트 (종료 시간, 승리 팀)
        game.finish(request.getWinTeam().name());

        // 1-2. 멤버별 통계 저장 (walk, survived 등)
        for (GameResultRequest.MemberStat statReq : request.getMemberStats()) {
            GameMemberStat stat = gameMemberStatRepository.findByGameMemberId(statReq.getGameMemberId())
                    .orElseGet(() -> {
                        // 없으면 생성 (방어 코드)
                        GameMember gm = gameMemberRepository.findById(statReq.getGameMemberId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
                        return gameMemberStatRepository.save(GameMemberStat.createInitialStat(gm));
                    });

            // 기존 arrestCount는 유지하고, 새로 들어온 데이터만 업데이트
            stat.updateResultStats(statReq.getWalk(), statReq.getLongestSurvived());
        }

        // 1-3. AI 뉴스 생성 요청 (저장 시점에 바로 트리거)
        triggerAiNewsGeneration(game);
    }

    /**
     * 2. 게임 결과 조회 및 MVP 산정 (Query)
     */
    @Override
    @Transactional(readOnly = true)
    public GameResultResponse getGameResult(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        // 2-1. MVP 선정 (조회 시점에 계산)
        GameMemberStat mvpStat = calculateMvp(game);
        String mvpNickname = (mvpStat != null)
                ? mvpStat.getGameMember().getMember().getMemberProfile().getNickname()
                : "없음";

        // 2-2. 통계 집계
        List<GameMember> participants = gameMemberRepository.findAllByGameId(gameId);
        int totalArrests = 0;

        for (GameMember p : participants) {
            GameMemberStat stat = gameMemberStatRepository.findByGameMemberId(p.getId()).orElse(null);
            if (stat != null && stat.getArrestCount() != null) {
                totalArrests += stat.getArrestCount();
            }
        }

        int durationSec = (int) Duration.between(game.getStartTime(), game.getEndTime()).toSeconds();

        // 2-3. 응답 반환
        return GameResultResponse.builder()
                .gameId(game.getId())
                .winner(game.getWinTeam())
                .endedAt(game.getEndTime())
                .mvp(mvpStat != null ? GameResultResponse.MvpResponse.builder()
                        .memberId(mvpStat.getGameMember().getMember().getId())
                        .nickname(mvpNickname)
                        .role(mvpStat.getGameMember().getGivenPosition().name())
                        .build() : null)
                .stats(GameResultResponse.TotalStats.builder()
                        .arrests(totalArrests)
                        .durationSec(durationSec)
                        .missionsCleared(0) // 미션 로직 연결 시 수정
                        .build())
                .build();
    }

    private void triggerAiNewsGeneration(Game game) {
        // MVP 및 통계 계산 (뉴스 생성을 위해 임시 계산)
        GameMemberStat mvpStat = calculateMvp(game);
        String mvpNickname = (mvpStat != null)
                ? mvpStat.getGameMember().getMember().getMemberProfile().getNickname()
                : "없음";

        List<GameMember> members = gameMemberRepository.findAllByGameId(game.getId());
        int policeCount = (int) members.stream().filter(m -> m.getGivenPosition() == GameMemberPosition.POLICE).count();
        int thiefCount = (int) members.stream().filter(m -> m.getGivenPosition() == GameMemberPosition.THIEF).count();
        int durationSec = (int) Duration.between(game.getStartTime(), game.getEndTime()).toSeconds();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        AiNewsRequest aiRequest = AiNewsRequest.builder()
                .gameId(game.getId())
                .startTime(game.getStartTime().format(formatter))
                .winningTeam("POLICE".equals(game.getWinTeam()) ? "경찰" : "도둑")
                .playTime(durationSec)
                .location("SSAFY 구미캠퍼스 운동장")
                .policeCount(policeCount)
                .thiefCount(thiefCount)
                .mvp(mvpNickname)
                .winnerTopMember(mvpNickname)
                .loserTopMember("도망왕") // 필요시 별도 로직 구현
                .build();

        rabbitTemplate.convertAndSend("game.news.exchange", "game.news.request", aiRequest);
    }

    private GameMemberStat calculateMvp(Game game) {
        // 승리 팀에 따라 MVP 선정 쿼리 호출
        return "POLICE".equals(game.getWinTeam())
                ? gameMemberStatRepository.findPoliceMvp(game.getId()).orElse(null)
                : gameMemberStatRepository.findThiefMvp(game.getId()).orElse(null);
    }

    @Override
    public Long calculateSurvivalTime(Game game, GameMember member) {
        return 0L;
    }
}