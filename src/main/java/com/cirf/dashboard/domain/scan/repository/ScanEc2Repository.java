package com.cirf.dashboard.domain.scan.repository;

import com.cirf.dashboard.domain.scan.entity.EnabledInstances;
import com.cirf.dashboard.domain.scan.entity.ScanEc2Metadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ScanEc2Repository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.table-name}")
    private String tableName;

    public Optional<ScanEc2Metadata> createScanEc2Metadata(long tenantId, long caseId, String accountId){
        // 1. Counter를 사용하여 새로운 ec2ScanId 생성
        Long newEc2ScanId = getNextEc2ScanId();

        // 2. ScanEc2Metadata 객체 생성
        String pk = "EC2#" + newEc2ScanId;
        String sk = ScanEc2Metadata.METADATA;
        String createdAt = Instant.now().toString();

        ScanEc2Metadata metadata = ScanEc2Metadata.builder()
                .pk(pk)
                .sk(sk)
                .ec2ScanId(newEc2ScanId)
                .tenantId(tenantId)
                .caseId(caseId)
                .accountId(accountId)
                .createdAt(createdAt)
                .build();

        // 3. DynamoDB에 저장
        DynamoDbTable<ScanEc2Metadata> table = dynamoDbEnhancedClient.table(
                tableName,
                TableSchema.fromBean(ScanEc2Metadata.class)
        );

        table.putItem(metadata);

        return Optional.of(metadata);
    }

    private Long getNextEc2ScanId() {
        // Counter 아이템의 키
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("PK", AttributeValue.builder().s("COUNTER").build());
        key.put("SK", AttributeValue.builder().s("EC2_SCAN_ID").build());

        // UpdateItem을 사용하여 atomic하게 카운터 증가
        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .updateExpression("ADD #counter :increment")
                .expressionAttributeNames(Map.of("#counter", "counter_value"))
                .expressionAttributeValues(Map.of(":increment", AttributeValue.builder().n("1").build()))
                .returnValues("UPDATED_NEW")
                .build();

        UpdateItemResponse response = dynamoDbClient.updateItem(updateRequest);

        return Long.parseLong(response.attributes().get("counter_value").n());
    }

    public void saveEnabledInstances(List<EnabledInstances> instances) {
        DynamoDbTable<EnabledInstances> table = dynamoDbEnhancedClient.table(
                tableName,
                TableSchema.fromBean(EnabledInstances.class)
        );

        // 배치로 저장
        instances.forEach(table::putItem);
    }
}
