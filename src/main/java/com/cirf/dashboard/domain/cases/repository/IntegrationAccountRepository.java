package com.cirf.dashboard.domain.cases.repository;

import com.cirf.dashboard.domain.cases.entity.IntegrationAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class IntegrationAccountRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private static final String TABLE_NAME = "cirf";

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

    public void saveAll(List<IntegrationAccount> accounts) {
        accounts.forEach(this::save);
    }

    public void delete(String pk, String sk) {
        try {
            DynamoDbTable<IntegrationAccount> table = dynamoDbEnhancedClient.table(
                    TABLE_NAME,
                    TableSchema.fromBean(IntegrationAccount.class)
            );

            Key key = Key.builder()
                    .partitionValue(pk)
                    .sortValue(sk)
                    .build();

            table.deleteItem(key);

            log.info("IntegrationAccount deleted from DynamoDB - PK: {}, SK: {}", pk, sk);

        } catch (Exception e) {
            log.error("Failed to delete IntegrationAccount from DynamoDB", e);
            throw new RuntimeException("DynamoDB 삭제 실패", e);
        }
    }

    public void deleteByUserIdAndCaseId(Long userId, Long caseId) {
        String pk = String.format("USER#%d#CASE#%d", userId, caseId);
        try {
            DynamoDbTable<IntegrationAccount> table = dynamoDbEnhancedClient.table(
                    TABLE_NAME,
                    TableSchema.fromBean(IntegrationAccount.class)
            );

            table.query(r -> r.queryConditional(
                    software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo(
                            Key.builder().partitionValue(pk).build()
                    )
            )).items().forEach(item -> {
                delete(item.getPk(), item.getSk());
            });

            log.info("All IntegrationAccounts deleted for PK: {}", pk);

        } catch (Exception e) {
            log.error("Failed to delete IntegrationAccounts by PK from DynamoDB", e);
            throw new RuntimeException("DynamoDB 삭제 실패", e);
        }
    }
}