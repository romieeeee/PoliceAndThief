package com.pnt.pnt_spring.domain.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.scret}")
    private String secretKey;
    private Key key;
    private final long accessTokenValidTime = 30 * 60 * 1000L; // 유효 시간 30분
    private final long refreshTokenValidTime = 3 * 24 * 60 * 60 * 1000L; // 리프레시 토큰 3일



}
