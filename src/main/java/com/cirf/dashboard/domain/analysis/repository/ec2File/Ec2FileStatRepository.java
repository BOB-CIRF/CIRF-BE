package com.cirf.dashboard.domain.analysis.repository.ec2File;

import com.cirf.dashboard.domain.analysis.entity.Ec2FileStat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class Ec2FileStatRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private static final String COLLECT_TABLE_NAME = "cirf";

    public List<Ec2FileStat> queryFileStats(
            String tenantId,
            Long caseId,
            String accountId,
            String region,
            String instanceId,
            String keyword,
            int page,
            int size
    ) {
        DynamoDbTable<Ec2FileStat> table = dynamoDbEnhancedClient.table(
                COLLECT_TABLE_NAME,
                TableSchema.fromBean(Ec2FileStat.class)
        );

        // PK 구성: STAT#TENANT#{tenantId}#CASE#{caseId}
        String pk = String.format("STAT#TENANT#%s#CASE#%d", tenantId, caseId);
        // SK prefix: ACCOUNT#{accountId}#REG#{region}#INSTANCE#{instanceId}#
        String skPrefix = String.format("ACCOUNT#%s#REG#%s#INSTANCE#%s#", accountId, region, instanceId);

        log.info("Query with PK: {}, SK prefix: {}, keyword: {}, page: {}, size: {}",
                pk, skPrefix, keyword, page, size);

        // FilterExpression 구성 (keyword만 필터링)
        Expression filterExpression = buildFilterExpression(keyword);

        // 필요한 만큼 조회: (page + 1) * size + 1 (hasNext 확인용)
        int totalNeeded = (page + 1) * size + 1;
        List<Ec2FileStat> allResults = new ArrayList<>();
        Map<String, AttributeValue> currentStartKey = null;

        while (allResults.size() < totalNeeded) {
            QueryEnhancedRequest.Builder queryBuilder = QueryEnhancedRequest.builder()
                    .queryConditional(QueryConditional.sortBeginsWith(Key.builder()
                            .partitionValue(pk)
                            .sortValue(skPrefix)
                            .build()));

            if (filterExpression != null) {
                queryBuilder.filterExpression(filterExpression);
            }

            if (currentStartKey != null && !currentStartKey.isEmpty()) {
                queryBuilder.exclusiveStartKey(currentStartKey);
            }

            // 한 번에 많이 조회
            queryBuilder.limit(100);

            Page<Ec2FileStat> page_result = table.query(queryBuilder.build())
                    .stream()
                    .findFirst()
                    .orElse(Page.create(List.of()));

            List<Ec2FileStat> items = page_result.items();
            if (items.isEmpty()) {
                break;
            }

            allResults.addAll(items);
            currentStartKey = page_result.lastEvaluatedKey();

            // 더 이상 데이터가 없으면 종료
            if (currentStartKey == null || currentStartKey.isEmpty()) {
                break;
            }
        }

        log.info("Total results found: {}", allResults.size());

        // 페이지에 해당하는 데이터만 반환 (skip + take)
        int skip = page * size;
        List<Ec2FileStat> pageResults = allResults.stream()
                .skip(skip)
                .limit(size + 1)  // hasNext 확인용 +1
                .toList();

        log.info("Returning {} items for page {}", pageResults.size(), page);
        return pageResults;
    }

    public long countFileStats(
            String tenantId,
            Long caseId,
            String accountId,
            String region,
            String instanceId,
            String keyword
    ) {
        DynamoDbTable<Ec2FileStat> table = dynamoDbEnhancedClient.table(
                COLLECT_TABLE_NAME,
                TableSchema.fromBean(Ec2FileStat.class)
        );

        String pk = String.format("STAT#TENANT#%s#CASE#%d", tenantId, caseId);
        String skPrefix = String.format("ACCOUNT#%s#REG#%s#INSTANCE#%s#", accountId, region, instanceId);

        Expression filterExpression = buildFilterExpression(keyword);

        long count = 0;
        Map<String, AttributeValue> lastKey = null;

        do {
            QueryEnhancedRequest.Builder queryBuilder = QueryEnhancedRequest.builder()
                    .queryConditional(QueryConditional.sortBeginsWith(Key.builder()
                            .partitionValue(pk)
                            .sortValue(skPrefix)
                            .build()));

            if (filterExpression != null) {
                queryBuilder.filterExpression(filterExpression);
            }

            if (lastKey != null && !lastKey.isEmpty()) {
                queryBuilder.exclusiveStartKey(lastKey);
            }

            Page<Ec2FileStat> page = table.query(queryBuilder.build())
                    .stream()
                    .findFirst()
                    .orElse(Page.create(List.of()));

            count += page.items().size();
            lastKey = page.lastEvaluatedKey();

        } while (lastKey != null && !lastKey.isEmpty());

        log.info("Total count: {}", count);
        return count;
    }

    private Expression buildFilterExpression(String keyword) {
        // keyword만 필터링 (file 필드에 contains)
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        Map<String, String> expressionNames = new HashMap<>();

        expressionValues.put(":keyword", AttributeValue.builder().s(keyword).build());
        expressionNames.put("#file", "file");

        return Expression.builder()
                .expression("contains(#file, :keyword)")
                .expressionValues(expressionValues)
                .expressionNames(expressionNames)
                .build();
    }
}
