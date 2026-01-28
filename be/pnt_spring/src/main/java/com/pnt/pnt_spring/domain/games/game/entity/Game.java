package com.pnt.pnt_spring.domain.games.game.entity;

import com.pnt.pnt_spring.domain.games.game.enums.GameStatus;
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

    /** 방장(게임방 생성자) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
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

    @Column(nullable = false)
    private Integer caughtCount;

    public static Game createWaitingRoom(Member host, String roomCode) {
        Game game = new Game();
        game.host = host;
        game.roomCode = roomCode;
        game.status = GameStatus.WAITING;
        game.winTeam = WinTeam.NONE;
        game.caughtCount = 0;
        return game;
    }

    public boolean isWaiting() {
        return this.status == GameStatus.WAITING;
    }

    public boolean isInGame() {
        return this.status == GameStatus.IN_GAME;
    }

    public boolean isEnded() {
        return this.status == GameStatus.ENDED;
    }

    public boolean isHost(Long memberId) {
        return this.host != null && this.host.getId().equals(memberId);
    }

    /** 방장 위임 */
    public void changeHost(Member newHost) {
        if (newHost == null) {
            throw new IllegalArgumentException("newHost는 null일 수 없습니다.");
        }
        this.host = newHost;
    }

    public void start() {
        if (!isWaiting()) {
            throw new IllegalStateException("WAITING 상태에서만 시작할 수 있습니다.");
        }
        this.status = GameStatus.IN_GAME;
        this.startTime = OffsetDateTime.now();
    }

    public void end(WinTeam winTeam) {
        if (!isInGame()) {
            throw new IllegalStateException("IN_GAME 상태에서만 종료할 수 있습니다.");
        }
        this.status = GameStatus.ENDED;
        this.endTime = OffsetDateTime.now();
        this.winTeam = (winTeam == null ? WinTeam.NONE : winTeam);
    }

    /** 방 닫기(삭제 플래그) - WAITING에서만 닫도록 제한하고 싶으면 조건 추가하세요 */
    public void close() {
        this.isDeleted = true;
        // updatedAt은 BaseEntity에서 처리하는게 일반적이라 여기서 건드리지 않는 걸 추천
    }
}
