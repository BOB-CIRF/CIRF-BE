package com.cirf.dashboard.domain.analysis.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(createIndex=false, indexName = "placeholder")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ec2LogEvent {

    @Id
    private String id;

    @JsonAlias("@timestamp")
    @Field(name = "@timestamp", type = FieldType.Date, format = DateFormat.date_optional_time)
    private Instant timestamp;

    @Field(name = "account", type = FieldType.Keyword)
    private String accountId;

    @Field(name = "instance_id", type = FieldType.Keyword)
    private String instanceId;

    @Field(name = "@name", type = FieldType.Keyword)
    private String fileName;

    @Field(name = "event.category", type = FieldType.Keyword)
    private String logType;

    @Field(name= "event.action", type = FieldType.Keyword)
    private String activity;

    @Field(name = "event.outcome", type = FieldType.Keyword)
    private String outcome;

    @Field(name = "region", type = FieldType.Keyword)
    private String region;

    @Field(name = "tenantId", type = FieldType.Keyword)
    private String tenantId;

    @Field(name = "caseId", type = FieldType.Keyword)
    private String caseId;

    @Field(name = "s3.bucket.name", type = FieldType.Keyword)
    private String s3Bucket;

    @Field(name = "s3.object.key", type = FieldType.Keyword)
    private String s3Key;

    @Field(name = "event.original", type = FieldType.Text)
    private String raw;
}
