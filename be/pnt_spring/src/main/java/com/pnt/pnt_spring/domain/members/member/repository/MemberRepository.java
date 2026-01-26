package com.pnt.pnt_spring.domain.members.member.repository;

import com.pnt.pnt_spring.domain.members.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByLoginId(String loginId);

    Optional<Member> findByLoginId(String loginId);

    boolean existsByEmail(String email);

    @Query("SELECT m FROM Member m " +
            "LEFT JOIN FETCH m.memberProfile " +
            "LEFT JOIN FETCH m.memberStat " +
            "LEFT JOIN FETCH m.memberStatPolice " +
            "LEFT JOIN FETCH m.memberStatThief " +
            "WHERE m.id = :id")
    Optional<Member> findMemberWithAllStats(@Param("id") Long id);

    @Query("""
        select m
        from Member m
        left join fetch m.memberProfile mp
        where m.id in :ids
    """)
    List<Member> findAllWithProfileByIdIn(List<Long> ids);
}
