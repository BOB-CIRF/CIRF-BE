package com.cirf.dashboard.domain.analysis.repository;

import com.cirf.dashboard.domain.analysis.dto.request.LogQueryRequest;
import com.cirf.dashboard.domain.analysis.entity.LogEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

public interface LogEventRepositoryCustom {
    Optional<Instant> findLatestEventInstant(String tenantId, String caseId);

    Page<LogEvent> search(
            String tenantId,
            String caseId,
            Instant from,
            Instant to,
            String activity,
            Pageable pageable
    );

    Page<LogEvent> searchByQuery(String tenantId, LogQueryRequest request);

    Optional<LogEvent> findByIdWithTenant(String tenantId, String caseId, String id);
}
