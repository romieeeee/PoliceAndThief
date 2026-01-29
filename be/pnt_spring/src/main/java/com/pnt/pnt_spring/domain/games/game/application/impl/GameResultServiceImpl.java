package com.pnt.pnt_spring.domain.games.game.application.impl;

import com.pnt.pnt_spring.domain.games.game.api.req.GameResultRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameResultResponse;
import com.pnt.pnt_spring.domain.games.game.application.GameResultService;
import com.pnt.pnt_spring.domain.games.game.entity.Game;
import com.pnt.pnt_spring.domain.games.game.entity.GameMember;
import com.pnt.pnt_spring.domain.games.game.entity.GameMemberStat;
import com.pnt.pnt_spring.domain.games.game.enums.GameStatus;
import com.pnt.pnt_spring.domain.games.game.enums.Position;
import com.pnt.pnt_spring.domain.games.game.enums.WinTeam;
import com.pnt.pnt_spring.domain.games.game.repository.GameMemberRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameMemberStatRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameRepository;
import com.pnt.pnt_spring.domain.games.news.api.req.AiNewsRequest;
import com.pnt.pnt_spring.global.api.code.ErrorCode;
import com.pnt.pnt_spring.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    @Override
    public void saveGameResult(GameResultRequest request) {
        log.info("게임 결과 저장 시작: GameId={}", request.getGameId());

        Game game = gameRepository.findById(request.getGameId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        if (game.getStatus() == GameStatus.ENDED) {
            throw new BusinessException(ErrorCode.GAME_ALREADY_ENDED);
        }

        game.end(request.getWinTeam());

        for (GameResultRequest.MemberStat statReq : request.getMemberStats()) {
            GameMemberStat stat = gameMemberStatRepository.findByGameMemberId(statReq.getGameMemberId())
                    .orElseGet(() -> {
                        GameMember gm = gameMemberRepository.findById(statReq.getGameMemberId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
                        return gameMemberStatRepository.save(GameMemberStat.createInitialStat(gm));
                    });

            stat.updateResultStats(statReq.getWalk(), statReq.getLongestSurvived());
        }

        // AI 뉴스 생성 요청
        triggerAiNewsGeneration(game);
    }

    @Override
    @Transactional(readOnly = true)
    public GameResultResponse getGameResult(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        // MVP 선정
        GameMemberStat mvpStat = calculateMvp(game);
        String mvpNickname = (mvpStat != null)
                ? mvpStat.getGameMember().getMember().getMemberProfile().getNickname()
                : "없음";

        // 전체 통계 집계 (N+1 방지를 위해 findAllByGameId 사용하는 것을 추천하지만 기존 로직 유지 시)
        List<GameMember> participants = gameMemberRepository.findAllByGameId(gameId);
        int totalArrests = 0;

        for (GameMember p : participants) {
            GameMemberStat stat = gameMemberStatRepository.findByGameMemberId(p.getId()).orElse(null);
            if (stat != null && stat.getArrestCount() != null) {
                totalArrests += stat.getArrestCount();
            }
        }

        int durationSec = (int) Duration.between(game.getStartTime(), game.getEndTime()).toSeconds();

        return GameResultResponse.builder()
                .gameId(game.getId())
                .winner(game.getWinTeam().toString())
                .endedAt(game.getEndTime())
                .mvp(mvpStat != null ? GameResultResponse.MvpResponse.builder()
                        .memberId(mvpStat.getGameMember().getMember().getId())
                        .nickname(mvpNickname)
                        .role(mvpStat.getGameMember().getGivenPosition().name())
                        .build() : null)
                .stats(GameResultResponse.TotalStats.builder()
                        .arrests(totalArrests)
                        .durationSec(durationSec)
                        .missionsCleared(0)
                        .build())
                .build();
    }

    private void triggerAiNewsGeneration(Game game) {
        // 1. 해당 게임의 모든 멤버 스탯 조회
        List<GameMemberStat> allStats = gameMemberStatRepository.findAllByGameId(game.getId());

        // 2. 팀별 분류
        List<GameMemberStat> policeStats = new ArrayList<>();
        List<GameMemberStat> thiefStats = new ArrayList<>();

        for (GameMemberStat stat : allStats) {
            if (stat.getPosition() == Position.POLICE) {
                policeStats.add(stat);
            } else if (stat.getPosition() == Position.THIEF) {
                thiefStats.add(stat);
            }
        }

        // 3. 정렬 (경찰: 체포수 내림차순, 도둑: 생존시간 내림차순)
        policeStats.sort((a, b) -> compareStats(b.getArrestCount(), a.getArrestCount()));
        thiefStats.sort((a, b) -> compareStats(b.getLongestSurvived(), a.getLongestSurvived()));

        // 4. 승리/패배 팀 데이터 추출
        String mvpNickname = "없음";
        String winnerTopMember = "없음";
        String loserTopMember = "없음";

        List<GameMemberStat> winnerStats;
        List<GameMemberStat> loserStats;

        // [중요] WinTeam Enum 비교 (== 사용)
        boolean isPoliceWin = (game.getWinTeam() == WinTeam.POLICE);

        if (isPoliceWin) {
            winnerStats = policeStats;
            loserStats = thiefStats;
        } else {
            winnerStats = thiefStats;
            loserStats = policeStats;
        }

        // MVP: 승리팀 1등
        if (!winnerStats.isEmpty()) {
            mvpNickname = getNickname(winnerStats.get(0));
        }
        // Winner Top Member: 승리팀 2등
        if (winnerStats.size() > 1) {
            winnerTopMember = getNickname(winnerStats.get(1));
        }
        // Loser Top Member: 패배팀 1등
        if (!loserStats.isEmpty()) {
            loserTopMember = getNickname(loserStats.get(0));
        }

        int durationSec = (int) Duration.between(game.getStartTime(), game.getEndTime()).toSeconds();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        AiNewsRequest aiRequest = AiNewsRequest.builder()
                .gameId(game.getId())
                .startTime(game.getStartTime().format(formatter))
                .winningTeam(isPoliceWin ? "경찰" : "도둑")
                .playTime(durationSec)
                .location("구미시 진평동")
                .policeCount(policeStats.size())
                .thiefCount(thiefStats.size())
                .mvp(mvpNickname)
                .winnerTopMember(winnerTopMember)
                .loserTopMember(loserTopMember)
                .build();

        rabbitTemplate.convertAndSend("NEWS", aiRequest);
        log.info("MQ Message Published to 'NEWS': gameId={}", game.getId());
    }

    private GameMemberStat calculateMvp(Game game) {
        // 0페이지에서 1개만 가져옴 (Top 1 효과)
        PageRequest limitOne = PageRequest.of(0, 1);

        List<GameMemberStat> stats;

        // WinTeam 비교 (Enum == 사용)
        if (game.getWinTeam() == WinTeam.POLICE) {
            stats = gameMemberStatRepository.findPoliceMvp(game.getId(), limitOne);
        } else {
            stats = gameMemberStatRepository.findThiefMvp(game.getId(), limitOne);
        }

        // 리스트가 비어있지 않으면 첫 번째 요소 반환, 없으면 null
        return stats.isEmpty() ? null : stats.get(0);
    }

    @Override
    public Long calculateSurvivalTime(Game game, GameMember member) {
        return 0L;
    }

    // 닉네임 추출 헬퍼
    private String getNickname(GameMemberStat stat) {
        return stat.getGameMember().getMember().getMemberProfile().getNickname();
    }

    // null-safe Integer 비교 헬퍼
    private int compareStats(Integer v1, Integer v2) {
        int val1 = (v1 == null) ? 0 : v1;
        int val2 = (v2 == null) ? 0 : v2;
        return Integer.compare(val1, val2);
    }
}