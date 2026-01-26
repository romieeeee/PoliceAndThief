package com.pnt.pnt_spring.domain.members.report.entity;

import com.pnt.pnt_spring.domain.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Table(name = "reports")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reporterId;

    @Column(nullable = false)
    private String reportedNickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Builder.Default // 빌더 사용 시 초기값 유지
    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.RECEIVED;

}