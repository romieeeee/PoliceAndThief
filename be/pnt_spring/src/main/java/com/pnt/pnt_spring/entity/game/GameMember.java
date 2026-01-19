package com.pnt.pnt_spring.entity.game;

import com.pnt.pnt_spring.entity.user.Member;
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
public class GameMember {

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

    @Column(length = 10)
    private String givenPosition;

    private Boolean ready;

    @Column(length = 10)
    private String status;
}