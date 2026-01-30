package com.pnt.pnt_spring.domain.games.mission.application;

import com.pnt.pnt_spring.domain.games.mission.api.resp.MissionResponse;

import java.util.List;

public interface MissionService {

    List<MissionResponse> getAllMissions();

    MissionResponse getMission(Long missionId);

    List<MissionResponse> getGameAllMissions(Long gameId);

    MissionResponse getGameMission(Long gameId, Long missionId);


}
