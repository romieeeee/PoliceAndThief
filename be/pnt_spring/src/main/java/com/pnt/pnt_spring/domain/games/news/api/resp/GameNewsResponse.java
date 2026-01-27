package com.pnt.pnt_spring.domain.games.news.api.resp;

import com.pnt.pnt_spring.domain.games.news.entity.GameNews;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class GameNewsResponse {
    private Long newsId;
    private Long gameId;
    private String title;
    private String content;
    private OffsetDateTime createdAt;

    public static GameNewsResponse from(GameNews news) {
        return GameNewsResponse.builder()
                .newsId(news.getId())
                .gameId(news.getGame().getId())
                .title(news.getTitle())
                .content(news.getContent())
                .createdAt(news.getCreatedAt())
                .build();
    }
}