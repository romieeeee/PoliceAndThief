package com.pnt.pnt_spring.domain.members.member.application.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pnt.pnt_spring.domain.members.member.api.req.FcmTokenRegisterRequest;
import com.pnt.pnt_spring.domain.members.member.api.req.FcmTokenUpdateRequest;
import com.pnt.pnt_spring.domain.members.member.api.req.MemberProfileUpdateRequest;
import com.pnt.pnt_spring.domain.members.member.api.resp.FcmTokenRegisterResponse;
import com.pnt.pnt_spring.domain.members.member.api.resp.FcmTokenUpdateResponse;
import com.pnt.pnt_spring.domain.members.member.api.resp.MemberProfileResponse;
import com.pnt.pnt_spring.domain.members.member.api.resp.MemberProfileUpdateResponse;
import com.pnt.pnt_spring.domain.members.member.application.MemberService;
import com.pnt.pnt_spring.domain.members.member.entity.FcmToken;
import com.pnt.pnt_spring.domain.members.member.entity.Member;
import com.pnt.pnt_spring.domain.members.member.entity.document.MemberDoc;
import com.pnt.pnt_spring.domain.members.member.repository.FcmTokenRepository;
import com.pnt.pnt_spring.domain.members.member.repository.jpa.MemberRepository;
import com.pnt.pnt_spring.domain.members.member.repository.mongo.MemberMongoRepository;
import com.pnt.pnt_spring.domain.members.stat.api.resp.MemberPoliceResponse;
import com.pnt.pnt_spring.domain.members.stat.api.resp.MemberThiefResponse;
import com.pnt.pnt_spring.global.api.code.ErrorCode;
import com.pnt.pnt_spring.global.exception.BusinessException;
import com.pnt.pnt_spring.global.utils.S3Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

	private final MemberRepository memberRepository;
	private final MemberMongoRepository memberMongoRepository;
	private final FcmTokenRepository fcmTokenRepository;
	private final S3Service s3Service;

	// 멤버 프로필 조회
	@Override
	public MemberProfileResponse getMemberProfile(Long memberId) {
		// 1. DB에서 멤버 정보 조회
		Member member = memberRepository.findMemberWithAllStats(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		// 2. DB에 저장된 Key 꺼내기 (예: "profiles/1/eb9ab..._1")
		String storedKey = member.getMemberProfile().getAvatarUrl();

		// 3. Key를 이용해 "조회용 Presigned URL" 생성
		// 이 메서드가 "https://...amazon...?Signature=..." 형태의 긴 URL을 리턴
		String viewableUrl = s3Service.getPresignedGetUrl(storedKey);

		// 4. 응답 DTO 만들기
		MemberProfileResponse response = MemberProfileResponse.from(member);

		response.setAvatarUrl(viewableUrl);

		return response;
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
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		String oldAvatarKey = member.getMemberProfile().getAvatarUrl();
		String newAvatarKey = request.getAvatarUrl();

		member.getMemberProfile().updateProfile(request.getNickname(), newAvatarKey);

		MemberDoc memberDoc = memberMongoRepository.findByMemberId(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		memberDoc.update(request.getNickname(), newAvatarKey);

		if (oldAvatarKey != null && !oldAvatarKey.isBlank() && !oldAvatarKey.equals(newAvatarKey)) {
			// "기본 이미지"가 있다면 그것은 삭제하면 안 됨 (예: "profiles/default.png")
			if (!isDefaultImage(oldAvatarKey)) {
				s3Service.deleteFile(oldAvatarKey);
			}
		}

		String viewableUrl = s3Service.getPresignedGetUrl(newAvatarKey);
		MemberProfileUpdateResponse response = MemberProfileUpdateResponse.from(member.getMemberProfile());
		response.setAvatarUrl(viewableUrl);

		return response;
	}

	@Override
	@Transactional
	public FcmTokenRegisterResponse registerFcmToken(Long memberId, FcmTokenRegisterRequest request) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		FcmToken token = FcmToken.builder()
			.member(member)
			.value(request.getValue())
			.isActive(request.isActive())
			.build();

		FcmToken savedToken = fcmTokenRepository.save(token);

		return FcmTokenRegisterResponse.from(savedToken);
	}

	@Override
	@Transactional
	public FcmTokenUpdateResponse updateFcmToken(Long memberId, FcmTokenUpdateRequest request) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		FcmToken fcmToken = member.getFcmToken();
		fcmToken.updateActive(request.isActive());

		return FcmTokenUpdateResponse.from(fcmToken);
	}

	// 헬퍼 메서드
	private boolean isDefaultImage(String key) {
		return "profiles/default.png".equals(key);
	}

}
