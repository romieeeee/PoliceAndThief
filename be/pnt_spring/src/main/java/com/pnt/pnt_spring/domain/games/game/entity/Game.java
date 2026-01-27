package com.pnt.pnt_spring.domain.games.game.entity;

import com.pnt.pnt_spring.domain.members.member.entity.Member;
import com.pnt.pnt_spring.domain.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "game")
public class Game extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_member_id", nullable = false)
    private Member host;

    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    @Column(length = 20)
    @Enumerated(value = EnumType.STRING)
    private GameStatus status;

    @Column(length = 20)
    private String winTeam;

    @Column(length = 10)
    private String roomCode;

    @Column(nullable = false)
    private Integer caughtedCount;

    public void finish(String winnerTeam){
        this.status = GameStatus.FINISHED;
        this.winTeam = winnerTeam;
        this.endTime = OffsetDateTime.now();
    }
}
