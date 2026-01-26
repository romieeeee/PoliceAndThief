package com.pnt.pnt_spring.domain.games.mission.repository;

import com.pnt.pnt_spring.domain.games.mission.entity.GameMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameMissionRepository extends JpaRepository<GameMission, Long> {

    @Query("SELECT gm FROM GameMission gm JOIN FETCH gm.mission WHERE gm.game.id = :gameId")
    List<GameMission> findByGameId(@Param("gameId") Long gameId);

    Optional<GameMission> findByGameIdAndMissionId(Long gameId, Long missionId);
}