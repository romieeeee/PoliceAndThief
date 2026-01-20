package com.pnt.pnt_spring.domain.chats.persistence.entity;

import com.pnt.pnt_spring.domain.members.member.persistence.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "join_chat_room",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"member_id", "chat_room_id"})
        }
)
public class JoinChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    private OffsetDateTime createdAt;
}