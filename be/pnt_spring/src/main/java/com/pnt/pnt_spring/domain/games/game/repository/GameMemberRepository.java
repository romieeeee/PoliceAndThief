package com.pnt.pnt_spring.domain.games.game.repository;

import com.pnt.pnt_spring.domain.games.game.entity.GameMember;
import com.pnt.pnt_spring.domain.games.game.entity.GameMemberPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameMemberRepository extends JpaRepository<GameMember, Long> {

    // 특정 게임에 참여한 모든 유저 조회
    List<GameMember> findAllByGameId(Long gameId);

    // 특정 게임에서 특정 역할을 가진 유저들 조회
    // Entity의 필드명(givenPosition)에 맞춰 쿼리 메소드를 작성합니다.
    List<GameMember> findAllByGameIdAndGivenPosition(Long gameId, GameMemberPosition givenPosition);
}