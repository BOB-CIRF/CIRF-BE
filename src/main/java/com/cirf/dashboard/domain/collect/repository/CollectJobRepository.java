package com.cirf.dashboard.domain.collect.repository;

import com.cirf.dashboard.domain.collect.entity.CollectJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CollectJobRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private static final String COLLECT_TABLE_NAME = "cirf-collect";

    public List<CollectJob> findAllByJobId(String jobId) {
        DynamoDbTable<CollectJob> table = dynamoDbEnhancedClient.table(
                COLLECT_TABLE_NAME,
                TableSchema.fromBean(CollectJob.class)
        );

        String pk = String.format("JOB#%s", jobId);

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                        .partitionValue(pk)
                        .build()))
                .build();

        return table.query(queryRequest)
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();
    }

    public List<CollectJob> findAllByCollectId(String collectId) {
        DynamoDbTable<CollectJob> table = dynamoDbEnhancedClient.table(
                COLLECT_TABLE_NAME,
                TableSchema.fromBean(CollectJob.class)
        );

        String skPrefix = String.format("COLLECT#%s#", collectId);

        log.info("Scanning DynamoDB table: {} with SK prefix: {}", COLLECT_TABLE_NAME, skPrefix);

        // Scan with filter expression for SK
        List<CollectJob> results = table.scan(builder -> builder
                        .filterExpression(software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                                .expression("begins_with(SK, :skPrefix)")
                                .putExpressionValue(":skPrefix",
                                        software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                                                .s(skPrefix)
                                                .build())
                                .build())
                        .build())
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();

        log.info("Found {} items for collectId: {}", results.size(), collectId);
        return results;
    }
}
