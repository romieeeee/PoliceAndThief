package com.pnt.pnt_spring.domain.games.game.persistence.entity;

import com.pnt.pnt_spring.domain.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;

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
}