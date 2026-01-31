package com.pnt.pnt_spring.domain.games.game.application.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomJoinRequest;
import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomPositionRequest;
import com.pnt.pnt_spring.domain.games.game.api.req.GameRoomReadyRequest;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomJoinResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomMemberItem;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomMemberListResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomPositionResponse;
import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomReadyResponse;
import com.pnt.pnt_spring.domain.games.game.application.GameRoomMemberService;
import com.pnt.pnt_spring.domain.games.game.entity.Game;
import com.pnt.pnt_spring.domain.games.game.entity.GameMember;
import com.pnt.pnt_spring.domain.games.game.entity.GameSetting;
import com.pnt.pnt_spring.domain.games.game.enums.PreferPosition;
import com.pnt.pnt_spring.domain.games.game.repository.GameMemberRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameRepository;
import com.pnt.pnt_spring.domain.games.game.repository.GameSettingRepository;
import com.pnt.pnt_spring.domain.members.member.entity.Member;
import com.pnt.pnt_spring.domain.members.member.repository.jpa.MemberRepository;
import com.pnt.pnt_spring.global.api.code.ErrorCode;
import com.pnt.pnt_spring.global.exception.BusinessException;
import com.pnt.pnt_spring.global.utils.S3Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class GameRoomMemberServiceImpl implements GameRoomMemberService {

	private final GameRepository gameRepository;
	private final GameMemberRepository gameMemberRepository;
	private final GameSettingRepository gameSettingRepository;
	private final MemberRepository memberRepository;
	private final S3Service s3Service;

	@Override
	public GameRoomJoinResponse joinRoom(Long memberId, GameRoomJoinRequest req) {
		if (req == null || req.getRoomCode() == null || req.getRoomCode().isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		String roomCode = req.getRoomCode().trim();

		Game game = gameRepository.findByRoomCodeForUpdate(roomCode)
			.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

		if (!game.isWaiting()) {
			throw new BusinessException(ErrorCode.ROOM_ALREADY_STARTED);
		}

		//  1) 내가 "이미 이 방에 참여 중"이면 멱등 처리(그대로 성공 응답)
		if (gameMemberRepository.existsByGameIdAndMemberIdAndIsDeletedFalse(game.getId(), memberId)) {
			GameMember gm = gameMemberRepository.findByGameIdAndMemberIdForUpdate(game.getId(), memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));

			return new GameRoomJoinResponse(
				game.getId(),
				game.getRoomCode(),
				memberId,
				gm.getPreferPosition(),
				gm.getGivenPosition(),
				gm.getReady()
			);
		}

		//  2) 내가 "다른 방에 참여 중"이면 차단
		if (gameMemberRepository.existsByMemberIdAndIsDeletedFalse(memberId)) {
			throw new BusinessException(ErrorCode.ROOM_ALREADY_JOINED);
		}

		GameSetting setting = gameSettingRepository.findById(game.getId())
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));

		long current = gameMemberRepository.countByGameIdAndIsDeletedFalse(game.getId());
		if (current >= setting.getPlayerCount()) {
			throw new BusinessException(ErrorCode.ROOM_FULL);
		}

		Member memberRef = memberRepository.getReferenceById(memberId);

		GameMember gm = gameMemberRepository.findByGameIdAndMemberIdForUpdate(game.getId(), memberId)
			.orElse(null);

		if (gm == null) {
			gm = GameMember.join(game, memberRef);
			gameMemberRepository.save(gm);
		} else {
			gm.rejoin(); // 이전에 들어왔다가 나갔던 기록이 있으면 복구
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
	public void leave(Long memberId, Long roomId) {
		Game game = gameRepository.findByIdForUpdate(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

		// 정책: 대기방에서만 나가기/강퇴 허용
		if (!game.isWaiting()) {
			throw new BusinessException(ErrorCode.ROOM_ALREADY_STARTED);
		}

		GameMember me = gameMemberRepository.findByGameIdAndMemberIdAndIsDeletedFalse(roomId, memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_JOINED));

		me.leave();
		gameMemberRepository.flush();

		long remain = gameMemberRepository.countByGameIdAndIsDeletedFalse(roomId);
		if (remain == 0) {
			game.close();
			return;
		}

		// 방장 나가면 위임
		if (game.isHost(memberId)) {
			GameMember nextHost = gameMemberRepository
				.findFirstByGameIdAndIsDeletedFalseOrderByCreatedAtAsc(roomId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));

			game.changeHost(nextHost.getMember());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public GameRoomMemberListResponse getRoomMembers(Long roomId) {
		gameRepository.findById(roomId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

		List<GameRoomMemberItem> rawItems = gameMemberRepository.findRoomMemberItems(roomId);

		List<GameRoomMemberItem> responseItems = rawItems.stream()
			.map(item -> {
				String storedKey = item.getAvatarUrl();
				String signedUrl = s3Service.getPresignedGetUrl(storedKey);
				return GameRoomMemberItem.builder()
					.memberId(item.getMemberId())
					.gameMemberId(item.getGameMemberId())
					.nickname(item.getNickname())
					.role(item.getRole())
					.isHost(item.isHost())
					.isReady(item.isReady())
					.avatarUrl(signedUrl)
					.build();
			})
			.toList();

		return new GameRoomMemberListResponse(roomId, responseItems);
	}

	@Override
	public GameRoomReadyResponse updateReady(Long roomId, Long memberId, GameRoomReadyRequest req) {
		if (req == null || req.getReady() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR);
		}

		Game game = gameRepository.findByIdForUpdate(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

		if (!game.isWaiting()) {
			throw new BusinessException(ErrorCode.ROOM_ALREADY_STARTED);
		}

		GameMember gm = gameMemberRepository.findByGameIdAndMemberIdForUpdate(roomId, memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_JOINED));

		gm.setReady(req.getReady());

		return new GameRoomReadyResponse(roomId, memberId, gm.getReady());
	}

	@Override
	public GameRoomPositionResponse pickPosition(Long actorMemberId, Long roomId, GameRoomPositionRequest req) {
		if (req == null)
			throw new IllegalArgumentException("position 요청 바디가 필요합니다.");
		PreferPosition prefer = req.getPreferPosition();
		if (prefer == null)
			throw new IllegalArgumentException("preferPosition은 필수입니다.");

		Game game = gameRepository.findByIdForUpdate(roomId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

		if (!game.isWaiting()) {
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

	@Override
	public void kick(Long actorId, Long roomId, Long targetMemberId, String reason) {
		if (targetMemberId == null) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		if (actorId.equals(targetMemberId)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		Game game = gameRepository.findByIdForUpdate(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

		if (!game.isWaiting()) {
			throw new BusinessException(ErrorCode.ROOM_ALREADY_STARTED);
		}

		if (!game.isHost(actorId)) {
			throw new BusinessException(ErrorCode.ROOM_NOT_HOST);
		}

		// 방장 강퇴 금지(정책)
		if (game.isHost(targetMemberId)) {
			throw new BusinessException(ErrorCode.ROOM_NOT_HOST); // 따로 코드 없으니 재사용 or INVALID_REQUEST
		}

		GameMember target = gameMemberRepository.findByGameIdAndMemberIdAndIsDeletedFalse(roomId, targetMemberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_JOINED));

		target.kick();
	}
}
