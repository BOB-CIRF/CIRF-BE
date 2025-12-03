package com.cirf.dashboard.domain.analysis.repository.ec2Log;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.cirf.dashboard.domain.analysis.dto.SliceWithSort;
import com.cirf.dashboard.domain.analysis.dto.request.Ec2LogQueryRequest;
import com.cirf.dashboard.domain.analysis.entity.Ec2LogEvent;
import com.cirf.dashboard.domain.analysis.entity.LogEvent;
import com.cirf.dashboard.domain.analysis.exception.ElasticsearchCommunicationException;
import com.cirf.dashboard.domain.analysis.exception.ErrorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class Ec2LogEventRepositoryCustomImpl implements Ec2LogEventRepositoryCustom {

    private final ElasticsearchOperations operations;
    private final ElasticsearchClient esClient;

    @Override
    public SliceWithSort<Ec2LogEvent> queryEc2LogEvents(String tenantId, Ec2LogQueryRequest request) {
        String indexName = "ec2-tenant-" + tenantId + "-" + request.caseId();
        String routing = tenantId + "-" + request.caseId();

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
            boolean isFirstPage = request.searchAfter() == null;
            SearchResponse<Ec2LogEvent> response = esClient.search(s -> {
                        var searchBuilder = s
                                .index(indexName)
                                .query(q -> q.bool(boolBuilder.build()))
                                .sort(determineSortOptions(request))  // 복합 정렬
                                .size(request.pageSize() + 1)  // hasNext 확인을 위해 +1
                                .routing(routing)
                                .trackScores(hasKeyword)
                                .trackTotalHits(t -> t.enabled(isFirstPage));  // 첫 페이지만 전체 개수 추적

                        // searchAfter가 있으면 적용
                        if (request.searchAfter() != null) {
                            searchBuilder.searchAfter(parseSearchAfter(request.searchAfter()));
                        }

                        return searchBuilder;
                    },
                    Ec2LogEvent.class
            );

            return mapSearchResponse(response, request);

        } catch (IOException e) {
            log.error("Elasticsearch search failed - Index: {}, Routing: {}", indexName, routing, e);
            throw new ElasticsearchCommunicationException(ErrorMessage.ELASTICSEARCH_COMMUNICATION_ERROR);
        }
    }

    private void addFilterConditions(co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder boolBuilder,
                                     String tenantId,
                                     Ec2LogQueryRequest request) {
        // 필수: tenantId, caseId, instanceId, accountId
        boolBuilder.filter(f -> f.term(t -> t.field("tenantId").value(tenantId)));
        boolBuilder.filter(f -> f.term(t -> t.field("caseId").value(request.caseId())));
        boolBuilder.filter(f -> f.term(t -> t.field("instance_id").value(request.instanceId())));
        boolBuilder.filter(f -> f.term(t -> t.field("account").value(request.accountId())));

        // 선택: logType, fileName, activity, outcome
        if (request.logType() != null && !request.logType().isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t.field("event.category").value(request.logType())));
        }
        if (request.fileName() != null && !request.fileName().isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t.field("@name").value(request.fileName())));
        }
        if (request.activity() != null && !request.activity().isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t.field("event.action").value(request.activity())));
        }
        if (request.outcome() != null && !request.outcome().isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t.field("event.outcome").value(request.outcome())));
        }

        // 시간 범위
        addTimeRangeFilter(boolBuilder, request);
    }

    private void addTimeRangeFilter(co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder boolBuilder,
                                     Ec2LogQueryRequest request) {
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
                                   Ec2LogQueryRequest request) {
        if (request.keyword() == null || request.keyword().isBlank()) {
            return;
        }

        String keyword = request.keyword();
        String wildcardPattern = "*" + keyword.toLowerCase(Locale.ROOT) + "*";
        log.debug("Searching with keyword: {}, wildcard: {}", keyword, wildcardPattern);

        // 검색 대상 필드 목록 (.text 서브필드 사용)
        List<String> searchFields = List.of(
                "event.action.text", "event.category.text", "event.outcome.text",
                "@name.text", "account.text", "instance_id.text", "region.text"
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
                .should(s -> s.wildcard(w -> w.field("event.action").value(wildcardPattern).caseInsensitive(true)))
                .should(s -> s.wildcard(w -> w.field("event.category").value(wildcardPattern).caseInsensitive(true)))
                .should(s -> s.wildcard(w -> w.field("@name").value(wildcardPattern).caseInsensitive(true)))
                .minimumShouldMatch("1")
        ));
    }

    private List<co.elastic.clients.elasticsearch._types.SortOptions> determineSortOptions(Ec2LogQueryRequest request) {
        // tie-breaker로 _doc 추가하여 안정적인 정렬 보장 (_id는 정렬 불가)
        if (request.keyword() != null && !request.keyword().isBlank()) {
            return List.of(
                    co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                            .score(sc -> sc.order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))
                    ),
                    co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                            .doc(d -> d.order(co.elastic.clients.elasticsearch._types.SortOrder.Asc))
                    )
            );
        }
        return List.of(
                co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                        .field(f -> f.field("@timestamp").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))
                ),
                co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                        .doc(d -> d.order(co.elastic.clients.elasticsearch._types.SortOrder.Asc))
                )
        );
    }

    private SliceWithSort<Ec2LogEvent> mapSearchResponse(SearchResponse<Ec2LogEvent> response,
                                                          Ec2LogQueryRequest request) {
        List<Hit<Ec2LogEvent>> hits = response.hits().hits();

        // hasNext 확인을 위해 pageSize + 1 만큼 조회했으므로, hasNext 여부 확인
        boolean hasNext = hits.size() > request.pageSize();

        // 실제 반환할 데이터는 pageSize 만큼만
        List<Hit<Ec2LogEvent>> contentHits = hits.stream()
                .limit(request.pageSize())
                .toList();

        List<Ec2LogEvent> content = contentHits.stream()
                .map(hit -> {
                    Ec2LogEvent event = hit.source();
                    if (event != null && event.getId() == null) {
                        event.setId(hit.id());
                    }
                    return event;
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        // 마지막 문서의 sort 값 추출 (배열 형태로 직렬화)
        String lastSortValue = null;
        if (!contentHits.isEmpty()) {
            Hit<Ec2LogEvent> lastHit = contentHits.get(contentHits.size() - 1);
            if (lastHit.sort() != null && !lastHit.sort().isEmpty()) {
                lastSortValue = serializeSortValues(lastHit.sort());
            }
        }

        // 전체 개수 추출
        long totalElements = response.hits().total() != null ? response.hits().total().value() : 0;

        Slice<Ec2LogEvent> slice = new SliceImpl<>(content, PageRequest.of(0, request.pageSize()), hasNext);
        return SliceWithSort.of(slice, lastSortValue, totalElements);
    }

    /**
     * searchAfter 문자열을 파싱하여 FieldValue 리스트로 변환
     * 예: "1733220000000_sort_id_123" -> [FieldValue(1733220000000), FieldValue(123)]
     * 예: "4.567_sort_id_123" -> [FieldValue(4.567), FieldValue(123)]
     */
    private List<FieldValue> parseSearchAfter(String searchAfter) {
        try {
            String[] parts = searchAfter.split("_sort_id_");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid searchAfter format: " + searchAfter);
            }

            // 첫 번째 값: long 또는 double
            FieldValue firstValue;
            if (parts[0].contains(".")) {
                // double (score)
                firstValue = FieldValue.of(Double.parseDouble(parts[0]));
            } else {
                // long (timestamp)
                firstValue = FieldValue.of(Long.parseLong(parts[0]));
            }

            // 두 번째 값: _doc
            long docValue = Long.parseLong(parts[1]);

            return List.of(firstValue, FieldValue.of(docValue));
        } catch (Exception e) {
            log.error("Failed to parse searchAfter: {}", searchAfter, e);
            throw new ElasticsearchCommunicationException(ErrorMessage.ELASTICSEARCH_COMMUNICATION_ERROR);
        }
    }

    /**
     * sort 값들을 간결한 문자열로 직렬화
     * 예: [FieldValue(1733220000000), FieldValue(123)] -> "1733220000000_sort_id_123"
     * 예: [FieldValue(4.567), FieldValue(123)] -> "4.567_sort_id_123" (score 기반)
     */
    private String serializeSortValues(List<FieldValue> sortValues) {
        try {
            if (sortValues.size() != 2) {
                log.warn("Expected 2 sort values, got {}", sortValues.size());
                return null;
            }

            // 첫 번째 값: timestamp (long) 또는 score (double)
            String firstValue;
            if (sortValues.get(0).isLong()) {
                firstValue = String.valueOf(sortValues.get(0).longValue());
            } else if (sortValues.get(0).isDouble()) {
                firstValue = String.valueOf(sortValues.get(0).doubleValue());
            } else {
                log.warn("First sort value is not a number: {}", sortValues.get(0));
                return null;
            }

            // 두 번째 값: _doc
            long docValue;
            if (sortValues.get(1).isLong()) {
                docValue = sortValues.get(1).longValue();
            } else if (sortValues.get(1).isDouble()) {
                docValue = (long) sortValues.get(1).doubleValue();
            } else {
                log.warn("Second sort value is not a number: {}", sortValues.get(1));
                return null;
            }

            return firstValue + "_sort_id_" + docValue;
        } catch (Exception e) {
            log.error("Failed to serialize sort values", e);
            return null;
        }
    }

    @Override
    public Optional<Ec2LogEvent> findByIdWithRouting(String tenantId, Long caseId, String id) {
        try {
            String routing = tenantId + "-" + caseId;

            // Criteria로 검색 (get 대신 search 사용하여 라우팅 적용)
            Criteria criteria = new Criteria("_id").is(id)
                    .and(new Criteria("tenantId").is(tenantId))
                    .and(new Criteria("caseId").is(caseId));

            CriteriaQuery query = new CriteriaQuery(criteria);
            query.setRoute(routing);

            SearchHits<Ec2LogEvent> hits = operations.search(query, Ec2LogEvent.class, dsOfTenant(tenantId, caseId));

            if (hits.hasSearchHits()) {
                Ec2LogEvent event = hits.getSearchHit(0).getContent();
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

    private IndexCoordinates dsOfTenant(String tenantId, Long caseId) {
        return IndexCoordinates.of("ec2-tenant-" + tenantId + "-" + caseId);
    }
}
