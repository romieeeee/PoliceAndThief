package com.pnt.pnt_spring.domain.members.member.api.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberProfileUpdateRequest {

    @Schema(description = "변경할 닉네임", example = "hello")
    private String nickname;

    @Schema(description = "변경할 프로필 이미지 URL", example = "https://example.com/image.png")
    private String avatarUrl;
}