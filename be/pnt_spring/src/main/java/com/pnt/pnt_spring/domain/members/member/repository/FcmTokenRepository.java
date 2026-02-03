package com.pnt.pnt_spring.domain.members.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pnt.pnt_spring.domain.members.member.entity.FcmToken;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
}
