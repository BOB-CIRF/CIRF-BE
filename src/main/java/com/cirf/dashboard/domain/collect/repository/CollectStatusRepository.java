package com.cirf.dashboard.domain.collect.repository;

import com.cirf.dashboard.domain.collect.entity.CollectStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CollectStatusRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private static final String COLLECT_TABLE_NAME = "collect_status";

    public void save(CollectStatus collectStatus) {
        try {
            DynamoDbTable<CollectStatus> table = dynamoDbEnhancedClient.table(
                    COLLECT_TABLE_NAME,
                    TableSchema.fromBean(CollectStatus.class)
            );

            table.putItem(collectStatus);

            log.info("CollectStatus saved to DynamoDB - PK: {}, SK: {}",
                    collectStatus.getPk(), collectStatus.getSk());

        } catch (Exception e) {
            log.error("Failed to save CollectStatus to DynamoDB", e);
            throw new RuntimeException("DynamoDB 저장 실패", e);
        }
    }
}
