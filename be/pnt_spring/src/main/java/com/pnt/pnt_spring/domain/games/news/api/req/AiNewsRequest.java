package com.pnt.pnt_spring.domain.games.news.api.req;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiNewsRequest {
    private String start_time;        // yyyy-MM-dd HH:mm
    private String winning_team;      // "경찰" 또는 "도둑"
    private int play_time;         // 플레이 경과 시간(초 정보)
    private String location;          // "장소 정보"
    private int thief_count;
    private int police_count;
    private String mvp;               // MVP 닉네임
    private String winner_top_member; // 승리팀 최고 공헌자
    private String loser_top_member;  // 패배팀 최고 공헌자
}