package com.pnt.pnt_spring.domain.games.game.application.impl;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomCreateRequest;
import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomReadyRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomCreateResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomMemberItem;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomMemberListResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomReadyResponse;
import com.pnt.pnt_spring.domain.games.game.application.GameRoomCodeGenerator;
import com.pnt.pnt_spring.domain.games.game.application.GameRoomService;
import com.pnt.pnt_spring.domain.games.game.entity.Game;
import com.pnt.pnt_spring.domain.games.game.entity.GameMember;
import com.pnt.pnt_spring.domain.games.game.entity.GameSetting;
import com.pnt.pnt_spring.domain.games.game.enums.GameStatus;
import com.pnt.pnt_spring.domain.games.game.repository.GameMemberRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameSettingRepository;
import com.pnt.pnt_spring.domain.members.member.entity.Member;
import com.pnt.pnt_spring.domain.members.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GameRoomServiceImpl implements GameRoomService {

    private final GameRepository gameRepository;
    private final GameMemberRepository gameMemberRepository;
    private final GameSettingRepository gameSettingRepository;
    private final MemberRepository memberRepository;
    private final GameRoomCodeGenerator gameRoomCodeGenerator;

    @Override
    public GameRoomCreateResponse createRoom(Long hostMemberId, GameRoomCreateRequest req) {
        Member hostRef = memberRepository.getReferenceById(hostMemberId);

        String roomCode = gameRoomCodeGenerator.generateUniqueCode();

        // 1) Game 생성/저장
        Game game = Game.createWaitingRoom(hostRef, roomCode);
        gameRepository.save(game);

        // 2) Setting 생성/저장
        GameSetting setting = GameSetting.createDefault(game);

        if (req != null) {
            if (req.getTimeLimitSec() != null) setting.updateTimeLimit(req.getTimeLimitSec());
            if (req.getPoliceCount() != null || req.getThiefCount() != null) {
                setting.updateCounts(req.getPoliceCount(), req.getThiefCount());
            }
        }

        gameSettingRepository.save(setting);

        // 3) Host 자동 참가
        GameMember hostMember = GameMember.join(game, hostRef);
        gameMemberRepository.save(hostMember);

        return new GameRoomCreateResponse(game.getId(), game.getRoomCode(), GameStatus.WAITING);
    }

    @Override
    @Transactional(readOnly = true)
    public GameRoomMemberListResponse getRoomMembers(Long roomId) {

        // 방 존재 체크 (없으면 404/예외)
        gameRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        List<GameRoomMemberItem> items = gameMemberRepository.findRoomMemberItems(roomId);
        return new GameRoomMemberListResponse(roomId, items);
    }

    @Override
    public GameRoomReadyResponse updateReady(Long roomId, Long memberId, GameRoomReadyRequest req) {

        // 1) 방 락 + 존재 확인
        Game game = gameRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        // 2) 상태 체크 (WAITING에서만 ready)
        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("게임이 이미 시작되어 준비 상태를 변경할 수 없습니다.");
        }

        // 3) 내 gameMember row 락 + 존재 확인
        GameMember gm = gameMemberRepository.findByGameIdAndMemberIdForUpdate(roomId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("방에 참가한 멤버가 아닙니다."));

        // 4) ready 업데이트 (엔티티 메서드 사용)
        gm.toggleReady(req.isReady());

        // save는 없어도 dirty checking으로 반영됨(트랜잭션 안이라)
        return new GameRoomReadyResponse(roomId, memberId, gm.getReady());
    }

}
