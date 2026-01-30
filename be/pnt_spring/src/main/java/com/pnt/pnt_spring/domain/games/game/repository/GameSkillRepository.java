package com.pnt.pnt_spring.domain.games.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pnt.pnt_spring.domain.games.game.entity.GameSkill;

public interface GameSkillRepository extends JpaRepository<GameSkill, Long> {

	boolean existsByGame_IdAndMember_Id(Long gameId, Long memberId);
}
