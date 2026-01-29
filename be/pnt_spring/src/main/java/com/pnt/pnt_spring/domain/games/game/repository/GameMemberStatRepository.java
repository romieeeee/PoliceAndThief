package com.pnt.pnt_spring.domain.games.game.repository;

import com.pnt.pnt_spring.domain.games.game.entity.GameMemberStat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameMemberStatRepository extends JpaRepository<GameMemberStat, Long> {

    Optional<GameMemberStat> findByGameMemberId(Long gameMemberId);

    boolean existsByGameMemberId(Long gameMemberId);

    // 경찰 MVP
    @Query("""
        SELECT s 
        FROM GameMemberStat s
        JOIN FETCH s.gameMember gm
        JOIN FETCH gm.member m
        JOIN FETCH m.memberProfile
        WHERE gm.game.id = :gameId
          AND gm.givenPosition = com.pnt.pnt_spring.domain.games.game.enums.Position.POLICE
        ORDER BY s.arrestCount DESC
    """)
    List<GameMemberStat> findPoliceMvp(@Param("gameId") Long gameId, Pageable pageable);

    // 도둑 MVP
    @Query("""
        SELECT s 
        FROM GameMemberStat s
        JOIN FETCH s.gameMember gm
        JOIN FETCH gm.member m
        JOIN FETCH m.memberProfile
        WHERE gm.game.id = :gameId
          AND gm.givenPosition = com.pnt.pnt_spring.domain.games.game.enums.Position.THIEF
        ORDER BY s.longestSurvived DESC
    """)
    List<GameMemberStat> findThiefMvp(@Param("gameId") Long gameId, Pageable pageable);

    // 특정 게임의 모든 참가자 스탯 조회
    @Query("SELECT s FROM GameMemberStat s " +
            "JOIN FETCH s.gameMember gm " +
            "JOIN FETCH gm.member m " +
            "JOIN FETCH m.memberProfile " +
            "WHERE gm.game.id = :gameId")
    List<GameMemberStat> findAllByGameId(@Param("gameId") Long gameId);
}