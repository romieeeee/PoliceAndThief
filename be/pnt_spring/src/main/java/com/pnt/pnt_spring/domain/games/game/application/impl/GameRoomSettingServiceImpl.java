package com.pnt.pnt_spring.domain.games.game.application.impl;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomSettingUpdateRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomSettingUpdateResponse;
import com.pnt.pnt_spring.domain.games.game.application.GameRoomSettingService;
import com.pnt.pnt_spring.domain.games.game.entity.Game;
import com.pnt.pnt_spring.domain.games.game.entity.GameSetting;
import com.pnt.pnt_spring.domain.games.game.enums.GameStatus;
import com.pnt.pnt_spring.domain.games.game.repository.GameRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameSettingRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GameRoomSettingServiceImpl implements GameRoomSettingService {

    private final GameRepository gameRepository;
    private final GameSettingRepository gameSettingRepository;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    public GameRoomSettingUpdateResponse updateSettings(Long actorMemberId, Long roomId, GameRoomSettingUpdateRequest req) {

        Game game = gameRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("대기방에서만 설정을 변경할 수 있습니다.");
        }

        Long hostId = game.getHost().getId();
        if (!hostId.equals(actorMemberId)) {
            throw new IllegalStateException("호스트만 게임 설정을 변경할 수 있습니다.");
        }

        if (!req.getPlayerCount().equals(req.getPoliceCount() + req.getThiefCount())) {
            throw new IllegalArgumentException("playerCount는 policeCount + thiefCount와 같아야 합니다.");
        }

        Geometry boundary = toPolygonForUpdate(req.getPolygon());

        Double prisonLat = req.getPrison().getLat();
        Double prisonLng = req.getPrison().getLng();
        if (prisonLat == null || prisonLng == null) {
            throw new IllegalArgumentException("감옥 좌표(prison.lat/lng)가 필요합니다.");
        }

        GameSetting setting = gameSettingRepository.findByGameIdForUpdate(roomId)
                .orElseThrow(() -> new IllegalArgumentException("게임 설정이 존재하지 않습니다."));

        setting.updateSetting(
                req.getTimeLimit(),
                req.getPlayerCount(),
                req.getPoliceCount(),
                req.getThiefCount()
        );

        setting.updateMap(boundary, prisonLat, prisonLng);

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
            raw[i] = new Coordinate(p.getLng(), p.getLat());
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
