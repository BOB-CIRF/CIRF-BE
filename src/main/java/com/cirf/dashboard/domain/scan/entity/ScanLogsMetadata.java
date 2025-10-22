package com.cirf.dashboard.domain.scan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class ScanLogsMetadata {

    public static final String METADATA = "METADATA";

    private String pk;              // Partition Key: SCAN#{scanId}
    private String sk;              // Sort Key: METADATA
    private Long scanId;
    private Long tenantId;
    private Long caseId;
    private String accountId;
    private String status;
    private String startedAt;
    private String finishedAt;

    private String gsi2Pk;          // GSI2 Partition Key: TENANT#{tenantId}#CASE#{caseId}#ACCOUNT#{accountId}
    private String gsi2Sk;          // GSI2 Sort Key: START#{startedAt}#SCAN#{scanId}

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() {
        return pk;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() {
        return sk;
    }

    @DynamoDbAttribute("scan_id")
    public Long getScanId() {
        return scanId;
    }

    @DynamoDbAttribute("tenant_id")
    public Long getTenantId() {
        return tenantId;
    }

    @DynamoDbAttribute("case_id")
    public Long getCaseId() {
        return caseId;
    }

    @DynamoDbAttribute("account_id")
    public String getAccountId() {
        return accountId;
    }

    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }

    @DynamoDbAttribute("started_at")
    public String getStartedAt() {
        return startedAt;
    }

    @DynamoDbAttribute("finished_at")
    public String getFinishedAt() {
        return finishedAt;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "GSI2PK-GSI2SK-index")
    @DynamoDbAttribute("GSI2PK")
    public String getGsi2Pk() {
        return gsi2Pk;
    }

    @DynamoDbSecondarySortKey(indexNames = "GSI2PK-GSI2SK-index")
    @DynamoDbAttribute("GSI2SK")
    public String getGsi2Sk() {
        return gsi2Sk;
    }

}

