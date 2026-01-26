package com.pnt.pnt_spring.domain.games.game.repository;

import com.pnt.pnt_spring.domain.games.game.entity.GameMemberStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GameMemberStatRepository extends JpaRepository<GameMemberStat, Long> {
    Optional<GameMemberStat> findByGameMemberId(Long gameMemberId);
}