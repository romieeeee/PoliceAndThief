package com.pnt.pnt_spring.domain.games.game.entity;

import com.pnt.pnt_spring.domain.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "game_setting")
public class GameSetting extends BaseEntity {

    @Id
    private Long gameId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game;

    private Integer timeLimitSec;
    private Integer policeCount;
    private Integer thiefCount;

    private Geometry boundaryGeo;
    private Point prisonLocation;

    /* =========================
       생성/변경 메서드
       ========================= */

    public static GameSetting createDefault(Game game) {
        GameSetting s = new GameSetting();
        s.game = game;
        // 기본값 원하는 대로
        s.timeLimitSec = 300;
        s.policeCount = 1;
        s.thiefCount = 3;
        return s;
    }

    public void updateCounts(Integer policeCount, Integer thiefCount) {
        if (policeCount != null) this.policeCount = policeCount;
        if (thiefCount != null) this.thiefCount = thiefCount;
    }

    public void updateTimeLimit(Integer timeLimitSec) {
        if (timeLimitSec != null) this.timeLimitSec = timeLimitSec;
    }

    public void updateMap(Geometry boundaryGeo, Point prisonLocation) {
        if (boundaryGeo != null) this.boundaryGeo = boundaryGeo;
        if (prisonLocation != null) this.prisonLocation = prisonLocation;
    }
}
