package com.pnt.pnt_spring.domain.games.game.repository;

import com.pnt.pnt_spring.domain.games.game.entity.GameSkill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSkillRepository extends JpaRepository<GameSkill, Long> {

    boolean existsByGame_IdAndMember_Id(Long gameId, Long memberId);
}
