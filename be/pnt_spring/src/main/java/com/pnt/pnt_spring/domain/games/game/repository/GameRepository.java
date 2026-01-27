package com.pnt.pnt_spring.domain.games.game.repository;

import com.pnt.pnt_spring.domain.games.game.entity.Game;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findByRoomCode(String roomCode);

    boolean existsByRoomCode(String roomCode);

    // WAITING 방 코드 중복 방지(원하면)
    boolean existsByRoomCodeAndStatus(String roomCode, String status);

    // 시작/설정 변경 등 "게임 1개"를 강하게 잡고 처리할 때
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Game g where g.id = :gameId")
    Optional<Game> findByIdForUpdate(@Param("gameId") Long gameId);

    // roomCode로도 락 (선택)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Game g where g.roomCode = :roomCode")
    Optional<Game> findByRoomCodeForUpdate(@Param("roomCode") String roomCode);
}
