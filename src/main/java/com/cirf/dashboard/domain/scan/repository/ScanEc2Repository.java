package com.cirf.dashboard.domain.scan.repository;

import com.cirf.dashboard.domain.scan.entity.EnabledInstances;
import com.cirf.dashboard.domain.scan.entity.ScanEc2Metadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Tag;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ScanEc2Repository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbClient dynamoDbClient;

    private final ExecutorService executor;

    @Value("${aws.dynamodb.table-name}")
    private String tableName;

    public Optional<ScanEc2Metadata> createScanEc2Metadata(long userId, long caseId, String accountId){
        // 1. Counter를 사용하여 새로운 ec2ScanId 생성
        Long newEc2ScanId = getNextEc2ScanId();

        // 2. ScanEc2Metadata 객체 생성
        String pk = "EC2#" + newEc2ScanId;
        String sk = ScanEc2Metadata.METADATA;
        String createdAt = Instant.now().toString();

        // GSI2 키 생성 (TYPE을 포함하여 Logs Scan과 구별)
        String gsi2Pk = String.format("USER#%d#CASE#%d#ACCOUNT#%s#TYPE#EC2", userId, caseId, accountId);
        String gsi2Sk = String.format("CREATED#%s#EC2#%d", createdAt, newEc2ScanId);

        ScanEc2Metadata metadata = ScanEc2Metadata.builder()
                .pk(pk)
                .sk(sk)
                .ec2ScanId(newEc2ScanId)
                .userId(userId)
                .caseId(caseId)
                .accountId(accountId)
                .createdAt(createdAt)
                .gsi2Pk(gsi2Pk)
                .gsi2Sk(gsi2Sk)
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

    public Long getNextEc2IdxId() {
        // Counter 아이템의 키
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("PK", AttributeValue.builder().s("COUNTER").build());
        key.put("SK", AttributeValue.builder().s("EC2#IDX").build());

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

    public Optional<ScanEc2Metadata> findLatestEc2Scan(long userId, long caseId, String accountId) {
        DynamoDbTable<ScanEc2Metadata> table = dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(ScanEc2Metadata.class));
        DynamoDbIndex<ScanEc2Metadata> gsi2 = table.index("GSI2PK-GSI2SK-index");

        // TYPE#EC2를 포함하여 EC2 스캔만 조회
        String gsi2Pk = String.format("USER#%d#CASE#%d#ACCOUNT#%s#TYPE#EC2", userId, caseId, accountId);

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                        .partitionValue(gsi2Pk)
                        .build()))
                .scanIndexForward(false)  // 내림차순 정렬 (최신순)
                .limit(1)
                .build();

        return gsi2.query(queryRequest)
                .stream()
                .flatMap(page -> page.items().stream())
                .findFirst();
    }

    public List<EnabledInstances> getEc2InstancesByRegion(Long ec2ScanId, String region) {
        DynamoDbTable<EnabledInstances> table = dynamoDbEnhancedClient.table(
                tableName,
                TableSchema.fromBean(EnabledInstances.class)
        );

        // region이 null이거나 empty면 모든 리전 조회, 아니면 특정 리전만 조회
        String sortKeyPrefix = (region == null || region.isEmpty())
                ? "REG#"
                : "REG#" + region + "#";

        Key key = Key.builder()
                .partitionValue("EC2#" + ec2ScanId)
                .sortValue(sortKeyPrefix)
                .build();

        return table.query(r -> r.queryConditional(
                software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
                        .sortBeginsWith(key)
        )).items().stream().toList();
    }

    public EnabledInstances createEnabledInstance(Long ec2ScanId, String region, Instance instance) {
        String instanceId = instance.instanceId();
        String instanceName = instance.tags().stream()
                .filter(tag -> "Name".equals(tag.key()))
                .map(Tag::value)
                .findFirst()
                .orElse("");

        // idxId 생성
        Long idxId = getNextEc2IdxId();

        String pk = "EC2#" + ec2ScanId;
        String sk = String.format("REG#%s#INSTANCE#%s", region, instanceId);
        String gsi4Pk = "EC2#IDX#" + idxId;

        return EnabledInstances.builder()
                .pk(pk)
                .sk(sk)
                .ec2ScanId(ec2ScanId)
                .idxId(idxId)
                .instanceId(instanceId)
                .instanceName(instanceName)
                .instanceType(instance.instanceType().toString())
                .instancePlatformDetails(instance.platformDetails())
                .region(region)
                .status(instance.state().nameAsString())
                .publicIp(instance.publicIpAddress() != null ? instance.publicIpAddress() : "")
                .gsi4Pk(gsi4Pk)
                .build();
    }

    public List<Long> getEc2ScanIds(long userId, long caseId, List<String> targetAccountIds) {
        return targetAccountIds.parallelStream()
                .map(acc -> {
                    try {
                        return findLatestEc2Scan(userId, caseId, acc)
                                .map(ScanEc2Metadata::getScanId)
                                .orElse(null);
                    } catch (Exception e) {
                        log.warn("findLatestEc2Scan failed for accountId={}, userId={}, caseId={}",
                                acc, userId, caseId, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public List<EnabledInstances> getEc2InstancesByRegionIn(List<Long> ec2ScanIds, @Nullable String region) {
        if (ec2ScanIds == null || ec2ScanIds.isEmpty()) return List.of();

        DynamoDbTable<EnabledInstances> table = dynamoDbEnhancedClient.table(
                tableName,
                TableSchema.fromBean(EnabledInstances.class)
        );

        // 병렬 Query (scanId 별)
        List<CompletableFuture<List<EnabledInstances>>> futures = ec2ScanIds.stream()
                .map(scanId -> CompletableFuture.supplyAsync(() -> queryByScanId(table, scanId, region), executor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();
    }

    private List<EnabledInstances> queryByScanId(DynamoDbTable<EnabledInstances> table, Long scanId, String region) {
        // PK = "EC2#scanId", SK starts with "REG#" (METADATA 제외)
        Key key = Key.builder()
                .partitionValue("EC2#" + scanId)
                .sortValue("REG#")
                .build();

        QueryEnhancedRequest.Builder qb = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.sortBeginsWith(key));  // SK가 REG#로 시작하는 것만

        // region 필터가 있으면 추가 (FilterExpression은 DynamoDB post-filter임)
        if (region != null && !region.isBlank()) {
            qb.filterExpression(Expression.builder()
                    .expression("#r = :region")
                    .putExpressionName("#r", "region")
                    .putExpressionValue(":region", AttributeValue.builder().s(region).build())
                    .build());
        }

        return table.query(qb.build())
                .items()
                .stream()
                .toList();
    }

}
