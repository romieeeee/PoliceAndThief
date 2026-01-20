package com.pnt.pnt_spring.domain.auth.controller;

import com.pnt.pnt_spring.domain.auth.dto.TokenDto;
import com.pnt.pnt_spring.domain.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@RequestMapping("/auth/dev")
@RequiredArgsConstructor
public class DevAuthController {

    private final JwtTokenProvider jwtTokenProvider;

    // http://localhost:8080/auth/dev/token?id=tester
    @GetMapping("/token")
    public TokenDto createDevToken(@RequestParam(value = "id", defaultValue = "tester") String id) {
        // 더미 유저 생성
        UserDetails userDetails = new User(id, "", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());

        // 토큰 발급
        return jwtTokenProvider.generateToken(authentication);
    }
}