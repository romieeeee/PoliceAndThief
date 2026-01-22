package com.pnt.pnt_spring.domain.chats.api.controller;

import com.pnt.pnt_spring.domain.chats.api.req.ChatRoomDisconnectRequest;
import com.pnt.pnt_spring.domain.chats.api.req.ChatRoomJoinRequest;
import com.pnt.pnt_spring.domain.chats.api.resp.ChatRoomMemberResponse;
import com.pnt.pnt_spring.domain.chats.application.ChatRoomMemberService;
import com.pnt.pnt_spring.domain.chats.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chats")
public class ChatRoomMemberController {

    private final ChatRoomMemberService chatRoomMemberService;

    // 채팅방 참여
    @PostMapping("/{id}/join")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatRoomMemberResponse join(@PathVariable("id") Long chatRoomId) {
        Long memberId = SecurityUtils.currentMemberId();
        return chatRoomMemberService.join(memberId, chatRoomId);
    }

    @PostMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable("id") Long chatRoomId) {
        Long memberId = SecurityUtils.currentMemberId();
        chatRoomMemberService.leave(memberId, chatRoomId);
    }

    @PostMapping("/{id}/connect")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void connect(@PathVariable("id") Long chatRoomId) {
        Long memberId = SecurityUtils.currentMemberId();
        chatRoomMemberService.connect(memberId, chatRoomId);
    }

    @PostMapping("/{id}/disconnect")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect(@PathVariable("id") Long chatRoomId) {
        Long memberId = SecurityUtils.currentMemberId();
        chatRoomMemberService.disconnect(memberId, chatRoomId);
    }


}
