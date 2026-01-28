package com.pnt.pnt_spring.domain.games.game.entity;

import com.pnt.pnt_spring.domain.games.game.enums.GameMemberStatus;
import com.pnt.pnt_spring.domain.games.game.enums.Position;
import com.pnt.pnt_spring.domain.games.game.enums.PreferPosition;
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

    @Column(name = "serial_code")
    private String serialCode;

    // === 선호 포지션 (픽) ===
    @Enumerated(EnumType.STRING)
    @Column(name = "prefer_position", length = 10, nullable = false)
    private PreferPosition preferPosition;

    // === 배정 포지션 (게임 시작 시 확정) ===
    @Enumerated(EnumType.STRING)
    @Column(name = "given_position", length = 10)
    private Position givenPosition; // 시작 전 null 가능

    @Column(nullable = false)
    private Boolean ready;

    @Column(length = 10)
    @Enumerated(value = EnumType.STRING)
    private GameMemberStatus status;

    private Boolean inGameConnected;

    /* =========================
       생성/상태 변경 메서드
       ========================= */

    public static GameMember join(Game game, Member member) {
        GameMember gm = new GameMember();
        gm.game = game;
        gm.member = member;
        gm.ready = false;
        gm.preferPosition = PreferPosition.ANY; // 기본값: 상관없음
        gm.givenPosition = null;                // 배정은 게임 시작 때
        gm.status = null;
        return gm;
    }

    public void rejoin() {
        // isDeleted 같은 소프트딜리트 쓰면 여기서 복구
        // this.isDeleted = false;
        this.ready = false;        // 재입장 시 준비 풀지 여부는 정책(보통 false)
        this.givenPosition = null; // 재입장 시 배정 초기화할지 여부는 정책
    }

    public void toggleReady(boolean ready) {
        this.ready = ready;
    }

    public void toggleReady() {
        this.ready = !this.ready;
    }

    // “픽” prefer를 변경
    public void pickPreferPosition(PreferPosition preferPosition) {
        if (preferPosition == null) {
            throw new IllegalArgumentException("preferPosition은 null일 수 없습니다.");
        }
        this.preferPosition = preferPosition;
    }

    // 배정은 게임 시작 단계에서
    public void assignPosition(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("givenPosition은 null일 수 없습니다.");
        }
        this.givenPosition = position;
    }


}
