package com.cirf.dashboard.domain.scan.repository;

import com.cirf.dashboard.domain.scan.entity.EnabledLogs;
import com.cirf.dashboard.domain.scan.entity.ScanLogsMetadata;
import com.cirf.dashboard.domain.scan.entity.ScanRegionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ScanLogsRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Value("${aws.dynamodb.table-name}")
    private String tableName;

    public Optional<ScanLogsMetadata> findLatestScan(long userId, long caseId, String accountId) {
        DynamoDbTable<ScanLogsMetadata> table = dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(ScanLogsMetadata.class));
        DynamoDbIndex<ScanLogsMetadata> gsi2 = table.index("GSI2PK-GSI2SK-index");

        String gsi2Pk = String.format("USER#%d#CASE#%d#ACCOUNT#%s", userId, caseId, accountId);

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


    public Optional<ScanRegionStatus> findScanRegionStatus(String scanPk, String regionSk) {
        DynamoDbTable<ScanRegionStatus> table = dynamoDbEnhancedClient.table(
                tableName,
                TableSchema.fromBean(ScanRegionStatus.class)
        );

        Key key = Key.builder()
                .partitionValue(scanPk)
                .sortValue(regionSk)
                .build();

        return Optional.ofNullable(table.getItem(key));
    }

    public boolean existsEnabledLog(Long scanId, String accountId, String region, String logType) {
        DynamoDbTable<EnabledLogs> table = dynamoDbEnhancedClient.table(
                tableName,
                TableSchema.fromBean(EnabledLogs.class)
        );
        DynamoDbIndex<EnabledLogs> gsi1 = table.index("GSI1PK-GSI1SK-index");

        String gsi1Pk = String.format("ACCOUNT#%s#LOGTYPE#%s#REG#%s", accountId, logType, region);
        String gsi1Sk = "SCAN#" + scanId;

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                        .partitionValue(gsi1Pk)
                        .sortValue(gsi1Sk)
                        .build()))
                .limit(1)
                .build();

        return gsi1.query(queryRequest)
                .stream()
                .flatMap(page -> page.items().stream())
                .findFirst()
                .isPresent();
    }
}
