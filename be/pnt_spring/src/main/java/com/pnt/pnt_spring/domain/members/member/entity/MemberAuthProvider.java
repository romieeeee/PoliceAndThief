package com.pnt.pnt_spring.domain.members.member.entity;

import com.pnt.pnt_spring.domain.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "member_auth_providers",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "provider_user_key"})
        }
)
public class MemberAuthProvider extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(length = 10, nullable = false)
    private String provider;

    @Column(name = "provider_user_key", nullable = false)
    private String providerUserKey;
}