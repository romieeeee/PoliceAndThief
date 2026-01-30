package com.pnt.pnt_spring.domain.games.mission.application;

import java.util.List;

import com.pnt.pnt_spring.domain.games.mission.api.resp.MissionResponse;

public interface MissionService {

	List<MissionResponse> getMissions(Long gameId);

	MissionResponse getMissionDetail(Long gameId, Long missionId);

	Boolean submitMission(Long gameId, Long missionId, Long thiefId);

}
