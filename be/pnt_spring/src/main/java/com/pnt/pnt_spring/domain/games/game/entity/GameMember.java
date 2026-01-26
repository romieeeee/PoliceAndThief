package com.pnt.pnt_spring.domain.games.game.entity;

import com.pnt.pnt_spring.domain.games.game.enums.Position;
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

    // 지금 너 코드에 있던 serialCode는 목적이 불명확해서 일단 유지
    private String serialCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Position givenPosition;

    @Column(nullable = false)
    private Boolean ready;

    @Column(length = 10)
    private String status;

    /* =========================
       생성/상태 변경 메서드
       ========================= */

    public static GameMember join(Game game, Member member) {
        GameMember gm = new GameMember();
        gm.game = game;
        gm.member = member;
        gm.ready = false;          // 기본은 미준비
        gm.givenPosition = null;   // 포지션은 나중에 배정
        gm.status = "JOINED";      // 이건 다음 단계에서 enum으로 바꿔도 됨
        return gm;
    }

    public void toggleReady(boolean ready) {
        this.ready = ready;
    }

    public void assignPosition(Position position) {
        this.givenPosition = position;
    }
}
