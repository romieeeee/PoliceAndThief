package com.pnt.pnt_spring.domain.members.member.entity;

import com.pnt.pnt_spring.domain.members.stat.entity.MemberStat;
import com.pnt.pnt_spring.domain.members.stat.entity.MemberStatPolice;
import com.pnt.pnt_spring.domain.members.stat.entity.MemberStatThief;
import com.pnt.pnt_spring.domain.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "member")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", length = 20, nullable = false, unique = true) // 5자리 이상 ~ 12자리 이하
    private String loginId;

    @Column(nullable = false) // 8자리 ~ 16 자리 이하 --> 특수문자 필요X
    private String password;

    @Column(length = 50, unique = true)
    private String email;

    private LocalDate birth;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    // 연관관계 설정(과다 조회 방지)
    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private MemberProfile memberProfile;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private MemberStat memberStat;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private MemberStatPolice memberStatPolice;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private MemberStatThief memberStatThief;

}