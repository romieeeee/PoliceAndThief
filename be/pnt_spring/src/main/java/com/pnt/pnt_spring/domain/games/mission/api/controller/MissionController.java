package com.pnt.pnt_spring.domain.games.mission.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pnt.pnt_spring.domain.games.mission.api.resp.MissionResponse;
import com.pnt.pnt_spring.domain.games.mission.application.MissionService;
import com.pnt.pnt_spring.global.api.response.CommonResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Game Mission", description = "게임 미션 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/games")
public class MissionController {

	private final MissionService missionService;

	@Operation(summary = "전체 미션 목록 조회 (원본)", description = "게임 할당 여부와 관계없이 시스템에 등록된 모든 원본 미션 목록을 조회합니다.")
	@GetMapping("/missions")
	public CommonResponse<List<MissionResponse>> getAllMissions() {
		List<MissionResponse> response = missionService.getAllMissions();
		return new CommonResponse<>(response, "전체 미션 목록 조회 성공", HttpStatus.OK);
	}

	@Operation(summary = "미션 상세 조회", description = "미션 상세 정보를 조회합니다.")
	@GetMapping("/missions/{missionId}")
	public CommonResponse<MissionResponse> getMission(
		@PathVariable Long missionId) {

		MissionResponse response = missionService.getMission(missionId);

		return new CommonResponse<>(response, "미션 상세 조회 성공", HttpStatus.OK);
	}

	@Operation(summary = "인게임 미션 목록 조회", description = "도둑들이 수행할 전체 미션 목록을 조회합니다.")
	@GetMapping("/{gameId}/missions")
	public CommonResponse<List<MissionResponse>> getMissions(@PathVariable Long gameId) {

		List<MissionResponse> response = missionService.getGameAllMissions(gameId);

		return new CommonResponse<>(response, "미션 목록 조회 성공", HttpStatus.OK);
	}

	@Operation(summary = "인게임 미션 상세 조회", description = "도둑들이 수행할 미션 상세 정보를 조회합니다.")
	@GetMapping("/{gameId}/missions/{missionId}")
	public CommonResponse<MissionResponse> getMissionDetail(
		@PathVariable Long gameId,
		@PathVariable Long missionId) {

		MissionResponse response = missionService.getGameMission(gameId, missionId);

		return new CommonResponse<>(response, "미션 상세 조회 성공", HttpStatus.OK);
	}

}