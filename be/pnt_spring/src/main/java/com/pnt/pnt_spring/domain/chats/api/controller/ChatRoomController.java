package com.pnt.pnt_spring.domain.chats.api.controller;

import com.pnt.pnt_spring.domain.chats.api.req.ChatRoomCreateRequest;
import com.pnt.pnt_spring.domain.chats.api.req.ChatRoomUpdateRequest;
import com.pnt.pnt_spring.domain.chats.api.resp.ChatRoomListResponse;
import com.pnt.pnt_spring.domain.chats.api.resp.ChatRoomResponse;
import com.pnt.pnt_spring.domain.chats.application.ChatRoomService;
import com.pnt.pnt_spring.domain.chats.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chats")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatRoomResponse create(@RequestBody @Valid ChatRoomCreateRequest req) {
        Long memberId = SecurityUtils.currentMemberId();
        return chatRoomService.create(memberId, req);
    }

    @GetMapping("/{id}")
    public ChatRoomResponse get(@PathVariable Long id){
        Long memberId = SecurityUtils.currentMemberId();
        return chatRoomService.get(memberId, id);
    }

    // 채팅방 목록 조회
    @GetMapping
    public ChatRoomListResponse list(
            @RequestParam(required = false) Integer regionCode,
            @RequestParam(required = false) String title
    ) {
        Long memberId = SecurityUtils.currentMemberId();
        List<ChatRoomResponse> rooms = chatRoomService.list(memberId, regionCode, title);
        return ChatRoomListResponse.from(rooms);
    }

    @PatchMapping("/{id}")
    public ChatRoomResponse update(
            @PathVariable Long id,
            @RequestBody @Valid ChatRoomUpdateRequest req
    ) {
        Long memberId = SecurityUtils.currentMemberId();
        return chatRoomService.update(memberId, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        Long memberId = SecurityUtils.currentMemberId();
        chatRoomService.delete(memberId, id);
    }
}
