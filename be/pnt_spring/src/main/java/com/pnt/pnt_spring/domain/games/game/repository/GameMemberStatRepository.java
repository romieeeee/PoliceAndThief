package com.pnt.pnt_spring.domain.games.game.repository;

import com.pnt.pnt_spring.domain.games.game.entity.GameMemberStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GameMemberStatRepository extends JpaRepository<GameMemberStat, Long> {

    // 경찰 MVP: 특정 게임(gameId)에서 경찰 역할인 사람들 중 체포 수 1등
    @Query("""
        SELECT s 
        FROM GameMemberStat s
        JOIN FETCH s.gameMember gm
        JOIN FETCH gm.member m
        JOIN FETCH m.memberProfile
        WHERE gm.game.id = :gameId
          AND gm.givenPosition = com.pnt.pnt_spring.domain.games.game.entity.GameMemberPosition.POLICE
        ORDER BY s.arrestCount DESC
        LIMIT 1
    """)
    Optional<GameMemberStat> findPoliceMvp(@Param("gameId") Long gameId);

    // 도둑 MVP: 특정 게임(gameId)에서 도둑 역할인 사람들 중 생존 시간 1등
    @Query("""
        SELECT s 
        FROM GameMemberStat s
        JOIN FETCH s.gameMember gm
        JOIN FETCH gm.member m
        JOIN FETCH m.memberProfile
        WHERE gm.game.id = :gameId
          AND gm.givenPosition = com.pnt.pnt_spring.domain.games.game.entity.GameMemberPosition.THIEF
        ORDER BY s.longestSurvived DESC
        LIMIT 1
    """)
    Optional<GameMemberStat> findThiefMvp(@Param("gameId") Long gameId);

    Optional<GameMemberStat> findByGameMemberId(Long gameMemberId);
}