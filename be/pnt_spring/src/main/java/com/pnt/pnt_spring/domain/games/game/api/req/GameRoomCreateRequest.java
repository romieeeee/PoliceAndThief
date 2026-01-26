package com.pnt.pnt_spring.domain.games.game.api.req;

import lombok.Getter;

@Getter
public class GameRoomCreateRequest {
    private Integer timeLimitSec;
    private Integer policeCount;
    private Integer thiefCount;

    // 경계/감옥 좌표까지 받으려면 다음 필드도 추가
    // private String boundaryGeoJson;
    // private Double prisonLat;
    // private Double prisonLng;
}
