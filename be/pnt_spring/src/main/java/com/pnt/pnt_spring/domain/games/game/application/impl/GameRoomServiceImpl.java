package com.pnt.pnt_spring.domain.games.game.application.impl;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomCreateRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomCreateResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameStartResponse;
import com.pnt.pnt_spring.domain.games.game.application.GameRoomCodeGenerator;
import com.pnt.pnt_spring.domain.games.game.application.GameRoomService;
import com.pnt.pnt_spring.domain.games.game.entity.Game;
import com.pnt.pnt_spring.domain.games.game.entity.GameMember;
import com.pnt.pnt_spring.domain.games.game.entity.GameSetting;
import com.pnt.pnt_spring.domain.games.game.enums.GameStatus;
import com.pnt.pnt_spring.domain.games.game.enums.PreferPosition;
import com.pnt.pnt_spring.domain.games.game.enums.Position;
import com.pnt.pnt_spring.domain.games.game.repository.GameMemberRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameSettingRepository;
import com.pnt.pnt_spring.domain.members.member.entity.Member;
import com.pnt.pnt_spring.global.api.code.ErrorCode;
import com.pnt.pnt_spring.global.exception.BusinessException;
import com.pnt.pnt_spring.domain.members.member.repository.jpa.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.*;
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

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    public GameRoomCreateResponse createRoom(Long hostMemberId, GameRoomCreateRequest req) {
        if (req == null) throw new IllegalArgumentException("방 생성 요청 바디가 필요합니다.");
        if (req.getPlayerCount() == null || req.getTimeLimit() == null
                || req.getPoliceCount() == null || req.getThiefCount() == null
                || req.getPrison() == null || req.getPolygon() == null) {
            throw new IllegalArgumentException("방 생성에 필요한 세팅 값이 누락되었습니다.");
        }

        Double prisonLat = req.getPrison().getLat();
        Double prisonLng = req.getPrison().getLng();
        if (prisonLat == null || prisonLng == null) {
            throw new IllegalArgumentException("감옥 좌표(prison.lat/lng)가 필요합니다.");
        }

        if (!req.getPlayerCount().equals(req.getPoliceCount() + req.getThiefCount())) {
            throw new IllegalArgumentException("playerCount는 policeCount + thiefCount와 같아야 합니다.");
        }

        Geometry boundary = toPolygon(req.getPolygon());

        Member hostRef = memberRepository.getReferenceById(hostMemberId);
        String roomCode = gameRoomCodeGenerator.generateUniqueCode();

        Game game = Game.createWaitingRoom(hostRef, roomCode);
        gameRepository.save(game);

        GameSetting setting = GameSetting.create(
                game,
                req.getTimeLimit(),
                req.getPlayerCount(),
                req.getPoliceCount(),
                req.getThiefCount(),
                boundary,
                prisonLat,
                prisonLng
        );
        gameSettingRepository.save(setting);

        GameMember hostMember = GameMember.join(game, hostRef);
        gameMemberRepository.save(hostMember);

        return new GameRoomCreateResponse(game.getId(), game.getRoomCode(), GameStatus.WAITING);
    }


    @Override
    public GameStartResponse start(Long actorMemberId, Long roomId) {

        Game game = gameRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (!game.isHost(actorMemberId)) {
            throw new BusinessException(ErrorCode.ROOM_NOT_HOST);
        }

        if (!game.isWaiting()) {
            throw new BusinessException(ErrorCode.ROOM_ALREADY_STARTED);
        }

        GameSetting setting = gameSettingRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));

        long joined = gameMemberRepository.countByGameIdAndIsDeletedFalse(roomId);
        if (!joinedEqualsSetting(joined, setting.getPlayerCount())) {
            // 정원 미충족/초과는 지금 코드 체계상 ROOM_NOT_READY로 묶는게 가장 자연스러움
            throw new BusinessException(ErrorCode.ROOM_NOT_READY);
        }

        long notReady = gameMemberRepository.countByGameIdAndIsDeletedFalseAndReadyFalse(roomId);
        if (notReady > 0) {
            throw new BusinessException(ErrorCode.ROOM_NOT_READY);
        }

        // active 멤버 로드(닉네임 응답용 memberProfile까지 fetch)
        List<GameMember> members = gameMemberRepository.findAllActiveByGameIdWithMember(roomId);

        // 포지션 확정(PreferPosition 반영)
        assignPositions(setting, members);

        // 게임 시작
        game.start();

        return GameStartResponse.from(game, members);
    }

    private boolean joinedEqualsSetting(long joined, Integer playerCount) {
        if (playerCount == null) return false;
        return joined == playerCount.longValue();
    }



    private Polygon toPolygon(List<GameRoomCreateRequest.LatLng> polygon) {
        if (polygon == null || polygon.size() < 3) {
            throw new IllegalArgumentException("polygon은 최소 3개 좌표가 필요합니다.");
        }

        Coordinate[] raw = new Coordinate[polygon.size()];
        for (int i = 0; i < polygon.size(); i++) {
            GameRoomCreateRequest.LatLng p = polygon.get(i);
            if (p == null || p.getLat() == null || p.getLng() == null) {
                throw new IllegalArgumentException("polygon 좌표에 null이 포함되어 있습니다.");
            }
            raw[i] = new Coordinate(p.getLng(), p.getLat()); // x=lng, y=lat
        }

        Coordinate first = raw[0];
        Coordinate last = raw[raw.length - 1];
        boolean alreadyClosed = first.equals2D(last);

        Coordinate[] coords;
        if (alreadyClosed) {
            coords = raw;
        } else {
            coords = new Coordinate[raw.length + 1];
            System.arraycopy(raw, 0, coords, 0, raw.length);
            coords[raw.length] = new Coordinate(first.x, first.y);
        }

        if (coords.length < 4) throw new IllegalArgumentException("polygon 좌표가 올바르지 않습니다.");

        LinearRing shell = GF.createLinearRing(coords);
        Polygon poly = GF.createPolygon(shell);

        if (!poly.isValid()) throw new IllegalArgumentException("polygon 형태가 유효하지 않습니다.");

        return poly;
    }

    private void assignPositions(GameSetting setting, List<GameMember> members) {
        int thiefNeed = setting.getThiefCount();
        int policeNeed = setting.getPoliceCount();

        if (thiefNeed + policeNeed != members.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        // 1) 선호로 후보군 나누기
        List<GameMember> preferThief = members.stream()
                .filter(gm -> gm.getPreferPosition() == PreferPosition.THIEF)
                .toList();

        List<GameMember> preferAny = members.stream()
                .filter(gm -> gm.getPreferPosition() == PreferPosition.ANY)
                .toList();

        List<GameMember> preferPolice = members.stream()
                .filter(gm -> gm.getPreferPosition() == PreferPosition.POLICE)
                .toList();

        // 2) thiefNeed 만큼 도둑 선정: THIEF 선호 → ANY → POLICE(강제 전환)
        java.util.LinkedHashSet<GameMember> thieves = new java.util.LinkedHashSet<>();

        for (GameMember gm : preferThief) {
            if (thieves.size() >= thiefNeed) break;
            thieves.add(gm);
        }
        for (GameMember gm : preferAny) {
            if (thieves.size() >= thiefNeed) break;
            thieves.add(gm);
        }
        for (GameMember gm : preferPolice) {
            if (thieves.size() >= thiefNeed) break;
            thieves.add(gm);
        }

        if (thieves.size() != thiefNeed) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        // 3) 확정 배정
        for (GameMember gm : members) {
            if (thieves.contains(gm)) {
                gm.assignPosition(Position.THIEF);
            } else {
                gm.assignPosition(Position.POLICE);
            }
        }
    }


}
