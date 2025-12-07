package com.cirf.dashboard.domain.collect.repository;

import com.cirf.dashboard.domain.collect.entity.CollectEc2Job;
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
public class CollectEc2JobRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private static final String COLLECT_TABLE_NAME = "cirf-collect-ec2";

    public List<CollectEc2Job> findAllByCollectId(long collectId) {
        DynamoDbTable<CollectEc2Job> table = dynamoDbEnhancedClient.table(
                COLLECT_TABLE_NAME,
                TableSchema.fromBean(CollectEc2Job.class)
        );

        String pk = String.format("JOB#REQ#%d", collectId);

        log.info("Querying DynamoDB table: {} with PK: {}", COLLECT_TABLE_NAME, pk);

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                        .partitionValue(pk)
                        .build()))
                .build();

        List<CollectEc2Job> results = table.query(queryRequest)
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();

        log.info("Found {} items for collectId: {}", results.size(), collectId);
        return results;
    }
}
