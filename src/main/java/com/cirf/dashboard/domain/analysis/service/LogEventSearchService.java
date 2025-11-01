package com.cirf.dashboard.domain.analysis.service;

import com.cirf.dashboard.domain.analysis.entity.LogEvent;
import com.cirf.dashboard.domain.analysis.repository.LogEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogEventSearchService {

    private final LogEventRepository logEventRepository;

    public Page<LogEvent> search(
            String tenantId,
            String caseId,
            Instant from,
            Instant to,
            String activity,
            Pageable pageable
    ) {
        // 기본값 결정/권한 검증/로깅 등은 여기서
        Instant f = (from != null) ? from : Instant.now().minus(Duration.ofDays(30));
        Instant t = (to   != null) ? to   : Instant.now();

        return logEventRepository.search(tenantId, caseId, f, t, activity, pageable);
    }
}

