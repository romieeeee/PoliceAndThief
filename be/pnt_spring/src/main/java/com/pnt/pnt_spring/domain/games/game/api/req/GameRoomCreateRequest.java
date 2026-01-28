package com.pnt.pnt_spring.domain.games.game.api.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class GameRoomCreateRequest {

    @NotNull @Min(1)
    private Integer playerCount;

    @NotNull @Min(1)
    private Integer timeLimit;

    @NotNull @Min(0)
    private Integer policeCount;

    @NotNull @Min(0)
    private Integer thiefCount;

    @NotNull
    @Valid
    private Prison prison;

    @NotNull
    @Size(min = 3, message = "polygon은 최소 3개 좌표가 필요합니다.")
    @Valid
    private List<LatLng> polygon;

    @Getter
    public static class Prison {
        @NotNull
        private Double lat;

        @NotNull
        private Double lng;
    }

    @Getter
    public static class LatLng {
        @NotNull
        private Double lat;

        @NotNull
        private Double lng;
    }
}
