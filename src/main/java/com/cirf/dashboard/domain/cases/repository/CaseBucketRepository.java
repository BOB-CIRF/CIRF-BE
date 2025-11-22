package com.cirf.dashboard.domain.cases.repository;

import com.cirf.dashboard.domain.cases.entity.CaseBucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CaseBucketRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Value("${aws.dynamodb.table-name}")
    private String tableName;

    public void save(CaseBucket caseBucket) {
        try {
            DynamoDbTable<CaseBucket> table = dynamoDbEnhancedClient.table(
                    tableName,
                    TableSchema.fromBean(CaseBucket.class)
            );

            table.putItem(caseBucket);
        } catch (Exception e) {
            log.error("Failed to save IntegrationAccount to DynamoDB", e);
            throw new RuntimeException("DynamoDB 저장 실패", e);
        }
    }
}
