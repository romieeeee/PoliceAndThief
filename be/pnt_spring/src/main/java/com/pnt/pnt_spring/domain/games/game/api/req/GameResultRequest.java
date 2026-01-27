package com.pnt.pnt_spring.domain.games.game.api.req;

import com.pnt.pnt_spring.domain.games.game.entity.GameMemberPosition;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@ToString
public class GameResultRequest {
    private Long gameId;
    private GameMemberPosition winTeam; // "POLICE" or "THIEF"
    private List<MemberStat> memberStats;

    @Getter
    @NoArgsConstructor
    @ToString
    public static class MemberStat {
        private Long gameMemberId;
        private GameMemberPosition position;
        private Integer walk;
        private Integer longestSurvived;
    }
}