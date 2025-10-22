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
public class ScanEc2Metadata {
    public static final String METADATA = "METADATA";

    private String pk;              // Partition Key: EC2#{ec2ScanId}
    private String sk;              // Sort Key: METADATA
    private Long ec2ScanId;
    private Long tenantId;
    private Long caseId;
    private String accountId;
    private String createdAt;

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

    @DynamoDbAttribute("ec2_scan_id")
    public Long getScanId() {
        return ec2ScanId;
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

    @DynamoDbAttribute("created_at")
    public String getCreatedAt() {
        return createdAt;
    }

    public void updatePk(String pk) { this.pk = pk; }

    public void updateSk(String sk) { this.sk = sk; }

    public void updateEc2ScanId(Long ec2ScanId) { this.ec2ScanId = ec2ScanId; }

    public void updateTenantId(Long tenantId) { this.tenantId = tenantId; }

    public void updateCaseId(Long caseId) { this.caseId = caseId; }

    public void updateAccountId(String accountId) { this.accountId = accountId; }

    public void updateCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
