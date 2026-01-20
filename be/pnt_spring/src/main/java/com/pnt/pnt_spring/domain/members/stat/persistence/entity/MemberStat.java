package com.pnt.pnt_spring.domain.members.stat.persistence.entity;

import com.pnt.pnt_spring.domain.members.member.persistence.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_stat")
public class MemberStat {

    @Id
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private Integer totalGames;
    private Integer wins;
    private Integer loses;
    private Integer policeGame;
    private Integer thiefGame;
}