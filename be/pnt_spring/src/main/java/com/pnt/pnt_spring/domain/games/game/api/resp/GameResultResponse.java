package com.pnt.pnt_spring.domain.games.game.api.resp;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameResultResponse {
	private Long gameId;
	private Long resultId;
	private String winner; // "POLICE" | "THIEF"
	private OffsetDateTime endedAt;
	private TotalStats stats; // 총 게임 스탯(모든 유저들이 쌓은 스탯)
	private MvpResponse mvp;          // MVP (이긴 팀 1등)
	private MvpResponse winningSecond; // 이긴 팀 2등
	private MvpResponse losingFirst;   // 진 팀 1등

<<<<<<<HEAD

	@Getter
	@Builder
	public static class TotalStats {
		private int arrests;         // 총 체포 횟수
		private int escapes;         // 총 탈출 횟수 (도둑 승리 시)
		private int missionsCleared; // 완료된 총 미션 수
		private int durationSec;     // 실제 게임 진행 시간(초)s
	}
=======

	@Getter
	@Builder
	public static class TotalStats {
		private int arrests;         // 총 체포 횟수
		private int missionsCleared; // 완료된 총 미션 수
		private int durationSec;     // 실제 게임 진행 시간(초)s
	}
>>>>>>>backend

	@Getter
	@Builder

	public static class MvpResponse {
		private Long memberId;
		private String nickname;
		private String role;
		private String description;
	}
}