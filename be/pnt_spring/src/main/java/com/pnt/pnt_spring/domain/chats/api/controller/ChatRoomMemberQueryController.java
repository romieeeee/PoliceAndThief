package com.pnt.pnt_spring.domain.chats.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pnt.pnt_spring.domain.chats.api.resp.ChatRoomMemberInfoResponse;
import com.pnt.pnt_spring.domain.chats.api.resp.MyChatRoomListResponse;
import com.pnt.pnt_spring.domain.chats.application.ChatRoomMemberQueryService;
import com.pnt.pnt_spring.global.api.response.CommonResponse;
import com.pnt.pnt_spring.global.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chats")
public class ChatRoomMemberQueryController {

	private final ChatRoomMemberQueryService queryService;

	// 채팅방 참여 멤버 목록 조회
	@GetMapping("/{chatRoomId}/members")
	public CommonResponse<List<ChatRoomMemberInfoResponse>> list(@PathVariable Long chatRoomId) {
		Long requesterId = SecurityUtils.currentMemberId();
		List<ChatRoomMemberInfoResponse> data = queryService.listMembers(requesterId, chatRoomId);

		return new CommonResponse<>(data, "채팅방 멤버 목록 조회 성공", HttpStatus.OK);
	}

	@GetMapping("/me/rooms")
	public MyChatRoomListResponse myRooms() {
		Long memberId = SecurityUtils.currentMemberId();
		return queryService.myJoinedRooms(memberId);
	}
}
