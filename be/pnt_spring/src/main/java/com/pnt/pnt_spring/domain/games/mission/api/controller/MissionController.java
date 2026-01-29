package com.pnt.pnt_spring.domain.games.mission.api.controller;

import com.pnt.pnt_spring.domain.games.mission.api.req.MissionRequest;
import com.pnt.pnt_spring.domain.games.mission.api.resp.MissionResponse;
import com.pnt.pnt_spring.domain.games.mission.application.MissionService;
import com.pnt.pnt_spring.global.api.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Game Mission API", description = "게임 미션 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/games")
public class MissionController {

    private final MissionService missionService;

    @Operation(summary = "미션 목록 조회", description = "도둑들이 수행할 전체 미션 목록을 조회합니다.")
    @GetMapping("/{gameId}/missions")
    public CommonResponse<List<MissionResponse>> getMissions(@PathVariable Long gameId) {

        List<MissionResponse> response = missionService.getMissions(gameId);

        return new CommonResponse<>(response, "미션 목록 조회 성공", HttpStatus.OK);
    }

    @Operation(summary = "미션 상세 조회", description = "미션 상세 정보를 조회합니다.")
    @GetMapping("/{gameId}/missions/{missionId}")
    public CommonResponse<MissionResponse> getMissionDetail(
            @PathVariable Long gameId,
            @PathVariable Long missionId) {

        MissionResponse response = missionService.getMissionDetail(gameId, missionId);

        return new CommonResponse<>(response, "미션 상세 조회 성공", HttpStatus.OK);
    }

    @Operation(summary = "미션 수행 제출 (Internal)", description = "Node.js 서버에서 호출하는 내부 API입니다. 근데 이거 안씀")
    @PostMapping("/missions/submit")
    public CommonResponse<Boolean> submitMission(
            @RequestBody MissionRequest request) { // PathVariable 제거

        Boolean isSuccess = missionService.submitMission(
                request.getGameId(),
                request.getMissionId(),
                request.getThiefId()
        );

        return new CommonResponse<>(isSuccess, "미션 수행 결과 처리 완료", HttpStatus.OK);
    }
}