package com.pnt.pnt_spring.domain.games.game.api.resp;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GameRoomSettingResponse {
	private Long roomId;
	private String status;

	private Integer timeLimit;
	private Integer playerCount;
	private Integer policeCount;
	private Integer thiefCount;
	private Integer cctvInterval;
	private Integer missionCount;
	private String roomCode;

	private Double prisonLat;
	private Double prisonLng;
}
