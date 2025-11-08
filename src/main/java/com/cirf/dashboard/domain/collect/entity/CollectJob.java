package com.cirf.dashboard.domain.collect.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * cirf-collect 테이블의 엔티티
 * PK: JOB#{jobId}
 * SK: COLLECT#{collectId}#TYPE#{logType}#DEST#{destType}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class CollectJob {

    private String pk;              // Partition Key: JOB#{jobId}
    private String sk;              // Sort Key: COLLECT#{collectId}#TYPE#{logType}#DEST#{destType}

    private String jobId;           // UUID
    private String collectId;       // 수집 작업 ID
    private String logType;         // 수집 타입 (cloudtrail, vpcflowlogs, etc.)
    private String destType;        // 목적지
    private String dt;              // 날짜 (YYYY-MM-DD)

    private String status;          // COMPLETED, IN_PROGRESS, FAIL, NoData
    private String startedAt;       // 시작 시간
    private String lastUpdated;     // 마지막 업데이트 시간 (종료 시간)

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

    @DynamoDbAttribute("job_id")
    public String getJobId() {
        return jobId;
    }

    @DynamoDbAttribute("collect_id")
    public String getCollectId() {
        return collectId;
    }

    @DynamoDbAttribute("log_type")
    public String getLogType() {
        return logType;
    }

    @DynamoDbAttribute("dest_type")
    public String getDestType() {
        return destType;
    }

    @DynamoDbAttribute("dt")
    public String getDt() {
        return dt;
    }

    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }

    @DynamoDbAttribute("StartedAt")
    public String getStartedAt() {
        return startedAt;
    }

    @DynamoDbAttribute("lastUpdated")
    public String getLastUpdated() {
        return lastUpdated;
    }
}
