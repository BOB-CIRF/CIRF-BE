package com.cirf.dashboard.domain.cases.repository;

import com.cirf.dashboard.domain.cases.entity.IntegrationAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Slf4j
@Repository
@RequiredArgsConstructor
public class IntegrationAccountRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private static final String TABLE_NAME = "cirf"; // 테이블명 변경 필요 시 수정

    /**
     * IntegrationAccount 저장
     */
    public void save(IntegrationAccount integrationAccount) {
        try {
            DynamoDbTable<IntegrationAccount> table = dynamoDbEnhancedClient.table(
                    TABLE_NAME,
                    TableSchema.fromBean(IntegrationAccount.class)
            );

            table.putItem(integrationAccount);

            log.info("IntegrationAccount saved to DynamoDB - PK: {}, SK: {}",
                    integrationAccount.getPk(), integrationAccount.getSk());

        } catch (Exception e) {
            log.error("Failed to save IntegrationAccount to DynamoDB", e);
            throw new RuntimeException("DynamoDB 저장 실패", e);
        }
    }

    /**
     * 여러 IntegrationAccount 일괄 저장
     */
    public void saveAll(java.util.List<IntegrationAccount> accounts) {
        accounts.forEach(this::save);
    }
}