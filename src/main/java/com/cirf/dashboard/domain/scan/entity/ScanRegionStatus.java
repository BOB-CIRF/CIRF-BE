package com.cirf.dashboard.domain.scan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class ScanRegionStatus {

    private String pk;              // Partition Key: SCAN#{scanId}
    private String sk;              // Sort Key: REG#{region}
    private String region;
    private String status;
    private Long scanId;
    private Long tenantId;
    private String startedAt;
    private String finishedAt;

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

    @DynamoDbAttribute("region")
    public String getRegion() {
        return region;
    }

    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }

    @DynamoDbAttribute("scan_id")
    public Long getScanId() {
        return scanId;
    }

    @DynamoDbAttribute("tenant_id")
    public Long getTenantId() {
        return tenantId;
    }

    @DynamoDbAttribute("started_at")
    public String getStartedAt() {
        return startedAt;
    }

    @DynamoDbAttribute("finished_at")
    public String getFinishedAt() {
        return finishedAt;
    }

}
