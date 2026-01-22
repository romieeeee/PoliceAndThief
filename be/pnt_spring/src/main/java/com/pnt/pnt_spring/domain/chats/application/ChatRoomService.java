package com.pnt.pnt_spring.domain.chats.application;

import com.pnt.pnt_spring.domain.chats.api.req.ChatRoomCreateRequest;
import com.pnt.pnt_spring.domain.chats.api.req.ChatRoomUpdateRequest;
import com.pnt.pnt_spring.domain.chats.api.resp.ChatRoomResponse;

import java.util.List;

public interface ChatRoomService {

    ChatRoomResponse create(Long memberId, ChatRoomCreateRequest req);

    ChatRoomResponse get(Long memberId, Long chatRoomId);

    List<ChatRoomResponse> list(Long memberId, Integer regionCode, String title);

    ChatRoomResponse update(Long memberId, Long chatRoomId, ChatRoomUpdateRequest req);

    void delete(Long memberId, Long chatRoomId);
}
