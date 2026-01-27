package com.pnt.pnt_spring.domain.games.game.repository;

import com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomMemberItem;
import com.pnt.pnt_spring.domain.games.game.entity.GameMember;
import com.pnt.pnt_spring.domain.games.game.enums.Position;
import com.pnt.pnt_spring.domain.games.game.enums.PreferPosition;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameMemberRepository extends JpaRepository<GameMember, Long> {

    @Query("""
        select new com.pnt.pnt_spring.domain.games.game.api.resp.GameRoomMemberItem(
            m.id,
            coalesce(mp.nickname, m.loginId),
            case
                when gm.givenPosition is null then 'UNDECIDED'
                else concat('', gm.givenPosition)
            end,
            case
                when g.host.id = m.id then true
                else false
            end,
            coalesce(gm.ready, false),
            mp.avatarUrl
        )
        from GameMember gm
        join gm.game g
        join gm.member m
        left join m.memberProfile mp
        where g.id = :gameId
        order by gm.id asc
    """)
    List<GameRoomMemberItem> findRoomMemberItems(@Param("gameId") Long gameId);

    boolean existsByGameIdAndMemberId(Long gameId, Long memberId);

    Optional<GameMember> findByGameIdAndMemberId(Long gameId, Long memberId);

    List<GameMember> findAllByGameId(Long gameId);

    long countByGameId(Long gameId);

    long countByGameIdAndReadyTrue(Long gameId);

    // "전원 ready" 체크용: ready=false인 사람 수가 0인지로 판단하기 좋음
    long countByGameIdAndReadyFalse(Long gameId);

    // 특정 포지션 인원 체크 (배정 포지션 POLICE/THIEF 카운트)
    long countByGameIdAndGivenPosition(Long gameId, Position givenPosition);

    // (선택) 선호 포지션 카운트가 필요하면 유용 (ANY 포함)
    long countByGameIdAndPreferPosition(Long gameId, PreferPosition preferPosition);

    // ready / preferPosition update 시 경합 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select gm
        from GameMember gm
        where gm.game.id = :gameId
          and gm.member.id = :memberId
    """)
    Optional<GameMember> findByGameIdAndMemberIdForUpdate(@Param("gameId") Long gameId,
                                                          @Param("memberId") Long memberId);

    // 멤버 목록 조회 최적화 (N+1 방지)
    @Query("""
        select gm
        from GameMember gm
        join fetch gm.member m
        where gm.game.id = :gameId
        order by gm.id asc
    """)
    List<GameMember> findAllByGameIdWithMember(@Param("gameId") Long gameId);
}
