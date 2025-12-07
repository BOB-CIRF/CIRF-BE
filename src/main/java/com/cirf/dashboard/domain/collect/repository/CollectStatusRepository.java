package com.cirf.dashboard.domain.collect.repository;

import com.cirf.dashboard.domain.collect.entity.CollectStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CollectStatusRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbClient dynamoDbClient;
    private static final String COLLECT_TABLE_NAME = "collect_status";
    private static final String COUNTER_PK = "COUNTER";
    private static final String COUNTER_SK = "PROGRESS_ID";

    public Long getNextProgressId() {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("PK", AttributeValue.builder().s(COUNTER_PK).build());
            key.put("SK", AttributeValue.builder().s(COUNTER_SK).build());

            Map<String, AttributeValueUpdate> updates = new HashMap<>();
            updates.put("counter", AttributeValueUpdate.builder()
                    .value(AttributeValue.builder().n("1").build())
                    .action(AttributeAction.ADD)
                    .build());

            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(COLLECT_TABLE_NAME)
                    .key(key)
                    .attributeUpdates(updates)
                    .returnValues(ReturnValue.UPDATED_NEW)
                    .build();

            UpdateItemResponse response = dynamoDbClient.updateItem(request);
            Long progressId = Long.parseLong(response.attributes().get("counter").n());

            log.info("Generated new progressId: {}", progressId);
            return progressId;

        } catch (Exception e) {
            log.error("Failed to generate progressId", e);
            throw new RuntimeException("progressId 생성 실패", e);
        }
    }

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


    public Optional<CollectStatus> findByProgressId(Long progressId) {
        try {
            DynamoDbTable<CollectStatus> table = dynamoDbEnhancedClient.table(
                    COLLECT_TABLE_NAME,
                    TableSchema.fromBean(CollectStatus.class)
            );

            String pk = String.format("PROGRESS#%d", progressId);
            String sk = "STATUS";

            Key key = Key.builder()
                    .partitionValue(pk)
                    .sortValue(sk)
                    .build();

            CollectStatus status = table.getItem(key);

            if (status != null) {
                log.debug("CollectStatus found - progressId: {}, totalJob: {}, completed: {}",
                        progressId, status.getTotalJob(), status.getCompleted());
            } else {
                log.debug("CollectStatus not found - progressId: {}", progressId);
            }

            return Optional.ofNullable(status);

        } catch (Exception e) {
            log.error("Failed to find CollectStatus by progressId: {}", progressId, e);
            throw new RuntimeException("DynamoDB 조회 실패", e);
        }
    }
}
