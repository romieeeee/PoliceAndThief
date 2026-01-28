package com.pnt.pnt_spring.domain.games.game.api.resp;

import com.pnt.pnt_spring.domain.games.game.entity.Game;
import com.pnt.pnt_spring.domain.games.game.entity.GameMember;
import com.pnt.pnt_spring.domain.games.game.enums.GameStatus;
import com.pnt.pnt_spring.domain.games.game.enums.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStartResponse {

    private Long roomId;           // == gameId
    private GameStatus status;     // IN_GAME
    private OffsetDateTime startTime;

    /** 시작과 동시에 확정된 역할(포지션) */
    private List<MemberPosition> members;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberPosition {
        private Long memberId;
        private String nickname;      // 없으면 null 가능
        private Position givenPosition;
    }

    public static GameStartResponse from(Game game, List<GameMember> members) {
        return GameStartResponse.builder()
                .roomId(game.getId())
                .status(game.getStatus())
                .startTime(game.getStartTime())
                .members(
                        members.stream()
                                .map(gm -> MemberPosition.builder()
                                        .memberId(gm.getMember().getId())
                                        .nickname(resolveNickname(gm))
                                        .givenPosition(gm.getGivenPosition())
                                        .build()
                                )
                                .toList()
                )
                .build();
    }

    private static String resolveNickname(GameMember gm) {
        var m = gm.getMember();
        if (m == null) return null;
        var profile = m.getMemberProfile();
        if (profile != null && profile.getNickname() != null && !profile.getNickname().isBlank()) {
            return profile.getNickname();
        }
        return m.getLoginId(); // 없으면 loginId fallback
    }
}
