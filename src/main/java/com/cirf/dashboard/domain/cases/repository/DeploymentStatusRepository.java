package com.cirf.dashboard.domain.cases.repository;

import com.cirf.dashboard.domain.cases.entity.DeploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeploymentStatusRepository extends JpaRepository<DeploymentStatus, Long> {

    Optional<DeploymentStatus> findByCaseIdAndAccountId(Long caseId, String accountId);

    // 가장 최근 생성된 DeploymentStatus 조회
    Optional<DeploymentStatus> findFirstByCaseIdAndAccountIdOrderByCreatedAtDesc(Long caseId, String accountId);

    boolean existsByCaseIdAndAccountId(Long caseId, String accountId);
}