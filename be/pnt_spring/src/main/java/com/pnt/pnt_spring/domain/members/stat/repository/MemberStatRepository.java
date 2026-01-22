package com.pnt.pnt_spring.domain.members.stat.repository;

import com.pnt.pnt_spring.domain.members.stat.entity.MemberStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberStatRepository extends JpaRepository<MemberStat, Long> {
    
}
