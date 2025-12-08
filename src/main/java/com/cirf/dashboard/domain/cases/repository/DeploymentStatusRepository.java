package com.cirf.dashboard.domain.cases.repository;

import com.cirf.dashboard.domain.cases.entity.DeploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeploymentStatusRepository extends JpaRepository<DeploymentStatus, Long> {

    Optional<DeploymentStatus> findByCaseIdAndAccountId(Long caseId, String accountId);

    boolean existsByCaseIdAndAccountId(Long caseId, String accountId);
}