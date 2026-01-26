package com.pnt.pnt_spring.domain.members.report.repository;

import com.pnt.pnt_spring.domain.members.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}