package com.pnt.pnt_spring.domain.chats.api;

import com.pnt.pnt_spring.domain.chats.api.resp.ChatRoomMemberInfoResponse;
import com.pnt.pnt_spring.domain.chats.application.ChatRoomMemberQueryService;
import com.pnt.pnt_spring.domain.chats.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chats")
public class ChatRoomMemberQueryController {

    private final ChatRoomMemberQueryService queryService;

    @GetMapping("/{chatRoomId}/members")
    public List<ChatRoomMemberInfoResponse> list(@PathVariable Long chatRoomId) {
        Long requesterId = SecurityUtils.currentMemberId();
        return queryService.listMembers(requesterId, chatRoomId);
    }
}
