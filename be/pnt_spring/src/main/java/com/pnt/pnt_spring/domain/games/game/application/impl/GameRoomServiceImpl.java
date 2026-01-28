package com.pnt.pnt_spring.domain.games.game.application.impl;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomCreateRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomCreateResponse;
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
}
