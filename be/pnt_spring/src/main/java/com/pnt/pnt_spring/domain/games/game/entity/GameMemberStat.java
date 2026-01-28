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
@Table(name = "game_member_stat",
        uniqueConstraints = @UniqueConstraint(columnNames = "game_member_id"))
public class GameMemberStat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_member_id", nullable = false)
    private GameMember gameMember;

    @Column(length = 10)
    private String position;

    private Integer walk = 0;
    private Integer arrestCount = 0;
    private Integer longestSurvived = 0;

    // Service에서 사용할 생성자 추가
    public GameMemberStat(GameMember gameMember) {
        this.gameMember = gameMember;
        this.position = gameMember.getGivenPosition().name();
        this.walk = 0;
        this.arrestCount = 0;
        this.longestSurvived = 0;
    }

    public static GameMemberStat create(GameMember gameMember) {
        GameMemberStat stat = new GameMemberStat();
        stat.gameMember = gameMember;
        stat.position = gameMember.getGivenPosition().name();
        stat.walk = 0;
        stat.arrestCount = 0;
        stat.longestSurvived = 0;
        return stat;
    }

    // 생존 시간 업데이트 메서드 추가
    public void updateLongestSurvived(int survivalSec) {
        this.longestSurvived = survivalSec;
    }

    @Builder
    public GameMemberStat(GameMember gameMember, String position, Integer walk, Integer arrestCount, Integer longestSurvived) {
        this.gameMember = gameMember;
        this.position = position;
        this.walk = walk;
        this.arrestCount = arrestCount;
        this.longestSurvived = longestSurvived;
    }
}