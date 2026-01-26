package com.pnt.pnt_spring.domain.games.mission.api.req;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MissionRequest {

    private Long gameId;
    private Long missionId;
    private Long thiefId;

}
