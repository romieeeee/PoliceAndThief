package com.pnt.pnt_spring.domain.games.mission.application.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pnt.pnt_spring.domain.games.mission.api.resp.MissionResponse;
import com.pnt.pnt_spring.domain.games.mission.application.MissionService;
import com.pnt.pnt_spring.domain.games.mission.entity.GameMission;
import com.pnt.pnt_spring.domain.games.mission.entity.Mission;
import com.pnt.pnt_spring.domain.games.mission.repository.GameMissionRepository;
import com.pnt.pnt_spring.domain.games.mission.repository.MissionRepository;
import com.pnt.pnt_spring.global.api.code.ErrorCode;
import com.pnt.pnt_spring.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MissionServiceImpl implements MissionService {

	private final GameMissionRepository gameMissionRepository;
	private final MissionRepository missionRepository;

	// 모든 미션 항목 조회
	@Override
	@Transactional(readOnly = true)
	public List<MissionResponse> getAllMissions() {
		return missionRepository.findAll().stream()
			.map(mission -> MissionResponse.builder()
				.missionId(mission.getId())
				.title(mission.getTitle())
				.description(mission.getDescription())
				.keyword(mission.getKeyword())
				.build())
			.collect(Collectors.toList());
	}

	// 미션 단건 조회
	@Override
	@Transactional(readOnly = true)
	public MissionResponse getMission(Long missionId) {
		Mission mission = missionRepository.findById(missionId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MISSION_NOT_FOUND));

		return MissionResponse.builder()
			.missionId(mission.getId())
			.title(mission.getTitle())
			.description(mission.getDescription())
			.keyword(mission.getKeyword())
			.build();
	}

	// 인 게임 내 할당된 미션 조회
	@Override
	@Transactional(readOnly = true)
	public List<MissionResponse> getGameAllMissions(Long gameId) {
		// 미션 목록 조회
		List<GameMission> gameMissions = gameMissionRepository.findByGameId(gameId);

		return gameMissions.stream()
			.map(gm -> MissionResponse.builder()
				.missionId(gm.getMission().getId()) // 미션 원본 ID
				.title(gm.getMission().getTitle())
				.description(gm.getMission().getDescription())
				.keyword(gm.getMission().getKeyword())
				.build())
			.collect(Collectors.toList());
	}

	// 게임 내 미션 세부항목
	@Override
	@Transactional(readOnly = true)
	public MissionResponse getGameMission(Long gameId, Long missionId) {
		// 미션 ID로 단건 조회
		Mission mission = missionRepository.findById(missionId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MISSION_NOT_FOUND));

		return MissionResponse.builder()
			.missionId(mission.getId())
			.title(mission.getTitle())
			.description(mission.getDescription())
			.keyword(mission.getKeyword())
			.build();
	}
}