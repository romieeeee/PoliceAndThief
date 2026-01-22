package com.pnt.pnt_spring.domain.auth.application.impl;

import com.pnt.pnt_spring.domain.auth.api.req.LoginRequest;
import com.pnt.pnt_spring.domain.auth.api.req.SignupRequest;
import com.pnt.pnt_spring.domain.auth.api.req.TokenDto;
import com.pnt.pnt_spring.domain.auth.api.resp.LoginResponse;
import com.pnt.pnt_spring.domain.auth.api.resp.SignupResponse;
import com.pnt.pnt_spring.domain.auth.application.AuthService;
import com.pnt.pnt_spring.domain.auth.jwt.JwtTokenProvider;
import com.pnt.pnt_spring.domain.members.member.entity.Member;
import com.pnt.pnt_spring.domain.members.member.entity.MemberProfile;
import com.pnt.pnt_spring.domain.members.member.entity.MemberRole;
import com.pnt.pnt_spring.domain.members.member.repository.MemberProfileRepository;
import com.pnt.pnt_spring.domain.members.member.repository.MemberRepository;
import com.pnt.pnt_spring.global.api.code.ErrorCode;
import com.pnt.pnt_spring.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public SignupResponse signup(SignupRequest request) {

        // 비밀번호 일치 확인
        if(!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "비밀번호가 일치하지 않습니다.");
        }

        // id 중복체크
        if(checkIdDuplicate(request.getId())){
            throw new BusinessException(ErrorCode.DUPLICATE_USER_ID, "이미 사용 중인 아이디입니다.");
        }

        // 이메일 체크
        if(memberRepository.existsByEmail(request.getEmail())){
            // ErrorCode에 DUPLICATE_EMAIL이 없다면 새로 만드시거나 VALIDATION_ERROR 등을 사용하세요.
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "이미 사용 중인 이메일입니다.");
        }

        // Member 엔터티 생성 및 저장
        Member member = Member.builder()
                .loginId(request.getId())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .birth(request.getBirth())
                .role(MemberRole.USER) // 일반 회원가입 시 유저 권한 부여
                .build();

        memberRepository.save(member);

        // MemberProfile 엔터티 생성 및 저장
        MemberProfile memberProfile = MemberProfile.builder()
                .member(member)
                .nickname(request.getNickname())
                .avatarUrl(request.getAvatarUrl())
                .build();

        memberProfileRepository.save(memberProfile);

        return SignupResponse.from(member, memberProfile);
    }

    // 아이디 중복 체크
    public boolean checkIdDuplicate(String loginId){
        return memberRepository.existsByLoginId(loginId);
    }


    public LoginResponse login(LoginRequest request) {

        // 유저 조회
        Member member = memberRepository.findByLoginId(request.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));


        // 패스워드 검사
        if(!passwordEncoder.matches(request.getPassword(), member.getPassword())){
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 프로필 조회
        MemberProfile memberProfile = memberProfileRepository.findByMember(member)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));


        // 인증 객체 생성 (DB에 저장된 Role 사용)
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                member.getLoginId(),
                null,
                List.of(new SimpleGrantedAuthority(member.getRole().getKey()))
        );

        // JWT 발급
        TokenDto tokenDto = jwtTokenProvider.generateToken(authentication, member.getId());

        // Redis
        // RT(Key - String) : loginId(Value - String)
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(
                "RT:" + member.getLoginId(),
                tokenDto.getRefreshToken(),
                tokenDto.getRefreshTokenExpiresIn(),
                TimeUnit.MILLISECONDS
        );

        // 응답 반환
        return LoginResponse.of(tokenDto, member, memberProfile);
    }

    @Transactional
    public void logout(String accessToken){
        // Access Token 검증
        if(!jwtTokenProvider.validateToken(accessToken)){
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "유효하지 않는 토큰입니다");
        }

        // Access Token 에서 유저 정보
        Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
        String loginId = authentication.getName();

        // redis 에서 해당 유저 refresh token 삭제
        if(redisTemplate.opsForValue().get("RT:" + loginId) != null){
            redisTemplate.delete("RT:" + loginId);
        }

        // 해당 Access Token 로그아웃(블랙리스트 처리)
        Long expiration = jwtTokenProvider.getExpiration(accessToken);
        if (expiration > 0){
            redisTemplate.opsForValue().set("BL:" + accessToken, "logout", expiration, TimeUnit.SECONDS);
        }
    }

}
