package com.pnt.pnt_spring.domain.games.game.repository;

import com.pnt.pnt_spring.domain.games.game.entity.GameSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSettingRepository extends JpaRepository<GameSetting, Long> {
    // gameId == PK
}
