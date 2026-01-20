package com.pnt.pnt_spring.domain.games.news.persistence.entity;

import com.pnt.pnt_spring.domain.games.game.persistence.entity.Game;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "game_news")
public class GameNews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    private String title;
    private String contents;

    private OffsetDateTime createdAt;
}