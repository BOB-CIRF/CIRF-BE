package com.cirf.dashboard.domain.analysis.repository.awsNativeLog;

import com.cirf.dashboard.domain.analysis.dto.SliceWithSort;
import com.cirf.dashboard.domain.analysis.dto.request.AwsNativeLogQueryRequest;
import com.cirf.dashboard.domain.analysis.entity.LogEvent;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface LogEventRepositoryCustom {

    Optional<LogEvent> findByIdWithTenant(String tenantId, String caseId, String id);

    SliceWithSort<LogEvent> queryLogEvents(String tenantId, AwsNativeLogQueryRequest request);
}
