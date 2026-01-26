package com.pnt.pnt_spring.domain.chats.api.controller;

import com.pnt.pnt_spring.domain.chats.api.req.ChatRoomKickRequest;
import com.pnt.pnt_spring.domain.chats.application.ChatRoomModerationService;
import com.pnt.pnt_spring.domain.chats.utils.SecurityUtils;
import com.pnt.pnt_spring.global.api.response.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chats")
public class ChatRoomModerationController {

    private final ChatRoomModerationService moderationService;

    @PostMapping("/{chatRoomId}/kick")
    public CommonResponse<Void> kick(@PathVariable Long chatRoomId,
                                     @Valid @RequestBody ChatRoomKickRequest req) {

        Long actorMemberId = SecurityUtils.currentMemberId();

        moderationService.kickAndBan3Days(
                actorMemberId,
                chatRoomId,
                req.getTargetMemberId(),
                req.getReason()
        );

        return new CommonResponse<>(null, "강퇴 처리 성공", HttpStatus.OK);
    }
}
