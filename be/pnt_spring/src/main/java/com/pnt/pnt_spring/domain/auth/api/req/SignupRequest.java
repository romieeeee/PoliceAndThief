package com.pnt.pnt_spring.domain.auth.api.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class SignupRequest {
    @NotBlank(message = "아이디는 필수입니다.")
    private String id; // user ID 아니고 로그인 ID

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    private String passwordConfirm;

    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    private String email;
    private LocalDate birth;
    private String avatarUrl;

    public String getAvatarUrl() {
        if (avatarUrl == null) {
            return "default.png";
        }
        return avatarUrl;
    }
}