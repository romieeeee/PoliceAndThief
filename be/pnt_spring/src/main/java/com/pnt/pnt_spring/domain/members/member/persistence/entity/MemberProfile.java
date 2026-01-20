package com.pnt.pnt_spring.domain.members.member.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_profile")
public class MemberProfile {

    @Id
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(length = 20)
    private String nickname;

    private String avatarUrl;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}