package com.pnt.pnt_spring.domain.chats.application.impl;

import java.beans.Transient;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import com.pnt.pnt_spring.domain.chats.application.ChatRoomModerationService;
import com.pnt.pnt_spring.domain.chats.entity.ChatRoom;
import com.pnt.pnt_spring.domain.chats.entity.ChatRoomBan;
import com.pnt.pnt_spring.domain.chats.entity.MemberChatRoom;
import com.pnt.pnt_spring.domain.chats.repository.ChatRoomBanRepository;
import com.pnt.pnt_spring.domain.chats.repository.ChatRoomRepository;
import com.pnt.pnt_spring.domain.chats.repository.MemberChatRoomRepository;
import com.pnt.pnt_spring.domain.members.member.entity.Member;
import com.pnt.pnt_spring.domain.members.member.repository.jpa.MemberRepository;
import com.pnt.pnt_spring.global.api.code.ErrorCode;
import com.pnt.pnt_spring.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRoomModerationServiceImpl implements ChatRoomModerationService {

	private final ChatRoomRepository chatRoomRepository;
	private final MemberRepository memberRepository;
	private final MemberChatRoomRepository memberChatRoomRepository;
	private final ChatRoomBanRepository chatRoomBanRepository;

	@Override
	@Transient
	public void kickAndBan3Days(Long actorMemberId, Long chatRoomId, Long targetMemberId, String reason) {
		if (actorMemberId.equals(targetMemberId)) {
			throw new BusinessException(ErrorCode.CHAT_ROOM_FORBIDDEN); // 셀프 강퇴 금지
		}

		ChatRoom room = chatRoomRepository.findById(chatRoomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

		// 권한: 방장만 강퇴 가능(정책)
		if (!room.getOwnerId().equals(actorMemberId)) {
			throw new BusinessException(ErrorCode.CHAT_ROOM_FORBIDDEN);
		}
		Member actor = memberRepository.findById(actorMemberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		Member target = memberRepository.findById(targetMemberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		// 대상이 현재 방에 참여중인지 확인 (is_deleted=false)
		MemberChatRoom mcr = memberChatRoomRepository.findByChatRoomIdAndMemberId(chatRoomId, targetMemberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

		if (!!mcr.isDeleted()) {
			// 이미 나간 상태/삭제 상태면 “강퇴” 대신 밴만 걸지, 에러칠지 정책 선택
			// 여기선 밴만 걸 수 있게 그대로 진행해도 됨.
		} else {
			// 참여중이면 강퇴 처리
			mcr.kickOut(); // 아래에 MemberChatRoom에 메서드 추가 예시 제공
			room.decreaseCurrentMembersSafely(); // 아래에 ChatRoom 메서드 예시 제공
		}

		OffsetDateTime now = OffsetDateTime.now();
		OffsetDateTime until = now.plusDays(3);

		ChatRoomBan ban = chatRoomBanRepository.findByChatRoomIdAndMemberId(chatRoomId, targetMemberId)
			.orElse(null);

		if (ban == null) {
			ban = ChatRoomBan.builder()
				.chatRoom(room)
				.member(target)
				.bannedBy(actor)
				.bannedAt(now)
				.bannedUntil(until)
				.reason(reason)
				.build();
		} else {
			ban.extendOrOverwrite(now, until, reason, actor);
		}

		chatRoomBanRepository.save(ban);

		// (옵션) 웹소켓 이벤트: target에게 강퇴 알림/연결 끊기 등
		// websocketPublisher.publishKicked(chatRoomId, targetMemberId, until);

	}
}
