package com.pnt.pnt_spring.domain.members.stat.repository;

import com.pnt.pnt_spring.domain.members.stat.entity.MemberStatPolice;
import org.springframework.data.jpa.repository.JpaRepository; // 수정됨

public interface MemberStatPoliceRepository extends JpaRepository<MemberStatPolice, Long> {
}