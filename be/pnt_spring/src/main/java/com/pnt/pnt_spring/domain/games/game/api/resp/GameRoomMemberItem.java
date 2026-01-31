package com.pnt.pnt_spring.domain.games.game.api.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class GameRoomMemberItem {
	private Long memberId;
	private Long gameMemberId;
	private String nickname;
	private String role;
	private boolean isHost;
	private boolean isReady;
	private String avatarUrl;
}
