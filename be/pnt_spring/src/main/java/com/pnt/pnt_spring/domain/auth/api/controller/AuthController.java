package com.pnt.pnt_spring.domain.auth.api.controller;

import com.pnt.pnt_spring.domain.auth.api.req.*;
import com.pnt.pnt_spring.domain.auth.api.resp.LoginResponse;
import com.pnt.pnt_spring.domain.auth.api.resp.SignupResponse;
import com.pnt.pnt_spring.domain.auth.application.AuthService;
import com.pnt.pnt_spring.global.api.response.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원가입
    @PostMapping("/signup")
    public CommonResponse<SignupResponse> signup(@RequestBody SignupRequest request) {
        SignupResponse signupResponse = authService.signup(request);
        return new CommonResponse<>(signupResponse, "회원가입에 성공했습니다.", HttpStatus.OK);
    }

    // 로그인
    @PostMapping("/login")
    public CommonResponse<LoginResponse> signin(@RequestBody LoginRequest request){
        LoginResponse loginResponse = authService.login(request);
        return new CommonResponse<>(loginResponse, "로그인에 성공했습니다.", HttpStatus.OK);
    }

    // 로그아웃
    @PostMapping("/logout")
    public CommonResponse<String> logout(HttpServletRequest request){

        String accessToken = resolveToken(request);

        authService.logout(accessToken);

        return new CommonResponse<>("로그아웃 되었습니다.", "로그아웃 성공", HttpStatus.OK);
    }

    // 아이디 중복 체크
    @PostMapping("/duplicate")
    public CommonResponse<Map<String, Boolean>> checkDuplicate(@RequestBody IdDuplicateRequest request) {

        boolean isDuplicate = authService.checkIdDuplicate(request.getId());

        Map<String, Boolean> responseData = Map.of("duplicated", isDuplicate);

        return new CommonResponse<>(responseData, "아이디 중복 확인 완료", HttpStatus.OK);
    }

    @PostMapping("/social-login")
    public CommonResponse<LoginResponse> socialLogin(@RequestBody SocialLoginRequest request) {
        LoginResponse loginResponse = authService.socialLogin(request);
        return new CommonResponse<>(loginResponse, "소셜 로그인 성공", HttpStatus.OK);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null; // 혹은 예외 처리
    }

    @PostMapping("/reissue")
    public CommonResponse<TokenDto> reissue(@RequestBody TokenDto tokenDto) {
        TokenDto newToken = authService.reissue(tokenDto);
        return new CommonResponse<>(newToken, "토큰이 성공적으로 재발급되었습니다.", HttpStatus.OK);
    }
}
