package com.cirf.dashboard.domain.analysis.entity;

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
public class Ec2FileStat {

    private String pk;              // Partition Key: STAT#TENANT#{tenantId}#CASE#{caseId}
    private String sk;              // Sort Key: ACCOUNT#{accountId}#REG#{region}#INSTANCE#{instanceId}#FILE#{file}

    private String access;
    private String accessTime;
    private String accountId;
    private String birthTime;
    private String caseId;
    private String changeTime;
    private String file;
    private String gid;
    private String instanceId;
    private String modifyTime;
    private String region;
    private String size;
    private String sourceAccountId;
    private String sourceRegion;
    private String targetInstanceId;
    private String targetRegion;
    private String uid;
    private String tenantId;

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

    @DynamoDbAttribute("access")
    public String getAccess() {
        return access;
    }

    @DynamoDbAttribute("accessTime")
    public String getAccessTime() {
        return accessTime;
    }

    @DynamoDbAttribute("accountId")
    public String getAccountId() {
        return accountId;
    }

    @DynamoDbAttribute("birthTime")
    public String getBirthTime() {
        return birthTime;
    }

    @DynamoDbAttribute("caseId")
    public String getCaseId() {
        return caseId;
    }

    @DynamoDbAttribute("changeTime")
    public String getChangeTime() {
        return changeTime;
    }

    @DynamoDbAttribute("file")
    public String getFile() {
        return file;
    }

    @DynamoDbAttribute("gid")
    public String getGid() {
        return gid;
    }

    @DynamoDbAttribute("instanceId")
    public String getInstanceId() {
        return instanceId;
    }

    @DynamoDbAttribute("modifyTime")
    public String getModifyTime() {
        return modifyTime;
    }

    @DynamoDbAttribute("region")
    public String getRegion() {
        return region;
    }

    @DynamoDbAttribute("sourceAccountId")
    public String getSourceAccountId() {
        return sourceAccountId;
    }

    @DynamoDbAttribute("size")
    public String getSize() {
        return size;
    }

    @DynamoDbAttribute("sourceRegion")
    public String getSourceRegion() {
        return sourceRegion;
    }

    @DynamoDbAttribute("targetInstanceId")
    public String getTargetInstanceId() {
        return targetInstanceId;
    }

    @DynamoDbAttribute("targetRegion")
    public String getTargetRegion() {
        return targetRegion;
    }

    @DynamoDbAttribute("uid")
    public String getUid() {
        return uid;
    }

    @DynamoDbAttribute("tenantId")
    public String getTenantId() {
        return tenantId;
    }

}
