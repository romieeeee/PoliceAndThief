package com.pnt.pnt_spring.domain.members.stat.persistence.entity;

import com.pnt.pnt_spring.domain.members.member.persistence.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_stat_police")
public class MemberStatPolice {

    @Id
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private Integer totalArrestCount;
    private Integer mostArrestsInGame;

    @Column(length = 10)
    private String gradePolice;
}