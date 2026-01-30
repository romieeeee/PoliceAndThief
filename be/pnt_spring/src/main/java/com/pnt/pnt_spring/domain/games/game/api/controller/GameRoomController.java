package com.pnt.pnt_spring.domain.games.game.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomCreateRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomCreateResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomStartableResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameStartResponse;
import com.pnt.pnt_spring.domain.games.game.application.GameRoomService;
import com.pnt.pnt_spring.global.api.response.CommonResponse;
import com.pnt.pnt_spring.global.utils.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
public class GameRoomController {

	private final GameRoomService gameRoomService;

	/**
	 * 게임방 생성
	 */
	@PostMapping
	public CommonResponse<GameRoomCreateResponse> createRoom(
		@Valid @RequestBody GameRoomCreateRequest req
	) {
		Long hostMemberId = SecurityUtils.currentMemberId();
		GameRoomCreateResponse data = gameRoomService.createRoom(hostMemberId, req);

		return new CommonResponse<>(
			data,
			"게임방 생성 성공",
			HttpStatus.CREATED
		);
	}

	@PostMapping("/{roomId}/start")
	public CommonResponse<GameStartResponse> start(@PathVariable Long roomId) {
		Long actorId = SecurityUtils.currentMemberId();
		GameStartResponse data = gameRoomService.start(actorId, roomId);

		return new CommonResponse<>(
			data,
			"게임 시작 성공",
			HttpStatus.OK
		);
	}

	@GetMapping("/{roomId}/startable")
	public CommonResponse<GameRoomStartableResponse> startable(@PathVariable Long roomId) {

		Long actorId = SecurityUtils.currentMemberId();
		GameRoomStartableResponse data =
			gameRoomService.getStartable(actorId, roomId);

		return new CommonResponse<>(
			data,
			"게임 시작 가능 여부 조회 성공",
			HttpStatus.OK
		);
	}

}
