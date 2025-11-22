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

        // TYPE#LOGS를 포함하여 Logs 스캔만 조회
        String gsi2Pk = String.format("USER#%d#CASE#%d#ACCOUNT#%s#TYPE#LOGS", userId, caseId, accountId);

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

        // GSI1PK는 region을 포함하지 않음
        String gsi1Pk = String.format("ACCOUNT#%s#LOGTYPE#%s", accountId, logType);

        // region이 null이거나 empty면 모든 리전 검색, 아니면 특정 리전만 검색
        if (region == null || region.isEmpty()) {
            // 모든 리전 검색: SK begins_with "SCAN#{scanId}"
            String gsi1SkPrefix = "SCAN#" + scanId;

            QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                    .queryConditional(QueryConditional.sortBeginsWith(Key.builder()
                            .partitionValue(gsi1Pk)
                            .sortValue(gsi1SkPrefix)
                            .build()))
                    .limit(1)
                    .build();

            return gsi1.query(queryRequest)
                    .stream()
                    .flatMap(page -> page.items().stream())
                    .findFirst()
                    .isPresent();
        } else {
            // 특정 리전만 검색: SK begins_with "SCAN#{scanId}#REG#{region}"
            String gsi1SkPrefix = String.format("SCAN#%d#REG#%s", scanId, region);

            QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                    .queryConditional(QueryConditional.sortBeginsWith(Key.builder()
                            .partitionValue(gsi1Pk)
                            .sortValue(gsi1SkPrefix)
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
}
