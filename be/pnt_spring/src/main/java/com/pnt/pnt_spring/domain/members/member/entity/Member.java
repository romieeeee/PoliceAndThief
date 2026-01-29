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

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(length = 50, unique = true, nullable = true)
    private String email;

    private LocalDate birth;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private MemberProfile memberProfile;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private MemberAuthProvider memberAuthProvider;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private MemberStat memberStat;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private MemberStatPolice memberStatPolice;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private MemberStatThief memberStatThief;

}