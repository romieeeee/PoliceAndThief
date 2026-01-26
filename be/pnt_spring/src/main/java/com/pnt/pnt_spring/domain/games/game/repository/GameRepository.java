package com.pnt.pnt_spring.domain.games.game.repository;

import com.pnt.pnt_spring.domain.games.game.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
}
