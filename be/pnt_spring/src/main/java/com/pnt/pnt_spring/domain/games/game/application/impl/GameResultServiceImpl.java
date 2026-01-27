package com.pnt.pnt_spring.domain.games.game.application.impl;

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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GameResultServiceImpl implements GameResultService {

    private final GameRepository gameRepository;
    private final GameMemberRepository gameMemberRepository;
    private final GameMemberStatRepository gameMemberStatRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public GameResultResponse processGameEnd(Long gameId, String winner) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        if (game.getStatus() == GameStatus.FINISHED) {
            throw new BusinessException(ErrorCode.GAME_ALREADY_ENDED);
        }

        // 게임 종료 상태 반영
        game.finish(winner);

        // 결과 데이터 생성
        GameResultResponse response = assembleGameResult(game);

        // AI 뉴스 생성을 위한 비동기 메시지 전송 --> to MQ
        sendAiNewsRequest(game, response);

        return response;
    }

    /**
     * 게임 결과를 바탕으로 응답 DTO를 조립하는 함수
     */
    private GameResultResponse assembleGameResult(Game game) {
        Long gameId = game.getId();
        String winner = game.getWinTeam();

        // MVP 선정
        GameMemberStat mvpStat = "POLICE".equals(winner)
                ? gameMemberStatRepository.findPoliceMvp(gameId).orElse(null)
                : gameMemberStatRepository.findThiefMvp(gameId).orElse(null);

        // 통계 집계 (Arrests)
        List<GameMember> participants = gameMemberRepository.findAllByGameId(gameId);
        int totalArrests = participants.stream()
                .map(p -> gameMemberStatRepository.findByGameMemberId(p.getId()).orElse(null))
                .filter(s -> s != null && s.getArrestCount() != null)
                .mapToInt(GameMemberStat::getArrestCount)
                .sum();

        int durationSec = (int) Duration.between(game.getStartTime(), game.getEndTime()).toSeconds();

        return GameResultResponse.builder()
                .gameId(gameId)
                .winner(winner)
                .endedAt(game.getEndTime())
                .mvp(mvpStat != null ? GameResultResponse.MvpResponse.builder()
                        .memberId(mvpStat.getGameMember().getMember().getId())
                        .nickname(mvpStat.getGameMember().getMember().getMemberProfile().getNickname())
                        .role(mvpStat.getGameMember().getGivenPosition().name())
                        .build() : null)
                .stats(GameResultResponse.TotalStats.builder()
                        .arrests(totalArrests)
                        .durationSec(durationSec)
                        .missionsCleared(0)
                        .build())
                .build();
    }

    /**
     * MQ 전송 로직
     */
    private void sendAiNewsRequest(Game game, GameResultResponse response) {
        List<GameMember> members = gameMemberRepository.findAllByGameId(game.getId());
        int policeCount = (int) members.stream().filter(m -> m.getGivenPosition() == GameMemberPosition.POLICE).count();
        int thiefCount = (int) members.stream().filter(m -> m.getGivenPosition() == GameMemberPosition.THIEF).count();

        String mvpName = response.getMvp() != null ? response.getMvp().getNickname() : "없음";

        AiNewsRequest aiRequest = AiNewsRequest.builder()
                .start_time(game.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .winning_team("POLICE".equals(game.getWinTeam()) ? "경찰" : "도둑")
                .play_time(response.getStats().getDurationSec())
                .location("SSAFY 구미캠퍼스")
                .police_count(policeCount)
                .thief_count(thiefCount)
                .mvp(mvpName)
                .winner_top_member(mvpName)
                .loser_top_member("상대팀 플레이어")
                .build();

        // config에서 설정한 exchange와 routingKey 이름을 사용하세요.
        rabbitTemplate.convertAndSend("game.news.exchange", "game.news.request", aiRequest);
    }

    @Override
    public Long calculateSurvivalTime(Game game, GameMember member) {
        return 0L;
    }
}