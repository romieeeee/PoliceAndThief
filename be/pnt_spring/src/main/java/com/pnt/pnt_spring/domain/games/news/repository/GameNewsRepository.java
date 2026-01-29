package com.pnt.pnt_spring.domain.games.news.repository;

import com.pnt.pnt_spring.domain.games.news.entity.GameNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameNewsRepository extends JpaRepository<GameNews, Long> {
    // 게임 ID로 뉴스를 조회
    Optional<GameNews> findByGameId(Long gameId);
}