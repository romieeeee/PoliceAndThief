package com.pnt.pnt_spring.domain.members.member.api.controller;

import com.pnt.pnt_spring.domain.members.member.api.req.MemberProfileUpdateRequest;
import com.pnt.pnt_spring.domain.members.member.api.resp.MemberProfileResponse;
import com.pnt.pnt_spring.domain.members.member.api.resp.MemberProfileUpdateResponse;
import com.pnt.pnt_spring.domain.members.member.application.MemberService;
import com.pnt.pnt_spring.domain.members.stat.api.resp.MemberPoliceResponse;
import com.pnt.pnt_spring.domain.members.stat.api.resp.MemberThiefResponse;
import com.pnt.pnt_spring.global.api.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Member", description = "멤버 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;


    @Operation(summary = "멤버 프로필 조회", description = "멤버 ID를 받아 멤버 프로필을 조회합니다.")
    @GetMapping("/{id}")
    public CommonResponse<MemberProfileResponse> getMemberProfile(@PathVariable("id") Long memberId){

        MemberProfileResponse response = memberService.getMemberProfile(memberId);
        return new CommonResponse<>(response, "멤버 프로필 조회 완료", HttpStatus.OK);
    }


    @Operation(summary = "멤버 경찰 프로필 조회", description = "멤버 ID를 받아 멤버의 경찰 프로필을 조회합니다.")
    @GetMapping("/{id}/police")
    public CommonResponse<MemberPoliceResponse> getMemberPoliceProfile(@PathVariable("id") Long memberId){
        MemberPoliceResponse response = memberService.getPoliceProfile(memberId);
        return new CommonResponse<>(response, "경찰 프로필 조회 완료", HttpStatus.OK);
    }

    @Operation(summary = "멤버 도둑 프로필 조회", description = "멤버 ID를 받아 멤버의 도둑 프로필을 조회합니다.")
    @GetMapping("/{id}/thief")
    public CommonResponse<MemberThiefResponse> getMemberThiefProfile(@PathVariable("id") Long memberId){
        MemberThiefResponse response = memberService.getThiefProfile(memberId);
        return new CommonResponse<>(response, "도둑 프로필 조회 완료", HttpStatus.OK);
    }

    @Operation(summary = "멤버 업데이트", description = "멤버 닉네임, 프로필 사진을 업데이트 합니다.")
    @PatchMapping("/{id}")
    public CommonResponse<MemberProfileUpdateResponse> updateProfile(
            @PathVariable("id") Long memberId,
            @RequestBody MemberProfileUpdateRequest request) {
        MemberProfileUpdateResponse response = memberService.updateProfile(memberId, request);
        return new CommonResponse<>(response, "프로필 수정 완료", HttpStatus.OK);
    }

}
