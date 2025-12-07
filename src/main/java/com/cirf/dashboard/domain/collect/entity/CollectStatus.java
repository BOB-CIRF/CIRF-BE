package com.cirf.dashboard.domain.collect.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class CollectStatus {

    private String pk;              // Partition Key: PROGRESS#{progressId}
    private String sk;              // STATUS

    private Long progressId;        // 진행도 ID (카운터)
    private Long tenantId;          // 테넌트 ID
    private Long caseId;            // 케이스 ID
    private Integer completed;      // 완료된 작업 수
    private Integer pending;        // 대기 중인 작업 수
    private Integer process;        // 진행 중인 작업 수
    private Integer fail;           // 실패한 작업 수
    private Integer totalJob;       // 전체 작업 수
    private String updatedAt;

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

    @DynamoDbAttribute("completed")
    public Integer getCompleted() {
        return completed;
    }

    @DynamoDbAttribute("pending")
    public Integer getPending() {
        return pending;
    }

    @DynamoDbAttribute("process")
    public Integer getProcess() {
        return process;
    }

    @DynamoDbAttribute("fail")
    public Integer getFail() {
        return fail;
    }

    @DynamoDbAttribute("totalJob")
    public Integer getTotalJob() {
        return totalJob;
    }

    @DynamoDbAttribute("progressId")
    public Long getProgressId() {
        return progressId;
    }

    @DynamoDbAttribute("tenantId")
    public Long getTenantId() {
        return tenantId;
    }

    @DynamoDbAttribute("caseId")
    public Long getCaseId() {
        return caseId;
    }

    @DynamoDbAttribute("updatedAt")
    public String getUpdatedAt() {
        return updatedAt;
    }
}
