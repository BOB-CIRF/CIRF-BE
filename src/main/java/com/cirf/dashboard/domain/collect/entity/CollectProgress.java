package com.cirf.dashboard.domain.collect.entity;

import com.cirf.dashboard.domain.collect.dto.request.WebhookJob;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class CollectProgress {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String pk;              // Partition Key: PROGRESS#{collectId}
    private String sk;              // Sort Key: JOB#{jobId}

    private Integer completed;      // 완료된 작업 수
    private Integer pending;        // 대기 중인 작업 수
    private Integer process;        // 진행 중인 작업 수
    private Integer fail;           // 실패한 작업 수
    private Integer totalJob;       // 전체 작업 수
    private Long revision;          // 리비전 (타임스탬프)
    private Boolean terminal;       // 종료 여부
    private String updatedAt;       // 업데이트 시간
    private String lastSequence;    // 마지막 시퀀스
    private String jobsJson;        // 작업 목록 JSON

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

    @DynamoDbAttribute("revision")
    public Long getRevision() {
        return revision;
    }

    @DynamoDbAttribute("terminal")
    public Boolean getTerminal() {
        return terminal;
    }

    @DynamoDbAttribute("updatedAt")
    public String getUpdatedAt() {
        return updatedAt;
    }

    @DynamoDbAttribute("lastSequence")
    public String getLastSequence() {
        return lastSequence;
    }

    @DynamoDbAttribute("jobs")
    public String getJobsJson() {
        return jobsJson;
    }

    /**
     * JSON 문자열을 WebhookJob 리스트로 변환
     */
    @DynamoDbIgnore
    public List<WebhookJob> getJobs() {
        if (jobsJson == null || jobsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(jobsJson, new TypeReference<List<WebhookJob>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse jobs JSON: {}", jobsJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * WebhookJob 리스트를 JSON 문자열로 변환하여 저장
     */
    @DynamoDbIgnore
    public void setJobs(List<WebhookJob> jobs) {
        try {
            this.jobsJson = objectMapper.writeValueAsString(jobs);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize jobs to JSON", e);
            this.jobsJson = "[]";
        }
    }

    /**
     * PK에서 collectId 추출
     * PK 형식: PROGRESS#{collectId}
     */
    @DynamoDbIgnore
    public Integer getCollectId() {
        if (pk == null || !pk.startsWith("PROGRESS#")) {
            return null;
        }
        try {
            return Integer.parseInt(pk.substring("PROGRESS#".length()));
        } catch (NumberFormatException e) {
            log.error("Failed to parse collectId from PK: {}", pk, e);
            return null;
        }
    }

    /**
     * SK에서 jobId 추출
     * SK 형식: JOB#{jobId}
     */
    @DynamoDbIgnore
    public String getJobId() {
        if (sk == null || !sk.startsWith("JOB#")) {
            return null;
        }
        return sk.substring("JOB#".length());
    }
}
