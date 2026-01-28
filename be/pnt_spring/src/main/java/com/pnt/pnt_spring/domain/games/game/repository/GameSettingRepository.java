package com.pnt.pnt_spring.domain.games.game.repository;

import com.pnt.pnt_spring.domain.games.game.entity.GameSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GameSettingRepository extends JpaRepository<GameSetting, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select gs
        from GameSetting gs
        where gs.gameId = :gameId
          and gs.isDeleted = false
    """)
    Optional<GameSetting> findByGameIdForUpdate(@Param("gameId") Long gameId);

    Optional<GameSetting> findByGameIdAndIsDeletedFalse(Long gameId);
}
