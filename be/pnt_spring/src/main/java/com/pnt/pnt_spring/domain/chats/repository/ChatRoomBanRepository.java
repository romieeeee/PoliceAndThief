package com.pnt.pnt_spring.domain.chats.repository;

import com.pnt.pnt_spring.domain.chats.entity.ChatRoomBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ChatRoomBanRepository extends JpaRepository<ChatRoomBan, Long> {

    Optional<ChatRoomBan> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    @Query("""
        select case when count(b) > 0 then true else false end
        from ChatRoomBan b
        where b.chatRoom.id = :chatRoomId
          and b.member.id = :memberId
          and b.bannedUntil > :now
    """)
    boolean existsActiveBan(Long chatRoomId, Long memberId, OffsetDateTime now);
}
