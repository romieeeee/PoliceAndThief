package com.pnt.pnt_spring.entity.game;

import com.pnt.pnt_spring.entity.user.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "game_skill",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"game_id", "member_id"})
        }
)
public class GameSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    private Boolean isUsed;
    private OffsetDateTime usedAt;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}