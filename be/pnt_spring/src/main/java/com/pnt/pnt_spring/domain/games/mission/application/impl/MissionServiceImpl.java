package com.pnt.pnt_spring.domain.games.mission.application.impl;

import com.pnt.pnt_spring.domain.games.mission.api.resp.MissionResponse;
import com.pnt.pnt_spring.domain.games.mission.application.MissionService;
import com.pnt.pnt_spring.domain.games.mission.entity.GameMission;
import com.pnt.pnt_spring.domain.games.mission.entity.Mission;
import com.pnt.pnt_spring.domain.games.mission.repository.GameMissionRepository;
import com.pnt.pnt_spring.domain.games.mission.repository.MissionRepository;
import com.pnt.pnt_spring.global.api.code.ErrorCode;
import com.pnt.pnt_spring.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissionServiceImpl implements MissionService {

    private final GameMissionRepository gameMissionRepository;
    private final MissionRepository missionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MissionResponse> getAllMissions() {
        return missionRepository.findAll().stream()
                .map(mission -> MissionResponse.builder()
                        .missionId(mission.getId())
                        .title(mission.getTitle())
                        .description(mission.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissionResponse> getMissions(Long gameId) {
        // 미션 목록 조회
        List<GameMission> gameMissions = gameMissionRepository.findByGameId(gameId);

        return gameMissions.stream()
                .map(gm -> MissionResponse.builder()
                        .missionId(gm.getMission().getId()) // 미션 원본 ID
                        .title(gm.getMission().getTitle())
                        .description(gm.getMission().getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MissionResponse getMissionDetail(Long gameId, Long missionId) {
        // 미션 ID로 단건 조회
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_NOT_FOUND));

        return MissionResponse.builder()
                .missionId(mission.getId())
                .title(mission.getTitle())
                .description(mission.getDescription())
                .build();
    }

    // 미션 제출
    @Override
    public Boolean submitMission(Long gameId, Long missionId, Long thiefId) {
        GameMission gameMission = gameMissionRepository.findByGameIdAndMissionId(gameId, missionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_NOT_FOUND));

        return true;
    }
}