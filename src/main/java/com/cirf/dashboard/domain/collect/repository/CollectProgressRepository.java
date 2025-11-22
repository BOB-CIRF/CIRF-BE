package com.cirf.dashboard.domain.collect.repository;

import com.cirf.dashboard.domain.collect.entity.CollectProgress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.Comparator;
import java.util.Optional;

/**
 * cirf-collect 테이블의 진행 상황 리포지토리
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CollectProgressRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private static final String COLLECT_TABLE_NAME = "cirf-collect";

    public Optional<CollectProgress> findLatestByCollectId(Integer collectId) {
        DynamoDbTable<CollectProgress> table = dynamoDbEnhancedClient.table(
                COLLECT_TABLE_NAME,
                TableSchema.fromBean(CollectProgress.class)
        );

        String pk = String.format("PROGRESS#%s", collectId);

        log.info("Querying DynamoDB for latest progress - collectId: {}, PK: {}", collectId, pk);

        // PK로 모든 PROGRESS 항목 조회
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                        .partitionValue(pk)
                        .build()))
                .scanIndexForward(false)  // 내림차순 정렬 (최신순)
                .limit(1)  // 최신 1개만 가져오기
                .build();

        Optional<CollectProgress> result = table.query(queryRequest)
                .stream()
                .flatMap(page -> page.items().stream())
                .filter(item -> item.getSk() != null && item.getSk().startsWith("JOB#"))
                .max(Comparator.comparing(CollectProgress::getRevision));

        if (result.isPresent()) {
            log.info("Found progress in DynamoDB - collectId: {}, revision: {}",
                    collectId, result.get().getRevision());
        } else {
            log.info("No progress found in DynamoDB for collectId: {}", collectId);
        }

        return result;
    }
}
