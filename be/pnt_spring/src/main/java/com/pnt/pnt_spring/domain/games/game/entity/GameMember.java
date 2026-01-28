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
        uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "member_id"})
)
public class GameMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
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

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Enumerated(value = EnumType.STRING)
    private GameMemberStatus status;

    private Boolean inGameConnected;
    private String status;

    /* =========================
       생성/상태 변경 메서드
       ========================= */

    public static GameMember join(Game game, Member member) {
        if (game == null) throw new IllegalArgumentException("game은 null일 수 없습니다.");
        if (member == null) throw new IllegalArgumentException("member는 null일 수 없습니다.");

        GameMember gm = new GameMember();
        gm.game = game;
        gm.member = member;

        gm.isDeleted = false;
        gm.ready = false;
        gm.preferPosition = PreferPosition.ANY;
        gm.givenPosition = null;

        gm.status = null;
        gm.inGameConnected = false; // 기본값(컬럼 null 싫으면 false 추천)
        return gm;
    }

    /** 재입장: 소프트삭제 복구 + 상태 초기화(정책) */
    public void rejoin() {
        this.isDeleted = false;

        // 정책: 재입장하면 ready는 풀고, 배정 포지션도 초기화
        this.ready = false;
        this.givenPosition = null;

        // 상태값도 대기방 기준으로 초기화하고 싶으면 null/WAITING 같은 걸로 맞추기
        this.status = null;
        this.inGameConnected = false;
    }

    /** 나가기: 소프트삭제 */
    public void leave() {
        this.isDeleted = true;

        // 정책: 나가면 ready/포지션은 남겨도 되지만,
        // 목록/정원은 isDeleted=false만 보니까 실제 기능엔 영향 없음
        this.ready = false;
        this.givenPosition = null;
        this.inGameConnected = false;
    }

    /** 강퇴: 소프트삭제 */
    public void kick() {
        this.isDeleted = true;

        // 나가기와 동일하게 정리
        this.ready = false;
        this.givenPosition = null;
        this.inGameConnected = false;
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
