package com.pnt.pnt_spring.domain.games.game.application.impl;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomCreateRequest;
import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomPositionRequest;
import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomReadyRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.*;
import com.pnt.pnt_spring.domain.games.game.application.GameRoomCodeGenerator;
import com.pnt.pnt_spring.domain.games.game.application.GameRoomService;
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
import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomSettingUpdateRequest;
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
        if (req == null) {
            throw new IllegalArgumentException("방 생성 요청 바디가 필요합니다.");
        }
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
            throw new IllegalArgumentException(
                    "playerCount는 policeCount + thiefCount와 같아야 합니다."
            );
        }

        // DB 저장 전에 먼저 변환/검증
        Geometry boundary = toPolygon(req.getPolygon());

        Member hostRef = memberRepository.getReferenceById(hostMemberId);
        String roomCode = gameRoomCodeGenerator.generateUniqueCode();

        // 1) Game 생성/저장
        Game game = Game.createWaitingRoom(hostRef, roomCode);
        gameRepository.save(game);

        // 2) Setting 생성/저장
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

        // 3) Host 자동 참가
        GameMember hostMember = GameMember.join(game, hostRef);
        gameMemberRepository.save(hostMember);

        return new GameRoomCreateResponse(game.getId(), game.getRoomCode(), GameStatus.WAITING);
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

        gm.toggleReady(req.isReady());

        return new GameRoomReadyResponse(roomId, memberId, gm.getReady());
    }

    @Override
    public GameRoomSettingUpdateResponse updateSettings(Long actorMemberId, Long roomId, GameRoomSettingUpdateRequest req) {

        // 1) 방 락 + 존재 확인
        Game game = gameRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        // 2) 상태 체크
        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("대기방에서만 설정을 변경할 수 있습니다.");
        }

        // 3) 호스트 권한 체크 (필드명은 프로젝트에 맞게 조정)
        Long hostId = game.getHost().getId(); // 또는 game.getHostMemberId()
        if (!hostId.equals(actorMemberId)) {
            throw new IllegalStateException("호스트만 게임 설정을 변경할 수 있습니다.");
        }

        // 4) 도메인 검증: 인원 수 일관성
        if (!req.getPlayerCount().equals(req.getPoliceCount() + req.getThiefCount())) {
            throw new IllegalArgumentException("playerCount는 policeCount + thiefCount와 같아야 합니다.");
        }

        // 5) geometry 변환/검증 (DB 반영 전)
        Geometry boundary = toPolygonForUpdate(req.getPolygon());

        Double prisonLat = req.getPrison().getLat();
        Double prisonLng = req.getPrison().getLng();
        if (prisonLat == null || prisonLng == null) {
            throw new IllegalArgumentException("감옥 좌표(prison.lat/lng)가 필요합니다.");
        }

        // 6) Setting row 락 + 존재 확인
        GameSetting setting = gameSettingRepository.findByGameIdForUpdate(roomId)
                .orElseThrow(() -> new IllegalArgumentException("게임 설정이 존재하지 않습니다."));

        // 7) 업데이트 (엔티티 메서드에 맞춰 분리 호출)
        setting.updateSetting(
                req.getTimeLimit(),
                req.getPlayerCount(),
                req.getPoliceCount(),
                req.getThiefCount()
        );

        setting.updateMap(
                boundary,
                prisonLat,
                prisonLng
        );


        // 8) 응답
        return new GameRoomSettingUpdateResponse(
                roomId,
                game.getStatus().name(),
                setting.getTimeLimit(),
                setting.getPlayerCount(),
                setting.getPoliceCount(),
                setting.getThiefCount(),
                setting.getPrisonLat(),
                setting.getPrisonLng()
        );
    }

    @Override
    public GameRoomPositionResponse pickPosition(Long actorMemberId, Long roomId, GameRoomPositionRequest req) {

        if (req == null) {
            throw new IllegalArgumentException("position 요청 바디가 필요합니다.");
        }
        PreferPosition prefer = req.getPreferPosition();
        if (prefer == null) {
            throw new IllegalArgumentException("preferPosition은 필수입니다.");
        }

        // 1) 게임(방) 락 + 존재 확인
        Game game = gameRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        // 2) 상태 체크
        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("대기방에서만 포지션 픽을 변경할 수 있습니다.");
        }

        // 3) 참가자 row 락 + 존재 확인
        GameMember gm = gameMemberRepository.findByGameIdAndMemberIdForUpdate(roomId, actorMemberId)
                .orElseThrow(() -> new IllegalArgumentException("방에 참가한 멤버가 아닙니다."));

        // 4) 픽 저장 (givenPosition은 건드리지 않음)
        gm.pickPreferPosition(prefer);

        // 5) 응답
        return new GameRoomPositionResponse(
                roomId,
                actorMemberId,
                gm.getPreferPosition(),
                gm.getGivenPosition(), // 보통 null
                gm.getReady()
        );
    }


    /* =========================
       helpers
       ========================= */

    /**
     * - 최소 3개 좌표 필요
     * - (x=lng, y=lat) 변환
     * - 닫힘 처리: 이미 닫혀있으면 그대로, 아니면 첫 점을 마지막에 추가
     * - polygon 유효성 검사 (자기교차/퇴화 등)
     */
    private Polygon toPolygon(List<GameRoomCreateRequest.LatLng> polygon) {
        if (polygon == null || polygon.size() < 3) {
            throw new IllegalArgumentException("polygon은 최소 3개 좌표가 필요합니다.");
        }

        // 좌표 배열 준비: 닫혀있을 수도/아닐 수도 있으니 일단 입력 size만큼 채우고,
        // 필요하면 마지막에 1개 더 붙여서 닫는다.
        Coordinate[] raw = new Coordinate[polygon.size()];

        for (int i = 0; i < polygon.size(); i++) {
            GameRoomCreateRequest.LatLng p = polygon.get(i);

            if (p == null || p.getLat() == null || p.getLng() == null) {
                throw new IllegalArgumentException("polygon 좌표에 null이 포함되어 있습니다.");
            }

            // JTS: x=lng, y=lat
            raw[i] = new Coordinate(p.getLng(), p.getLat());
        }

        Coordinate first = raw[0];
        Coordinate last = raw[raw.length - 1];

        // 닫힘 여부 체크 (2D 비교)
        boolean alreadyClosed = first.equals2D(last);

        Coordinate[] coords;
        if (alreadyClosed) {
            coords = raw;
        } else {
            coords = new Coordinate[raw.length + 1];
            System.arraycopy(raw, 0, coords, 0, raw.length);
            coords[raw.length] = new Coordinate(first.x, first.y);
        }

        // LinearRing은 최소 4개 좌표 필요 (첫점=마지막점 포함)
        if (coords.length < 4) {
            throw new IllegalArgumentException("polygon 좌표가 올바르지 않습니다.");
        }

        LinearRing shell = GF.createLinearRing(coords);
        Polygon poly = GF.createPolygon(shell);

        // 유효성 검사 (자기교차/퇴화 등)
        if (!poly.isValid()) {
            throw new IllegalArgumentException("polygon 형태가 유효하지 않습니다.");
        }

        return poly;
    }

    /**
     * Update용 polygon 변환 (닫힘 처리 + isValid)
     * ※ Request DTO가 LatLng 타입이 다르니 오버로드 형태로 따로 둡니다.
     */
    private Polygon toPolygonForUpdate(List<GameRoomSettingUpdateRequest.LatLng> polygon) {
        if (polygon == null || polygon.size() < 3) {
            throw new IllegalArgumentException("polygon은 최소 3개 좌표가 필요합니다.");
        }

        Coordinate[] raw = new Coordinate[polygon.size()];
        for (int i = 0; i < polygon.size(); i++) {
            var p = polygon.get(i);
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

        if (coords.length < 4) {
            throw new IllegalArgumentException("polygon 좌표가 올바르지 않습니다.");
        }

        LinearRing shell = GF.createLinearRing(coords);
        Polygon poly = GF.createPolygon(shell);

        if (!poly.isValid()) {
            throw new IllegalArgumentException("polygon 형태가 유효하지 않습니다.");
        }
        return poly;
    }
}
