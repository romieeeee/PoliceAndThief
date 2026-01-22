package com.pnt.pnt_spring.domain.auth.application;


import com.pnt.pnt_spring.domain.auth.api.req.LoginRequest;
import com.pnt.pnt_spring.domain.auth.api.req.SignupRequest;
import com.pnt.pnt_spring.domain.auth.api.resp.LoginResponse;
import com.pnt.pnt_spring.domain.auth.api.resp.SignupResponse;

public interface AuthService {

    // 회원가입
    SignupResponse signup(SignupRequest request);

    // 아이디 중복 체크
    boolean checkIdDuplicate(String loginId);

    // 로그인
    LoginResponse login(LoginRequest request);

    // 로그아웃
    void logout(String accessToken);
}