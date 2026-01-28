package com.pnt.pnt_spring.domain.games.game.application.impl;

import com.pnt.pnt_spring.domain.games.game.api.req.*;
import com.pnt.pnt_spring.domain.games.game.api.resp.*;
import com.pnt.pnt_spring.domain.games.game.application.GameRoomMemberService;
import com.pnt.pnt_spring.domain.games.game.entity.Game;
import com.pnt.pnt_spring.domain.games.game.entity.GameMember;
import com.pnt.pnt_spring.domain.games.game.entity.GameSetting;
import com.pnt.pnt_spring.domain.games.game.enums.GameStatus;
import com.pnt.pnt_spring.domain.games.game.enums.PreferPosition;
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
public class GameRoomMemberServiceImpl implements GameRoomMemberService {

    private final GameRepository gameRepository;
    private final GameMemberRepository gameMemberRepository;
    private final GameSettingRepository gameSettingRepository;
    private final MemberRepository memberRepository;

    @Override
    public GameRoomJoinResponse joinRoom(Long memberId, GameRoomJoinRequest req) {
        if (req == null || req.getRoomCode() == null || req.getRoomCode().isBlank()) {
            throw new IllegalArgumentException("roomCode가 필요합니다.");
        }

        String roomCode = req.getRoomCode().trim();

        Game game = gameRepository.findByRoomCodeForUpdate(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방 코드입니다."));

        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("이미 시작된 게임방에는 참여할 수 없습니다.");
        }

        GameSetting setting = gameSettingRepository.findById(game.getId())
                .orElseThrow(() -> new IllegalArgumentException("게임 설정이 존재하지 않습니다."));

        long current = gameMemberRepository.countByGameId(game.getId());
        if (current >= setting.getPlayerCount()) {
            throw new IllegalStateException("게임방 정원이 가득 찼습니다.");
        }

        Member memberRef = memberRepository.getReferenceById(memberId);

        GameMember gm = gameMemberRepository.findByGameIdAndMemberIdForUpdate(game.getId(), memberId)
                .orElse(null);

        if (gm == null) {
            gm = GameMember.join(game, memberRef);
            gameMemberRepository.save(gm);
        } else {
            gm.rejoin();
        }

        return new GameRoomJoinResponse(
                game.getId(),
                game.getRoomCode(),
                memberId,
                gm.getPreferPosition(),
                gm.getGivenPosition(),
                gm.getReady()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GameRoomMemberListResponse getRoomMembers(Long roomId) {
        gameRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        List<GameRoomMemberItem> items = gameMemberRepository.findRoomMemberItems(roomId);
        return new GameRoomMemberListResponse(roomId, items);
    }

    @Override
    public GameRoomReadyResponse updateReady(Long roomId, Long memberId, GameRoomReadyRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("ready 요청 바디가 필요합니다.");
        }

        Game game = gameRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("게임이 이미 시작되어 준비 상태를 변경할 수 없습니다.");
        }

        GameMember gm = gameMemberRepository.findByGameIdAndMemberIdForUpdate(roomId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("방에 참가한 멤버가 아닙니다."));

        gm.toggleReady();

        return new GameRoomReadyResponse(roomId, memberId, gm.getReady());
    }

    @Override
    public GameRoomPositionResponse pickPosition(Long actorMemberId, Long roomId, GameRoomPositionRequest req) {
        if (req == null) throw new IllegalArgumentException("position 요청 바디가 필요합니다.");
        PreferPosition prefer = req.getPreferPosition();
        if (prefer == null) throw new IllegalArgumentException("preferPosition은 필수입니다.");

        Game game = gameRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("대기방에서만 포지션 픽을 변경할 수 있습니다.");
        }

        GameMember gm = gameMemberRepository.findByGameIdAndMemberIdForUpdate(roomId, actorMemberId)
                .orElseThrow(() -> new IllegalArgumentException("방에 참가한 멤버가 아닙니다."));

        gm.pickPreferPosition(prefer);

        return new GameRoomPositionResponse(
                roomId,
                actorMemberId,
                gm.getPreferPosition(),
                gm.getGivenPosition(),
                gm.getReady()
        );
    }
}
