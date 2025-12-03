package com.cirf.dashboard.domain.analysis.repository;

import com.cirf.dashboard.domain.analysis.dto.request.AwsNativeLogQueryRequest;
import com.cirf.dashboard.domain.analysis.entity.LogEvent;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface LogEventRepositoryCustom {

    Page<LogEvent> searchByQuery(String tenantId, AwsNativeLogQueryRequest request);

    Optional<LogEvent> findByIdWithTenant(String tenantId, String caseId, String id);
}
