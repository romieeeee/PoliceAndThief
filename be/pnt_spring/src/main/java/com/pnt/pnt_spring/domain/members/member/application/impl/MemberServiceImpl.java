package com.pnt.pnt_spring.domain.members.member.application.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pnt.pnt_spring.domain.members.member.api.req.MemberProfileUpdateRequest;
import com.pnt.pnt_spring.domain.members.member.api.resp.MemberProfileResponse;
import com.pnt.pnt_spring.domain.members.member.api.resp.MemberProfileUpdateResponse;
import com.pnt.pnt_spring.domain.members.member.application.MemberService;
import com.pnt.pnt_spring.domain.members.member.entity.Member;
import com.pnt.pnt_spring.domain.members.member.entity.document.MemberDoc;
import com.pnt.pnt_spring.domain.members.member.repository.jpa.MemberRepository;
import com.pnt.pnt_spring.domain.members.member.repository.mongo.MemberMongoRepository;
import com.pnt.pnt_spring.domain.members.stat.api.resp.MemberPoliceResponse;
import com.pnt.pnt_spring.domain.members.stat.api.resp.MemberThiefResponse;
import com.pnt.pnt_spring.domain.members.stat.repository.MemberStatPoliceRepository;
import com.pnt.pnt_spring.domain.members.stat.repository.MemberStatThiefRepository;
import com.pnt.pnt_spring.global.api.code.ErrorCode;
import com.pnt.pnt_spring.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

	private final MemberRepository memberRepository;
	private final MemberStatPoliceRepository memberStatPoliceRepository;
	private final MemberStatThiefRepository memberStatThiefRepository;
	private final MemberMongoRepository memberMongoRepository;

	// 멤버 프로필 조회
	@Override
	public MemberProfileResponse getMemberProfile(Long memberId) {

		Member member = memberRepository.findMemberWithAllStats(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		return MemberProfileResponse.from(member);
	}

	@Override
	public MemberPoliceResponse getPoliceProfile(Long memberId) {

		// 멤버 존재 확인
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		return MemberPoliceResponse.of(memberId, member.getMemberStatPolice());
	}

	@Override
	public MemberThiefResponse getThiefProfile(Long memberId) {

		// 멤버 존재 확인
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		// 도둑 스탯 확인
		return MemberThiefResponse.of(memberId, member.getMemberStatThief());
	}

	@Override
	@Transactional
	public MemberProfileUpdateResponse updateProfile(Long memberId, MemberProfileUpdateRequest request) {
		// 프로필 조회
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)); // 혹은 PROFILE_NOT_FOUND

		// 데이터 수정
		member.getMemberProfile().updateProfile(request.getNickname(), request.getAvatarUrl());

		MemberDoc memberDoc = memberMongoRepository.findByMemberId(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		memberDoc.update(request.getNickname(), request.getAvatarUrl());

		// 변경된 정보 반환
		return MemberProfileUpdateResponse.from(member.getMemberProfile());
	}

}
