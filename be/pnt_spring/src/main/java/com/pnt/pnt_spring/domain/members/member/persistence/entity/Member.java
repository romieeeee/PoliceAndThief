package com.pnt.pnt_spring.domain.members.member.persistence.entity;

import com.pnt.pnt_spring.domain.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", length = 20, nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(length = 50, unique = true)
    private String email;

    private LocalDate birth;

    @Column(length = 10)
    private String status;
}