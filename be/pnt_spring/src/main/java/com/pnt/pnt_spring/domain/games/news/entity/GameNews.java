package com.pnt.pnt_spring.domain.games.news.entity;

import com.pnt.pnt_spring.domain.games.game.entity.Game;
import com.pnt.pnt_spring.domain.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "game_news")
@Builder
@AllArgsConstructor
public class GameNews extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String contents;

    public GameNews(Game game, String title, String contents) {
        this.game = game;
        this.title = title;
        this.contents = contents;
    }
}