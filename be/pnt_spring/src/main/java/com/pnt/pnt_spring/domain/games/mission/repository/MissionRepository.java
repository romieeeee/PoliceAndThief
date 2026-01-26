package com.pnt.pnt_spring.domain.games.mission.repository;

import com.pnt.pnt_spring.domain.games.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionRepository extends JpaRepository<Mission, Long> {

}
