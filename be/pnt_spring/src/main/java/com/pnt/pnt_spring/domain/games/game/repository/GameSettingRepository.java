package com.pnt.pnt_spring.domain.games.game.repository;

import com.pnt.pnt_spring.domain.games.game.entity.GameSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface GameSettingRepository extends JpaRepository<GameSetting, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from GameSetting s where s.game.id = :gameId")
    Optional<GameSetting> findByGameIdForUpdate(Long gameId);
}
