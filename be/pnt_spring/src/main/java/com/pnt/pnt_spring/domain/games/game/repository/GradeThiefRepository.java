package com.pnt.pnt_spring.domain.games.game.repository;

import com.pnt.pnt_spring.domain.members.stat.entity.GradeThief;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeThiefRepository extends JpaRepository<GradeThief, Long> {
}
