package com.pnt.pnt_spring.domain.chats.repository;

import com.pnt.pnt_spring.domain.chats.entity.MemberChatRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberChatRoomRepository extends JpaRepository<MemberChatRoom, Long> {

    /**
     * (기본) 멤버십 조회
     * - join에서 "있으면 복구(isDeleted=false) / 없으면 생성" 할 때 사용
     */
    Optional<MemberChatRoom> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    /**
     * (기본) 현재 참여중인지 체크 (is_deleted=false)
     * - currentMembers++ 중복 방지
     */
    boolean existsByChatRoomIdAndMemberIdAndIsDeletedFalse(Long chatRoomId, Long memberId);

    /**
     * (목록) 특정 유저가 참여중인 방 목록 뽑을 때 유용
     */
    List<MemberChatRoom> findAllByMemberIdAndIsDeletedFalse(Long memberId);

    /**
     * (목록) 특정 방의 참여자 목록 뽑을 때 유용
     */
    List<MemberChatRoom> findAllByChatRoomIdAndIsDeletedFalse(Long chatRoomId);

    /**
     * (옵션) current_members를 DB값 대신 count로 맞추고 싶을 때(동기화/검증용)
     */
//    long countByChatRoomIdAndIsDeletedFalse(Long chatRoomId);

}
