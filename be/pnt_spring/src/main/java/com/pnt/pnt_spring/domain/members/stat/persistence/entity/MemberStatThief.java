package com.pnt.pnt_spring.domain.members.stat.persistence.entity;

import com.pnt.pnt_spring.domain.members.member.persistence.entity.Member;
import com.pnt.pnt_spring.domain.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_stat_thief")
public class MemberStatThief extends BaseEntity {

    @Id
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private Integer escapeCount;
    private Integer totalMissionCount;
    private Integer longestSurvivalSec;
    private Integer averageSurvivalSec;

    @Column(length = 10)
    private String gradeThief;
}