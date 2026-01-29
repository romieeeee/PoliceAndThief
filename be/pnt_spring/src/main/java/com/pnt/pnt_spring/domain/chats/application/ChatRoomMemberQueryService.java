package com.pnt.pnt_spring.domain.chats.application;

import com.pnt.pnt_spring.domain.chats.api.resp.ChatRoomMemberInfoResponse;
import com.pnt.pnt_spring.domain.chats.api.resp.MyChatRoomListResponse;

import java.util.List;

public interface ChatRoomMemberQueryService {
    List<ChatRoomMemberInfoResponse> listMembers(Long requesterId, Long chatRoomId);

    MyChatRoomListResponse myJoinedRooms(Long memberId);
}
