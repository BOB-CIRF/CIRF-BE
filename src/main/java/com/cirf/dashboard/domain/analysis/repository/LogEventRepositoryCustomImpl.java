package com.cirf.dashboard.domain.analysis.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.cirf.dashboard.domain.analysis.dto.request.AwsNativeLogQueryRequest;
import com.cirf.dashboard.domain.analysis.entity.LogEvent;
import com.cirf.dashboard.domain.analysis.exception.ElasticsearchCommunicationException;
import com.cirf.dashboard.domain.analysis.exception.ErrorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Repository;
import co.elastic.clients.elasticsearch.core.SearchResponse;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LogEventRepositoryCustomImpl implements LogEventRepositoryCustom {

    private final ElasticsearchOperations operations;
    private final ElasticsearchClient esClient;

    private IndexCoordinates dsOfTenant(String tenantId, String caseId) {
        return IndexCoordinates.of("logs-tenant-" + tenantId + "-" + caseId + "-default");
    }

    public Page<LogEvent> searchByQuery(String tenantId, AwsNativeLogQueryRequest request) {
        String indexName = "logs-tenant-" + tenantId + "-" + request.caseId() + "-default";
        String routing = tenantId + "|" + request.caseId();

        try {
            // Bool Query 구성
            co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder boolBuilder =
                    new co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder();

            // 필터 조건 추가
            addFilterConditions(boolBuilder, tenantId, request);

            // 키워드 검색 추가
            addKeywordSearch(boolBuilder, request);

            // 검색 실행
            boolean hasKeyword = request.keyword() != null && !request.keyword().isBlank();
            SearchResponse<LogEvent> response = esClient.search(s -> s
                            .index(indexName)
                            .query(q -> q.bool(boolBuilder.build()))
                            .sort(determineSortOption(request))
                            .from(request.pageNumber() * request.pageSize())
                            .size(request.pageSize())
                            .routing(routing)
                            .trackTotalHits(t -> t.enabled(true))
                            .trackScores(hasKeyword),
                    LogEvent.class
            );

            return mapSearchResponse(response, request);

        } catch (IOException e) {
            log.error("Elasticsearch search failed - Index: {}, Routing: {}", indexName, routing, e);
            throw new ElasticsearchCommunicationException(ErrorMessage.ELASTICSEARCH_COMMUNICATION_ERROR);
        }
    }

    private void addFilterConditions(co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder boolBuilder,
                                     String tenantId,
                                     AwsNativeLogQueryRequest request) {
        // 필수: tenantId, caseId
        boolBuilder.filter(f -> f.term(t -> t.field("tenantId").value(tenantId)));
        boolBuilder.filter(f -> f.term(t -> t.field("caseId").value(request.caseId().toString())));

        // 선택: accountId, region, logType, activity, outcome
        if (request.accountId() != null && !request.accountId().isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t.field("accountId").value(request.accountId())));
        }
        if (request.region() != null && !request.region().isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t.field("region").value(request.region())));
        }
        if (request.logType() != null && !request.logType().isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t.field("type").value(request.logType())));
        }
        if (request.activity() != null && !request.activity().isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t.field("activity").value(request.activity())));
        }
        if (request.outcome() != null && !request.outcome().isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t.field("outcome").value(request.outcome())));
        }

        // 시간 범위
        addTimeRangeFilter(boolBuilder, request);
    }

    private void addTimeRangeFilter(co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder boolBuilder,
                                     AwsNativeLogQueryRequest request) {
        if (request.startTime() != null && request.endTime() != null) {
            Instant start = request.startTime().atZone(ZoneId.systemDefault()).toInstant();
            Instant end = request.endTime().atZone(ZoneId.systemDefault()).toInstant();
            boolBuilder.filter(f -> f.range(r -> r.date(d -> d
                    .field("@timestamp").gte(start.toString()).lt(end.toString())
            )));
        } else if (request.startTime() != null) {
            Instant start = request.startTime().atZone(ZoneId.systemDefault()).toInstant();
            boolBuilder.filter(f -> f.range(r -> r.date(d -> d
                    .field("@timestamp").gte(start.toString())
            )));
        } else if (request.endTime() != null) {
            Instant end = request.endTime().atZone(ZoneId.systemDefault()).toInstant();
            boolBuilder.filter(f -> f.range(r -> r.date(d -> d
                    .field("@timestamp").lt(end.toString())
            )));
        }
    }

    private void addKeywordSearch(co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder boolBuilder,
                                   AwsNativeLogQueryRequest request) {
        if (request.keyword() == null || request.keyword().isBlank()) {
            return;
        }

        String keyword = request.keyword();
        String wildcardPattern = "*" + keyword.toLowerCase(Locale.ROOT) + "*";
        log.debug("Searching with keyword: {}, wildcard: {}", keyword, wildcardPattern);

        // 검색 대상 필드 목록
        List<String> searchFields = List.of(
                "activity.text", "dst.text", "src.text", "type.text",
                "actorAccountId.text", "target.text", "actor.text",
                "accountId.text", "outcome.text"
        );

        boolBuilder.must(m -> m.bool(b -> b
                // 1. Multi-match: Best Fields (퍼지 검색) - 메인 검색
                .should(s -> s.multiMatch(mm -> mm
                        .query(keyword)
                        .fields(searchFields)
                        .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                        .fuzziness("AUTO")
                ))
                // 2. Wildcard: 부분 문자열 (핵심 필드만, 대소문자 무시)
                // activity: 가장 많이 검색되는 필드 (예: ConsoleLogin, CreateBucket)
                .should(s -> s.wildcard(w -> w.field("activity").value(wildcardPattern).caseInsensitive(true)))
                // actor: 사용자/역할 검색
                .should(s -> s.wildcard(w -> w.field("actor").value(wildcardPattern).caseInsensitive(true)))
                // target: 리소스 검색
                .should(s -> s.wildcard(w -> w.field("target").value(wildcardPattern).caseInsensitive(true)))
                .minimumShouldMatch("1")
        ));
    }

    private co.elastic.clients.elasticsearch._types.SortOptions determineSortOption(AwsNativeLogQueryRequest request) {
        if (request.keyword() != null && !request.keyword().isBlank()) {
            return co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .score(sc -> sc.order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))
            );
        }
        return co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                .field(f -> f.field("@timestamp").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))
        );
    }

    private Page<LogEvent> mapSearchResponse(SearchResponse<LogEvent> response,
                                              AwsNativeLogQueryRequest request) {
        List<LogEvent> content = response.hits().hits().stream()
                .map(hit -> {
                    LogEvent event = hit.source();
                    if (event != null && event.getId() == null) {
                        event.setId(hit.id());
                    }
                    return event;
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        Pageable pageable = PageRequest.of(request.pageNumber(), request.pageSize());
        long total = response.hits().total() != null ? response.hits().total().value() : 0;

        return new PageImpl<>(content, pageable, total);
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

            SearchHits<LogEvent> hits = operations.search(query, LogEvent.class, dsOfTenant(tenantId, caseId));

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
