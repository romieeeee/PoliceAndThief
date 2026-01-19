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
@Table(name = "game")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_user_id", nullable = false)
    private Member host;

    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    @Column(length = 20)
    private String status;

    @Column(length = 20)
    private String winTeam;

    @Column(length = 10)
    private String roomCode;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
