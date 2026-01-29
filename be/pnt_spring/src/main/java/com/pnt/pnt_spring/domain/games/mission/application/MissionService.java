package com.pnt.pnt_spring.domain.games.mission.application;

import com.pnt.pnt_spring.domain.games.mission.api.resp.MissionResponse;

import java.util.List;

public interface MissionService {

    List<MissionResponse> getAllMissions();

    List<MissionResponse> getMissions(Long gameId);

    MissionResponse getMissionDetail(Long gameId, Long missionId);

    Boolean submitMission(Long gameId, Long missionId, Long thiefId);

}
