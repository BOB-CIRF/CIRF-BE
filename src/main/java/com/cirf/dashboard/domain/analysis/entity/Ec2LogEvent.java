package com.cirf.dashboard.domain.analysis.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("account")
    @Field(name = "account", type = FieldType.Keyword)
    private String accountId;

    @JsonProperty("instance_id")
    @Field(name = "instance_id", type = FieldType.Keyword)
    private String instanceId;

    @JsonAlias("@name")
    @JsonProperty("@name")
    @Field(name = "@name", type = FieldType.Keyword)
    private String fileName;

    @Field(name = "event.category", type = FieldType.Keyword)
    private String logType;

    @Field(name= "event.action", type = FieldType.Keyword)
    private String activity;

    @Field(name = "event.outcome", type = FieldType.Keyword)
    private String outcome;

    // Jackson이 nested event 객체를 파싱하기 위한 setter
    @JsonProperty("event")
    public void setEvent(java.util.Map<String, Object> event) {
        if (event != null) {
            this.logType = (String) event.get("category");
            this.activity = (String) event.get("action");
            this.outcome = (String) event.get("outcome");
            this.raw = (String) event.get("original");
        }
    }

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

    // Jackson이 nested aws.s3 객체를 파싱하기 위한 setter
    @JsonProperty("aws")
    @SuppressWarnings("unchecked")
    public void setAws(java.util.Map<String, Object> aws) {
        if (aws != null && aws.containsKey("s3")) {
            java.util.Map<String, Object> s3 = (java.util.Map<String, Object>) aws.get("s3");
            if (s3 != null) {
                if (s3.containsKey("bucket")) {
                    java.util.Map<String, Object> bucket = (java.util.Map<String, Object>) s3.get("bucket");
                    if (bucket != null) {
                        this.s3Bucket = (String) bucket.get("name");
                    }
                }
                if (s3.containsKey("object")) {
                    java.util.Map<String, Object> object = (java.util.Map<String, Object>) s3.get("object");
                    if (object != null) {
                        this.s3Key = (String) object.get("key");
                    }
                }
            }
        }
    }
}
