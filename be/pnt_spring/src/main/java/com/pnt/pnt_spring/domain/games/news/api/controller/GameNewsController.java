package com.pnt.pnt_spring.domain.games.news.api.controller;

import com.pnt.pnt_spring.domain.games.news.api.resp.AiNewsResponse;
import com.pnt.pnt_spring.domain.games.news.application.Impl.GameNewsServiceImpl;
import com.pnt.pnt_spring.global.api.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/news")
@RequiredArgsConstructor
@Slf4j
public class GameNewsController {

    private final GameNewsServiceImpl gameNewsService;

    @Operation(summary = "AI 뉴스 저장", description = "FastAPI 서버로부터 생성된 뉴스를 받아 DB에 저장합니다.")
    @PostMapping("/result")
    public CommonResponse<Void> saveGameNews(@RequestBody AiNewsResponse response) {
        return new CommonResponse<>(null, "AI 뉴스 저장 성공", HttpStatus.OK);
    }
}