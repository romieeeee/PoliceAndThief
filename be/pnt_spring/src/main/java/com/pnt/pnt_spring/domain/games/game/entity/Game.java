package com.pnt.pnt_spring.domain.games.game.entity;

import com.pnt.pnt_spring.domain.games.game.enums.GameStatus;
import com.pnt.pnt_spring.domain.games.game.enums.WinTeam;
import com.pnt.pnt_spring.domain.games.game.enums.WinTeam;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WinTeam winTeam;

    @Column(length = 10, nullable = false, unique = true)
    private String roomCode;

    /* =========================
       생성/상태 변경 메서드
       ========================= */

    public static Game createWaitingRoom(Member host, String roomCode) {
        Game game = new Game();
        game.host = host;
        game.roomCode = roomCode;
        game.status = GameStatus.WAITING;
        game.winTeam = WinTeam.NONE;
        return game;
    }

    public void start() {
        if (this.status != GameStatus.WAITING) {
            throw new IllegalStateException("WAITING 상태에서만 시작할 수 있습니다.");
        }
        this.status = GameStatus.IN_GAME;
        this.startTime = OffsetDateTime.now();
    }

    public void end(WinTeam winTeam) {
        if (this.status != GameStatus.IN_GAME) {
            throw new IllegalStateException("IN_GAME 상태에서만 종료할 수 있습니다.");
        }
        this.status = GameStatus.ENDED;
        this.endTime = OffsetDateTime.now();
        this.winTeam = (winTeam == null ? WinTeam.NONE : winTeam);
    }

    public boolean isWaiting() {
        return this.status == GameStatus.WAITING;
    }

    public boolean isInGame() {
        return this.status == GameStatus.IN_GAME;
    }

    public boolean isHost(Long memberId) {
        return this.host != null && this.host.getId().equals(memberId);
    }
}
