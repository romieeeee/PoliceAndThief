package com.pnt.pnt_spring.domain.games.game.application.impl;

import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomSettingUpdateRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomSettingGetResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomSettingUpdateResponse;
import com.pnt.pnt_spring.domain.games.game.application.GameRoomSettingService;
import com.pnt.pnt_spring.domain.games.game.entity.Game;
import com.pnt.pnt_spring.domain.games.game.entity.GameSetting;
import com.pnt.pnt_spring.domain.games.game.enums.GameStatus;
import com.pnt.pnt_spring.domain.games.game.repository.GameMemberRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameSettingRepository;
import com.pnt.pnt_spring.global.api.code.ErrorCode;
import com.pnt.pnt_spring.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class GameRoomSettingServiceImpl implements GameRoomSettingService {

	private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);
	private final GameRepository gameRepository;
	private final GameSettingRepository gameSettingRepository;
	private final GameMemberRepository gameMemberRepository;

	//게임방 설정 조회 (기본값 불러오기)
	@Override
	@Transactional(readOnly = true)
	public GameRoomSettingGetResponse getSettings(Long actorMemberId, Long roomId) {

		Game game = gameRepository.findByIdAndIsDeletedFalse(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

		GameSetting setting = gameSettingRepository.findByGameIdAndIsDeletedFalse(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));

		return new GameRoomSettingGetResponse(
			roomId,
			game.getStatus().name(),
			setting.getTimeLimit(),
			setting.getPlayerCount(),
			setting.getPoliceCount(),
			setting.getThiefCount(),
			setting.getCctvInterval(),
			setting.getPrisonLat(),
			setting.getPrisonLng()
		);
	}

	// 게임방 설정 변경
	@Override
	public GameRoomSettingUpdateResponse updateSettings(Long actorMemberId, Long roomId,
		GameRoomSettingUpdateRequest req) {

		Game game = gameRepository.findByIdForUpdate(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

		// 대기방만 가능
		if (game.getStatus() != GameStatus.WAITING) {
			throw new BusinessException(ErrorCode.ROOM_ALREADY_STARTED);
		}

		// 호스트만 가능
		if (!game.getHost().getId().equals(actorMemberId)) {
			throw new BusinessException(ErrorCode.ROOM_NOT_HOST);
		}

		// playerCount = police + thief 검증
		if (!req.getPlayerCount().equals(req.getPoliceCount() + req.getThiefCount())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR);
		}

		// 현재 인원보다 playerCount 줄이기 방지
		long current = gameMemberRepository.countByGameIdAndIsDeletedFalse(roomId);
		if (req.getPlayerCount() < current) {
			throw new BusinessException(ErrorCode.ROOM_FULL); // or INVALID_REQUEST
		}

		// 좌표 변환
		Geometry boundary = toPolygon(req.getPolygon());

		Double prisonLat = req.getPrison().getLat();
		Double prisonLng = req.getPrison().getLng();
		if (prisonLat == null || prisonLng == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR);
		}

		GameSetting setting = gameSettingRepository.findByGameIdForUpdate(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));

		setting.updateSetting(
			req.getTimeLimit(),
			req.getPlayerCount(),
			req.getPoliceCount(),
			req.getThiefCount(),
			req.getCctvInterval()
		);

		setting.updateMap(boundary, prisonLat, prisonLng);

		return new GameRoomSettingUpdateResponse(
			roomId,
			game.getStatus().name(),
			setting.getTimeLimit(),
			setting.getPlayerCount(),
			setting.getPoliceCount(),
			setting.getThiefCount(),
			setting.getCctvInterval(),
			setting.getPrisonLat(),
			setting.getPrisonLng()
		);
	}

	// 게임방 설정 변경
	private Polygon toPolygon(List<GameRoomSettingUpdateRequest.LatLng> polygon) {
		if (polygon == null || polygon.size() < 3) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR);
		}

		Coordinate[] raw = new Coordinate[polygon.size()];
		for (int i = 0; i < polygon.size(); i++) {
			var p = polygon.get(i);
			if (p == null || p.getLat() == null || p.getLng() == null) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR);
			}
			raw[i] = new Coordinate(p.getLng(), p.getLat());
		}

		Coordinate first = raw[0];
		Coordinate last = raw[raw.length - 1];

		Coordinate[] coords;
		if (first.equals2D(last)) {
			coords = raw;
		} else {
			coords = new Coordinate[raw.length + 1];
			System.arraycopy(raw, 0, coords, 0, raw.length);
			coords[raw.length] = new Coordinate(first.x, first.y);
		}

		LinearRing shell = GF.createLinearRing(coords);
		Polygon poly = GF.createPolygon(shell);

		if (!poly.isValid()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR);
		}

		return poly;
	}
}
