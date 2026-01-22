package com.pnt.pnt_spring.domain.members.stat.api.resp;

import com.pnt.pnt_spring.domain.members.stat.entity.MemberStatThief;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ThiefStatResponse { // 별도 파일로 독립
    private Integer escapeCount;
    private Integer averageSurvivalTimeSec;
    private Integer missionClearCount;
    private Integer longestSurvivalSec;
    private String grade;

    public static ThiefStatResponse from(MemberStatThief stat) {
        if (stat == null) {
            return ThiefStatResponse.builder()
                    .escapeCount(0).averageSurvivalTimeSec(0)
                    .missionClearCount(0).longestSurvivalSec(0).grade("좀도둑")
                    .build();
        }
        String gradeName = "좀도둑";
        if (stat.getGradeThief() != null && stat.getGradeThief().getName() != null) {
            gradeName = stat.getGradeThief().getName();
        }

        return ThiefStatResponse.builder()
                .escapeCount(stat.getEscapeCount())
                .averageSurvivalTimeSec(stat.getAverageSurvivalSec())
                .missionClearCount(stat.getTotalMissionCount())
                .longestSurvivalSec(stat.getLongestSurvivalSec())
                .grade(gradeName)
                .build();
    }
}