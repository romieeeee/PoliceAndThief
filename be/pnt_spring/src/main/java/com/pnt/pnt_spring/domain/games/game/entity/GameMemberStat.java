package com.pnt.pnt_spring.domain.games.game.entity;

import com.pnt.pnt_spring.domain.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "game_member_stat")
public class GameMemberStat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_member_id", nullable = false)
    private GameMember gameMember;

    @Enumerated(EnumType.STRING)
    private GameMemberPosition position;

    private Integer walk = 0;

    @Column(name = "arrest_count")
    private Integer arrestCount = 0;

    @Column(name = "longest_survived")
    private Integer longestSurvived = 0;

    // Service에서 사용할 생성자 추가
    public GameMemberStat(GameMember gameMember) {
        this.gameMember = gameMember;
        this.position = gameMember.getGivenPosition();
        this.walk = 0;
        this.arrestCount = 0;
        this.longestSurvived = 0;
    }

    @Builder
    public GameMemberStat(GameMember gameMember, GameMemberPosition position, Integer walk, Integer arrestCount, Integer longestSurvived) {
        this.gameMember = gameMember;
        this.position = position;
        this.walk = walk;
        this.arrestCount = arrestCount;
        this.longestSurvived = longestSurvived;
    }

    // 스탯 초기화 설정
    public static GameMemberStat createInitialStat(GameMember gameMember) {
        return GameMemberStat.builder()
                .gameMember(gameMember)
                .position(gameMember.getGivenPosition()) // 게임 멤버의 포지션을 가져옴
                .walk(0)
                .arrestCount(0)
                .longestSurvived(0)
                .build();
    }

    // 결과 업데이트
    public void updateResultStats(Integer walk, Integer longestSurvived) {
        this.walk = walk;
        this.longestSurvived = longestSurvived;
    }
}