package com.pnt.pnt_spring.domain.games.news.application;

import com.pnt.pnt_spring.domain.games.news.api.resp.AiNewsResponse;

public interface GameNewsService {
    void saveNews(AiNewsResponse response);
}