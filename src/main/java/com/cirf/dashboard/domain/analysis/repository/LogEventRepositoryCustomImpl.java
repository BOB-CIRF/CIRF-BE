package com.cirf.dashboard.domain.analysis.repository;

import com.cirf.dashboard.domain.analysis.dto.request.LogQueryRequest;
import com.cirf.dashboard.domain.analysis.entity.LogEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LogEventRepositoryCustomImpl implements LogEventRepositoryCustom {

    private final ElasticsearchOperations operations;

    private IndexCoordinates dsOfTenant(String tenantId) {
        return IndexCoordinates.of("logs-tenant-" + tenantId + "-default");
    }

    @Override
    public Optional<Instant> findLatestEventInstant(String tenantId, String caseId) {
        Criteria criteria = new Criteria("tenantId").is(tenantId)
                .and(new Criteria("caseId").is(caseId));

        CriteriaQuery q = new CriteriaQuery(criteria);
        q.addSort(Sort.by(Sort.Order.desc("timestamp")));
        q.setPageable(PageRequest.of(0, 1));

        SearchHits<LogEvent> hits = operations.search(q, LogEvent.class, dsOfTenant(tenantId));
        return hits.getSearchHits().stream()
                .findFirst()
                .map(h -> h.getContent().getTimestamp()); // LogEvent#getTimestamp()가 Instant라고 가정
    }

    @Override
    public Page<LogEvent> search(
            String tenantId,
            String caseId,
            Instant from,
            Instant to,
            String activity,
            Pageable pageable
    ) {
        // 상한 미포함(< to)로 경계 중복 방지
        Criteria criteria = new Criteria("tenantId").is(tenantId)
                .and(new Criteria("caseId").is(caseId))
                .and(new Criteria("timestamp").greaterThanEqual(from))
                .and(new Criteria("timestamp").lessThan(to));

        if (activity != null && !activity.isBlank()) {
            criteria = criteria.and("activity").is(activity);
        }

        CriteriaQuery q = new CriteriaQuery(criteria);
        if (pageable.getSort().isUnsorted()) {
            q.addSort(Sort.by(Sort.Order.desc("timestamp")));
        } else {
            q.addSort(pageable.getSort());
        }
        q.setPageable(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
        q.setTrackTotalHits(true);

        SearchHits<LogEvent> hits = operations.search(q, LogEvent.class, dsOfTenant(tenantId));

        List<LogEvent> content = hits.getSearchHits().stream()
                .map(h -> {
                    LogEvent e = h.getContent();
                    if (e.getId() == null) e.setId(h.getId()); // ES _id 매핑
                    return e;
                })
                .toList();

        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }

    public Page<LogEvent> searchByQuery(String tenantId, LogQueryRequest request) {
        // 필수 조건: tenantId, caseId
        Criteria criteria = new Criteria("tenantId").is(tenantId)
                .and(new Criteria("caseId").is(request.caseId().toString()));

        // 선택적 조건: accountId
        if (request.accountId() != null && !request.accountId().isBlank()) {
            criteria = criteria.and(new Criteria("accountId").is(request.accountId()));
        }

        // 선택적 조건: region
        if (request.region() != null && !request.region().isBlank()) {
            criteria = criteria.and(new Criteria("region").is(request.region()));
        }

        // 선택적 조건: logType (LogEvent의 type 필드에 매핑)
        if (request.logType() != null && !request.logType().isBlank()) {
            criteria = criteria.and(new Criteria("type").is(request.logType()));
        }

        // 선택적 조건: activity
        if (request.activity() != null && !request.activity().isBlank()) {
            criteria = criteria.and(new Criteria("activity").is(request.activity()));
        }

        // 선택적 조건: outcome
        if (request.outcome() != null && !request.outcome().isBlank()) {
            criteria = criteria.and(new Criteria("outcome").is(request.outcome()));
        }

        // 선택적 조건: 시간 범위 (startTime ~ endTime)
        if (request.startTime() != null) {
            Instant startInstant = request.startTime().atZone(ZoneId.systemDefault()).toInstant();
            criteria = criteria.and(new Criteria("timestamp").greaterThanEqual(startInstant));
        }

        if (request.endTime() != null) {
            Instant endInstant = request.endTime().atZone(ZoneId.systemDefault()).toInstant();
            criteria = criteria.and(new Criteria("timestamp").lessThan(endInstant));
        }

        // 쿼리 생성
        CriteriaQuery query = new CriteriaQuery(criteria);

        // 기본 정렬: timestamp 내림차순
        query.addSort(Sort.by(Sort.Order.desc("timestamp")));

        // 페이징 설정
        Pageable pageable = PageRequest.of(request.pageNumber(), request.pageSize());
        query.setPageable(pageable);
        query.setTrackTotalHits(true);

        // 라우팅 설정: {tenantId}|{caseId} 형식
        String routing = tenantId + "|" + request.caseId();
        query.setRoute(routing);

        // 검색 실행
        SearchHits<LogEvent> hits = operations.search(query, LogEvent.class, dsOfTenant(tenantId));

        // 결과 매핑
        List<LogEvent> content = hits.getSearchHits().stream()
                .map(h -> {
                    LogEvent e = h.getContent();
                    if (e.getId() == null) e.setId(h.getId()); // ES _id 매핑
                    return e;
                })
                .toList();

        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }

    @Override
    public Optional<LogEvent> findByIdWithTenant(String tenantId, String caseId, String id) {
        try {
            // 라우팅 설정: {tenantId}|{caseId} 형식
            String routing = tenantId + "|" + caseId;

            // Criteria로 검색 (get 대신 search 사용하여 라우팅 적용)
            Criteria criteria = new Criteria("_id").is(id)
                    .and(new Criteria("tenantId").is(tenantId))
                    .and(new Criteria("caseId").is(caseId));

            CriteriaQuery query = new CriteriaQuery(criteria);
            query.setRoute(routing);

            SearchHits<LogEvent> hits = operations.search(query, LogEvent.class, dsOfTenant(tenantId));

            if (hits.hasSearchHits()) {
                LogEvent event = hits.getSearchHit(0).getContent();
                if (event.getId() == null) {
                    event.setId(hits.getSearchHit(0).getId());
                }
                return Optional.of(event);
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
