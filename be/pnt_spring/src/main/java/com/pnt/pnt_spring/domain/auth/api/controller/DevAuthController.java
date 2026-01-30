package com.pnt.pnt_spring.domain.auth.api.controller;

import java.util.Collections;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pnt.pnt_spring.domain.auth.api.req.TokenDto;
import com.pnt.pnt_spring.domain.auth.jwt.CustomUserDetails;
import com.pnt.pnt_spring.domain.auth.jwt.JwtTokenProvider;
import com.pnt.pnt_spring.global.api.response.CommonResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth/dev")
@RequiredArgsConstructor
@Slf4j
public class DevAuthController {

	private final JwtTokenProvider jwtTokenProvider;

	@GetMapping("/token")
	public TokenDto createDevToken(@RequestParam(value = "id", defaultValue = "tester") Long id) {
		// 더미 유저 생성
		UserDetails userDetails = new User(String.valueOf(id), "q1w2e3r4",
			Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, "",
			userDetails.getAuthorities());

		// 토큰 발급
		return jwtTokenProvider.generateToken(authentication, id);
	}

	@GetMapping("/test")
	public CommonResponse<?> testApi(@AuthenticationPrincipal CustomUserDetails details) {

		Long memberId = details.getMemberId();

		log.info("test loginId={}", details.getUsername());
		log.info("test memberId={}", memberId);

		return new CommonResponse<>(details, "테스트 api", HttpStatus.OK);
	}
}