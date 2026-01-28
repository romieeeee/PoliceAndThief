package com.pnt.pnt_spring.domain.games.game.api.resp;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GameRoomMemberItem {
    private Long memberId;
    private String nickname;
    private String role;              // POLICE / THIEF / UNDECIDED
    private boolean isHost;
    private boolean isReady;
    private String profileImageUrl;   // avatarUrl
}
