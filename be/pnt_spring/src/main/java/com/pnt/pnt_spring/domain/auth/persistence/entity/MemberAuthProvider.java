package com.pnt.pnt_spring.domain.auth.persistence.entity;

import com.pnt.pnt_spring.domain.members.member.persistence.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "member_auth_providers",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "provider_user_key"})
        }
)
public class MemberAuthProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(length = 10, nullable = false)
    private String provider;

    @Column(name = "provider_user_key", nullable = false)
    private String providerUserKey;

    private OffsetDateTime connectedAt;
}