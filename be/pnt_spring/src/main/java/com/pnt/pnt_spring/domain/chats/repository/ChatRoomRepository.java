package com.pnt.pnt_spring.domain.chats.repository;

import com.pnt.pnt_spring.domain.chats.entity.ChatRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByIdAndIsDeletedFalse(Long id);

    List<ChatRoom> findAllByIsDeletedFalse();

    List<ChatRoom> findAllByRegionCodeAndIsDeletedFalse(Integer regionCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select cr
        from ChatRoom cr
        where cr.id = :id
        and cr.isDeleted = false
        """)
    Optional<ChatRoom> findByIdForUpdate(@Param("id") Long id);

    List<ChatRoom> findAllByIsDeletedFalseAndTitleContainingIgnoreCase(String title);

    List<ChatRoom> findAllByRegionCodeAndIsDeletedFalseAndTitleContainingIgnoreCase(Integer regionCode, String title);

}
