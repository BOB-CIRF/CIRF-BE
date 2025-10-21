package com.cirf.dashboard.domain.scan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class EnabledLogs {

    private String pk;              // Partition Key: SCAN#{scanId}
    private String sk;              // Sort Key: LOG#{logId}
    private String logArn;
    private String logType;
    private String logRegion;
    private Long logId;
    private String createdAt;
    private String gsi1Pk; // ACCOUNT#{accountId}#LOGTYPE#{ex.cloudtrail}#REG#{region}
    private String gsi1Sk; // SCAN#{scanId}

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

    @DynamoDbAttribute("logArn")
    public String getLogArn() {
        return logArn;
    }

    @DynamoDbAttribute("logType")
    public String getLogType() {
        return logType;
    }

    @DynamoDbAttribute("logRegion")
    public String getLogRegion() {
        return logRegion;
    }

    @DynamoDbAttribute("logId")
    public Long getLogId() {
        return logId;
    }

    @DynamoDbAttribute("createdAt")
    public String getCreatedAt() {
        return createdAt;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "GSI1PK-GSI1SK-index")
    @DynamoDbAttribute("GSI1PK")
    public String getGsi1Pk() {
        return gsi1Pk;
    }

    @DynamoDbSecondarySortKey(indexNames = "GSI1PK-GSI1SK-index")
    @DynamoDbAttribute("GSI1SK")
    public String getGsi1Sk() {
        return gsi1Sk;
    }

}
