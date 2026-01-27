package com.pnt.pnt_spring.domain.games.game.entity;

import com.pnt.pnt_spring.domain.members.member.entity.Member;
import com.pnt.pnt_spring.domain.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "game_member",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"game_id", "member_id"})
        }
)
public class GameMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private String serialCode;

    @Enumerated(value = EnumType.STRING)
    @Column(length = 10)
    private GameMemberPosition givenPosition;

    @Column(length = 10)
    @Enumerated(value = EnumType.STRING)
    private GameMemberPosition preferPosition;

    private Boolean ready;

    @Column(length = 10)
    @Enumerated(value = EnumType.STRING)
    private GameMemberStatus status;

    private Boolean inGameConnected;
}