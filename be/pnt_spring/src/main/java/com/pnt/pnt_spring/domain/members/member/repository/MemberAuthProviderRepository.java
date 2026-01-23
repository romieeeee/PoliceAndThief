package com.pnt.pnt_spring.domain.members.member.repository;

import com.pnt.pnt_spring.domain.members.member.entity.MemberAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberAuthProviderRepository extends JpaRepository<MemberAuthProvider, Long> {
    Optional<MemberAuthProvider> findByProviderAndProviderUserKey(String provider, String providerUserKey);
}
