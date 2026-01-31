package com.pnt.pnt_spring.domain.members.member.application;

import com.pnt.pnt_spring.domain.members.member.api.req.MemberProfileUpdateRequest;
import com.pnt.pnt_spring.domain.members.member.api.resp.MemberProfileResponse;
import com.pnt.pnt_spring.domain.members.member.api.resp.MemberProfileUpdateResponse;
import com.pnt.pnt_spring.domain.members.stat.api.resp.MemberPoliceResponse;
import com.pnt.pnt_spring.domain.members.stat.api.resp.MemberThiefResponse;

public interface MemberService {

	// 조회
	MemberProfileResponse getMemberProfile(Long memberId);

	// 경찰 스탯 조회
	MemberPoliceResponse getPoliceProfile(Long memberId);

	// 도둑 스탯 조회
	MemberThiefResponse getThiefProfile(Long memberId);

	// 프로필 수정
	MemberProfileUpdateResponse updateProfile(Long memberId, MemberProfileUpdateRequest request);

}
