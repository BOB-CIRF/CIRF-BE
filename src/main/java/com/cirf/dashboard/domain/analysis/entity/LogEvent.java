package com.cirf.dashboard.domain.analysis.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.Map;

/**
 * 데이터 스트림: logs-tenant-<tenantId>-default
 * 기본 템플릿 매핑에 맞춘 엔티티
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Document(createIndex=false, indexName = "placeholder")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogEvent {

    // ES _id (별도 지정 없으면 eventID를 @Id로 써도 됨)
    @Id
    private String id;

    // ---- 공통 메타 ----
    @Field(name = "@timestamp", type = FieldType.Date, format = DateFormat.date_optional_time)
    private Instant timestamp;              // @timestamp

    @Field(type = FieldType.Keyword)
    private String time;                   // 원본 eventTime 문자열 (파싱 문제 회피)

    // ---- 식별/분류 ----
    @Field(type = FieldType.Keyword)
    private String eventID;

    @Field(type = FieldType.Keyword)
    private String type;                    // "CloudTrail.Management" 등

    @Field(type = FieldType.Keyword)
    private String activity;                // eventName

    @Field(type = FieldType.Keyword)
    private String outcome;                 // Success 또는 errorCode

    // ---- 주체/대상/환경 ----
    @Field(type = FieldType.Keyword)
    private String actor;                   // userIdentity.arn

    @Field(type = FieldType.Keyword)
    private String actorAccountId;          // userIdentity.accountId

    @Field(type = FieldType.Keyword)
    private String accountId;               // recipientAccountId or 보정값

    @Field(type = FieldType.Keyword)
    private String src;                     // sourceIPAddress

    @Field(type = FieldType.Keyword)
    private String dst;                     // eventSource

    @Field(type = FieldType.Keyword)
    private String target;                  // resources[].ARN join

    @Field(type = FieldType.Keyword)
    private String region;                  // awsRegion

    // ---- 멀티테넌시/원천 ----
    @Field(type = FieldType.Keyword)
    private String tenantId;

    @Field(type = FieldType.Keyword)
    private String caseId;

    @Field(type = FieldType.Keyword)
    private String s3Bucket;

    @Field(type = FieldType.Keyword)
    private String s3Key;

    // ---- 원문 보존 (flattened) ----
    @Field(type = FieldType.Flattened)
    private Map<String, Object> event_data;
}

