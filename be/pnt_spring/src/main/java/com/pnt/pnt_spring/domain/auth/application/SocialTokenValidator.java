package com.pnt.pnt_spring.domain.auth.application;

import com.pnt.pnt_spring.global.api.code.ErrorCode;
import com.pnt.pnt_spring.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialTokenValidator {

    @Value("${oauth2.kakao.app-id}") // 검증
    private String kakaoAppId;

    private final RestTemplate restTemplate = new RestTemplate();

    public String validateAndGetId(String provider, String token) {
        if ("KAKAO".equalsIgnoreCase(provider)) {
            return validateKakao(token);
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    private String validateKakao(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + token);
            headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

            HttpEntity<Void> request = new HttpEntity<>(headers);

            // 토큰 유효성 및 앱 ID 검증 (더 안전함)
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v1/user/access_token_info",
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            Map body = response.getBody();
            String appId = String.valueOf(body.get("appId"));

            if (!appId.equals(kakaoAppId)) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN, "유효하지 않은 카카오 토큰입니다(App ID 불일치).");
            }

            return String.valueOf(body.get("id"));

        } catch (Exception e) {
            log.error("Kakao Token Validation Error: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "카카오 로그인 실패");
        }
    }
}