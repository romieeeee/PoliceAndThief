package com.pnt.pnt_spring.domain.chats.application;

public interface ChatRoomModerationService {
	void kickAndBan3Days(Long actorMemberId, Long chatRoomId, Long targetMemberId, String reason);
}
