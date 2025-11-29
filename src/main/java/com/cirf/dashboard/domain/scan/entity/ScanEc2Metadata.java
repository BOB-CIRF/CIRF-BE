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
    private Long userId;
    private Long caseId;
    private Long tenantId;
    private String accountId;
    private String createdAt;

    private String gsi2Pk;          // GSI2 Partition Key: USER#{userId}#CASE#{caseId}#ACCOUNT#{accountId}#TYPE#EC2
    private String gsi2Sk;          // GSI2 Sort Key: CREATED#{createdAt}#EC2#{ec2ScanId}

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

    @DynamoDbAttribute("user_id")
    public Long getUserId() {
        return userId;
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

    public void updatePk(String pk) { this.pk = pk; }

    public void updateSk(String sk) { this.sk = sk; }

    public void updateEc2ScanId(Long ec2ScanId) { this.ec2ScanId = ec2ScanId; }

    public void updateUserId(Long userId) { this.userId = userId; }

    public void updateCaseId(Long caseId) { this.caseId = caseId; }

    public void updateAccountId(String accountId) { this.accountId = accountId; }

    public void updateCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public void updateGsi2Pk(String gsi2Pk) { this.gsi2Pk = gsi2Pk; }

    public void updateGsi2Sk(String gsi2Sk) { this.gsi2Sk = gsi2Sk; }
}
